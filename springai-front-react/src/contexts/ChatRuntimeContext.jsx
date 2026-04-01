/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useAuth } from './AuthContext';
import {
    createCoachSSE,
    createLoveAppSSE,
    getActiveRuns,
    getChatImages,
    getChatMessages,
    getChatRun
} from '../services/chatApi';

const ChatRuntimeContext = createContext(null);

const ACTIVE_STATUSES = new Set(['QUEUED', 'RUNNING']);
const RECOVERY_POLL_INTERVAL_MS = 3000;
const buildChatKey = (chatType, chatId) => {
    if (!chatType || !chatId) {
        return null;
    }
    return `${chatType}:${chatId}`;
};

const createEmptyRuntime = (chatType = 'loveapp', chatId = null) => ({
    chatType,
    chatId,
    messages: [],
    isLoading: false,
    streamingStatus: null,
    runId: null,
    runStatus: 'IDLE',
    lastEventType: null,
    latestStatusText: '',
    partialResponse: '',
    errorMessage: null,
    hasLocalConnection: false
});

const cleanContent = (text) => {
    if (!text) {
        return text;
    }
    let cleaned = text.replace(/\{\{[^}]*\}\}/g, '');
    cleaned = cleaned.replace(/\{\{/g, '').replace(/\}\}/g, '');
    return cleaned.trim();
};

const parseSSEMessage = (rawData) => {
    if (rawData === '[DONE]') {
        return { type: 'done', content: '', data: null };
    }

    try {
        const parsed = JSON.parse(rawData);
        if (parsed === null || typeof parsed !== 'object') {
            return { type: 'content', content: cleanContent(String(parsed)), data: null };
        }
        return {
            type: parsed.type || 'content',
            content: cleanContent(parsed.content || ''),
            data: parsed.data || null
        };
    } catch {
        return { type: 'content', content: cleanContent(rawData), data: null };
    }
};

const createStatusStep = (type, content) => ({
    type,
    content,
    timestamp: Date.now()
});

const appendUserMessage = (messages, content, imageUrl = null) => [
    ...messages,
    { role: 'user', content, ...(imageUrl && { imageUrl }) }
];

const upsertStatusStep = (messages, type, content) => {
    const step = createStatusStep(type, content);
    const lastMessage = messages[messages.length - 1];

    if (lastMessage?.role === 'assistant' && lastMessage.isStreaming) {
        const steps = lastMessage.statusSteps || [];
        const nextSteps = steps.length > 0 && steps[steps.length - 1].type === type
            ? [...steps.slice(0, -1), step]
            : [...steps, step];
        return [
            ...messages.slice(0, -1),
            { ...lastMessage, statusSteps: nextSteps }
        ];
    }

    return [
        ...messages,
        { role: 'assistant', content: '', isStreaming: true, statusSteps: [step] }
    ];
};

const appendStreamingContent = (messages, chunk) => {
    const lastMessage = messages[messages.length - 1];
    if (lastMessage?.role === 'assistant' && lastMessage.isStreaming) {
        return [
            ...messages.slice(0, -1),
            { ...lastMessage, content: `${lastMessage.content || ''}${chunk}` }
        ];
    }

    return [
        ...messages,
        { role: 'assistant', content: chunk, isStreaming: true }
    ];
};

const appendImageToStreamingMessage = (messages, imageData) => {
    const lastMessage = messages[messages.length - 1];
    if (lastMessage?.role === 'assistant') {
        const existingImages = lastMessage.images || [];
        const isDuplicate = existingImages.some(img =>
            img.url === imageData.url || (img.name === imageData.name && img.url === imageData.url)
        );
        if (isDuplicate) return messages;
        return [
            ...messages.slice(0, -1),
            { ...lastMessage, images: [...existingImages, imageData] }
        ];
    }
    return messages;
};

const finalizeStreamingMessage = (messages) => {
    const lastMessage = messages[messages.length - 1];
    if (lastMessage?.isStreaming) {
        return [
            ...messages.slice(0, -1),
            { ...lastMessage, isStreaming: false }
        ];
    }
    return messages;
};

const appendErrorMessage = (messages, message) => [
    ...messages,
    { role: 'assistant', content: message, isError: true }
];

const restoreStreamingMessage = (messages, lastEventType, latestStatusText, partialResponse) => {
    const lastMessage = messages[messages.length - 1];
    const statusStep = latestStatusText
        ? [createStatusStep(lastEventType || 'status', latestStatusText)]
        : [];

    if (lastMessage?.role === 'assistant' && lastMessage.isStreaming) {
        return [
            ...messages.slice(0, -1),
            {
                ...lastMessage,
                content: partialResponse || lastMessage.content || '',
                statusSteps: statusStep.length > 0 ? statusStep : (lastMessage.statusSteps || [])
            }
        ];
    }

    return [
        ...messages,
        {
            role: 'assistant',
            content: partialResponse || '',
            isStreaming: true,
            ...(statusStep.length > 0 ? { statusSteps: statusStep } : {})
        }
    ];
};

const finalizeRecoveredMessages = (messages, partialResponse) => {
    const lastMessage = messages[messages.length - 1];
    if (lastMessage?.role === 'assistant' && lastMessage.isStreaming) {
        return [
            ...messages.slice(0, -1),
            {
                ...lastMessage,
                content: partialResponse || lastMessage.content || '',
                isStreaming: false
            }
        ];
    }
    if (messages.length === 0 && partialResponse) {
        return [{ role: 'assistant', content: partialResponse }];
    }
    return messages;
};

export function useChatRuntime() {
    return useContext(ChatRuntimeContext);
}

export function ChatRuntimeProvider({ children }) {
    const { isAuthenticated } = useAuth();
    const [runtimes, setRuntimes] = useState({});
    const runtimesRef = useRef(runtimes);
    const connectionsRef = useRef(new Map());
    const listenersRef = useRef(new Map());
    const recoveryPollRunIdsRef = useRef(new Set());

    useEffect(() => {
        runtimesRef.current = runtimes;
    }, [runtimes]);

    const closeConnectionByKey = useCallback((chatKey) => {
        const existing = connectionsRef.current.get(chatKey);
        if (existing) {
            existing.close();
            connectionsRef.current.delete(chatKey);
        }
    }, []);

    const updateRuntime = useCallback((chatType, chatId, updater) => {
        const chatKey = buildChatKey(chatType, chatId);
        if (!chatKey) {
            return;
        }

        setRuntimes(prev => {
            const current = prev[chatKey] || createEmptyRuntime(chatType, chatId);
            const next = typeof updater === 'function' ? updater(current) : updater;
            return {
                ...prev,
                [chatKey]: {
                    ...createEmptyRuntime(chatType, chatId),
                    ...next,
                    chatType,
                    chatId
                }
            };
        });
    }, []);

    const refreshChatMessages = useCallback(async (chatType, chatId) => {
        if (!chatId) {
            return;
        }
        try {
            const messages = await getChatMessages(chatId, chatType);
            let nextMessages = Array.isArray(messages) ? messages : [];

            // Coach 模式：加载会话图片并附加到最后一条 assistant 消息
            if (chatType === 'coach' && nextMessages.length > 0) {
                try {
                    const images = await getChatImages(chatId, chatType);
                    if (Array.isArray(images) && images.length > 0) {
                        const lastAssistantIdx = nextMessages.findLastIndex(m => m.role === 'assistant');
                        if (lastAssistantIdx >= 0) {
                            nextMessages = [...nextMessages];
                            nextMessages[lastAssistantIdx] = {
                                ...nextMessages[lastAssistantIdx],
                                images
                            };
                        }
                    }
                } catch (error) {
                    console.error('Failed to load images during refresh:', error);
                }
            }

            updateRuntime(chatType, chatId, current => {
                // 如果有本地连接且正在流式传输，保留当前状态
                if (current.hasLocalConnection && current.messages.some(message => message.isStreaming)) {
                    return current;
                }

                // 如果当前处于活跃状态（QUEUED/RUNNING），不要覆盖现有消息
                if (ACTIVE_STATUSES.has(current.runStatus) && current.messages.length > 0) {
                    return current;
                }

                return {
                    ...current,
                    messages: nextMessages
                };
            });
        } catch (error) {
            console.error('Failed to refresh chat messages:', error);
        }
    }, [updateRuntime]);

    const applyRunSnapshot = useCallback((run) => {
        if (!run?.chatId || !run?.chatType) {
            return;
        }

        updateRuntime(run.chatType, run.chatId, current => {
            const isActive = ACTIVE_STATUSES.has(run.status);
            let nextMessages = current.messages;

            if (!current.hasLocalConnection) {
                if (isActive) {
                    nextMessages = restoreStreamingMessage(
                        current.messages,
                        run.lastEventType,
                        run.latestStatusText,
                        run.partialResponse
                    );
                } else {
                    nextMessages = finalizeRecoveredMessages(current.messages, run.partialResponse);
                    if (run.status === 'FAILED' && run.errorMessage) {
                        const lastMessage = nextMessages[nextMessages.length - 1];
                        if (!lastMessage?.isError || lastMessage.content !== run.errorMessage) {
                            nextMessages = appendErrorMessage(nextMessages, run.errorMessage);
                        }
                    }
                }
            }

            return {
                ...current,
                runId: run.runId,
                runStatus: run.status,
                lastEventType: run.lastEventType,
                latestStatusText: run.latestStatusText || '',
                partialResponse: run.partialResponse || current.partialResponse,
                errorMessage: run.errorMessage,
                isLoading: isActive,
                streamingStatus: isActive && run.latestStatusText
                    ? { type: run.lastEventType || 'status', content: run.latestStatusText }
                    : null,
                messages: nextMessages
            };
        });
    }, [updateRuntime]);

    const hydrateMessages = useCallback((chatType, chatId, messages) => {
        updateRuntime(chatType, chatId, current => {
            // 如果有本地连接且正在流式传输，保留当前状态
            if (current.hasLocalConnection && current.messages.some(message => message.isStreaming)) {
                return current;
            }

            // 如果当前处于活跃状态（QUEUED/RUNNING），不要覆盖现有消息
            if (ACTIVE_STATUSES.has(current.runStatus) && current.messages.length > 0) {
                return current;
            }

            let nextMessages = Array.isArray(messages) ? messages : [];
            return {
                ...current,
                messages: nextMessages
            };
        });
    }, [updateRuntime]);

    const clearChatRuntime = useCallback((chatType, chatId) => {
        const chatKey = buildChatKey(chatType, chatId);
        if (!chatKey) {
            return;
        }
        closeConnectionByKey(chatKey);
        listenersRef.current.delete(chatKey);
        setRuntimes(prev => {
            const next = { ...prev };
            delete next[chatKey];
            return next;
        });
    }, [closeConnectionByKey]);

    const registerParsedMessageListener = useCallback((chatType, chatId, listener) => {
        const chatKey = buildChatKey(chatType, chatId);
        if (!chatKey || typeof listener !== 'function') {
            return () => {};
        }

        listenersRef.current.set(chatKey, listener);
        return () => {
            const currentListener = listenersRef.current.get(chatKey);
            if (currentListener === listener) {
                listenersRef.current.delete(chatKey);
            }
        };
    }, []);

    const handleParsedMessage = useCallback((chatType, chatId, parsed) => {
        updateRuntime(chatType, chatId, current => {
            switch (parsed.type) {
                case 'run_started':
                    return {
                        ...current,
                        runId: parsed.data?.runId || current.runId,
                        runStatus: parsed.data?.status || 'QUEUED',
                        hasLocalConnection: true
                    };

                case 'thinking':
                case 'status':
                case 'intake_status':
                case 'ocr_result':
                case 'rewrite_result':
                case 'rag_status':
                case 'tool_call':
                    return {
                        ...current,
                        messages: upsertStatusStep(current.messages, parsed.type, parsed.content),
                        isLoading: true,
                        runStatus: 'RUNNING',
                        lastEventType: parsed.type,
                        latestStatusText: parsed.content,
                        streamingStatus: { type: parsed.type, content: parsed.content }
                    };

                case 'content':
                    return {
                        ...current,
                        messages: appendStreamingContent(current.messages, parsed.content),
                        isLoading: true,
                        runStatus: 'RUNNING',
                        lastEventType: 'content',
                        partialResponse: `${current.partialResponse || ''}${parsed.content || ''}`,
                        streamingStatus: null
                    };

                case 'done':
                    return {
                        ...current,
                        messages: finalizeStreamingMessage(current.messages),
                        isLoading: false,
                        runId: parsed.data?.runId || current.runId,
                        runStatus: parsed.data?.status || 'COMPLETED',
                        lastEventType: 'done',
                        latestStatusText: '',
                        streamingStatus: null,
                        hasLocalConnection: false
                    };

                case 'error':
                    return {
                        ...current,
                        messages: appendErrorMessage(finalizeStreamingMessage(current.messages), parsed.content || '抱歉，AI 服务暂时不可用。'),
                        isLoading: false,
                        runStatus: 'FAILED',
                        lastEventType: 'error',
                        latestStatusText: parsed.content || '',
                        errorMessage: parsed.content || '',
                        streamingStatus: null,
                        hasLocalConnection: false
                    };

                case 'file_created':
                    if (parsed.data?.type === 'image' && parsed.data?.url) {
                        return {
                            ...current,
                            messages: appendImageToStreamingMessage(current.messages, {
                                type: parsed.data.type,
                                name: parsed.data.name || '',
                                url: parsed.data.url
                            })
                        };
                    }
                    return current;

                default:
                    if (!parsed.content) {
                        return current;
                    }
                    return {
                        ...current,
                        messages: appendStreamingContent(current.messages, parsed.content),
                        isLoading: true,
                        runStatus: 'RUNNING',
                        lastEventType: 'content',
                        partialResponse: `${current.partialResponse || ''}${parsed.content || ''}`,
                        streamingStatus: null
                    };
            }
        });
    }, [updateRuntime]);

    const sendMessage = useCallback(async ({
        chatType,
        chatId,
        message,
        imageUrl = null
    }) => {
        if (!chatId) {
            throw new Error('chatId is required to send a message');
        }

        const userMessage = (message || '').trim();
        if (!userMessage && !imageUrl) {
            return;
        }

        const chatKey = buildChatKey(chatType, chatId);
        const createSSE = chatType === 'coach' ? createCoachSSE : createLoveAppSSE;
        const initialType = imageUrl ? 'intake_status' : 'thinking';
        const initialContent = imageUrl
            ? (chatType === 'coach'
                ? '正在读取聊天截图，并判断这件事要不要进入任务执行...'
                : '正在识别聊天截图，准备给你回复建议...')
            : (chatType === 'coach'
                ? '正在整理问题，并判断是否需要继续执行任务...'
                : '正在分析对话语气，准备回复建议...');

        closeConnectionByKey(chatKey);

        updateRuntime(chatType, chatId, current => ({
            ...current,
            messages: upsertStatusStep(
                appendUserMessage(current.messages, userMessage, imageUrl),
                initialType,
                initialContent
            ),
            isLoading: true,
            runStatus: 'QUEUED',
            lastEventType: initialType,
            latestStatusText: initialContent,
            partialResponse: '',
            errorMessage: null,
            streamingStatus: { type: initialType, content: initialContent },
            hasLocalConnection: true
        }));

        try {
            const eventSource = await createSSE(userMessage, chatId, imageUrl, {
                onData: (rawData) => {
                    const parsed = parseSSEMessage(rawData);
                    handleParsedMessage(chatType, chatId, parsed);
                    listenersRef.current.get(chatKey)?.(parsed);

                    if (parsed.type === 'done' || parsed.type === 'error') {
                        connectionsRef.current.delete(chatKey);
                        refreshChatMessages(chatType, chatId);
                    }
                },
                onError: async (error) => {
                    console.error('SSE Error:', error);
                    const errorMessage = error?.name === 'AuthExpiredError'
                        ? error.message
                        : '抱歉，连接出现问题，请稍后重试。';

                    updateRuntime(chatType, chatId, current => ({
                        ...current,
                        messages: appendErrorMessage(finalizeStreamingMessage(current.messages), errorMessage),
                        isLoading: false,
                        runStatus: current.runStatus === 'COMPLETED' ? 'COMPLETED' : 'FAILED',
                        latestStatusText: '',
                        errorMessage,
                        streamingStatus: null,
                        hasLocalConnection: false
                    }));
                    connectionsRef.current.delete(chatKey);
                    await refreshChatMessages(chatType, chatId);
                },
                onComplete: async () => {
                    connectionsRef.current.delete(chatKey);
                    updateRuntime(chatType, chatId, current => ({
                        ...current,
                        hasLocalConnection: false
                    }));
                    await refreshChatMessages(chatType, chatId);
                }
            });

            connectionsRef.current.set(chatKey, eventSource);
        } catch (error) {
            console.error('Failed to create SSE:', error);
            const errorMessage = error?.message || '抱歉，连接出现问题，请稍后重试。';
            updateRuntime(chatType, chatId, current => ({
                ...current,
                messages: appendErrorMessage(finalizeStreamingMessage(current.messages), errorMessage),
                isLoading: false,
                runStatus: 'FAILED',
                latestStatusText: '',
                errorMessage,
                streamingStatus: null,
                hasLocalConnection: false
            }));
        }
    }, [closeConnectionByKey, handleParsedMessage, refreshChatMessages, updateRuntime]);

    useEffect(() => {
        if (!isAuthenticated) {
            connectionsRef.current.forEach(connection => connection.close());
            connectionsRef.current.clear();
            listenersRef.current.clear();
            Promise.resolve().then(() => {
                setRuntimes({});
            });
            return;
        }

        let cancelled = false;
        const recoveryPollRunIds = recoveryPollRunIdsRef.current;

        const restoreActiveRuns = async () => {
            try {
                const activeRuns = await getActiveRuns();
                if (cancelled) {
                    return;
                }
                activeRuns.forEach(applyRunSnapshot);
            } catch (error) {
                console.error('Failed to restore active chat runs:', error);
            }
        };

        restoreActiveRuns();

        let timeoutId = null;

        const pollActiveRuns = async () => {
            const runtimeEntries = Object.values(runtimesRef.current);
            const runsToPoll = runtimeEntries.filter(runtime =>
                runtime.runId
                && ACTIVE_STATUSES.has(runtime.runStatus)
                && !runtime.hasLocalConnection
                && !recoveryPollRunIds.has(runtime.runId)
            );

            try {
                await Promise.all(runsToPoll.map(async (runtime) => {
                    recoveryPollRunIds.add(runtime.runId);
                    try {
                        const run = await getChatRun(runtime.runId);
                        if (cancelled) {
                            return;
                        }
                        applyRunSnapshot(run);
                        if (!ACTIVE_STATUSES.has(run.status)) {
                            await refreshChatMessages(run.chatType, run.chatId);
                        }
                    } catch (error) {
                        if (error?.status === 404) {
                            updateRuntime(runtime.chatType, runtime.chatId, current => ({
                                ...current,
                                runId: null,
                                runStatus: 'IDLE',
                                isLoading: false,
                                lastEventType: null,
                                latestStatusText: '',
                                streamingStatus: null,
                                errorMessage: null,
                                hasLocalConnection: false
                            }));
                            return;
                        }
                        console.error('Failed to poll chat run status:', error);
                    } finally {
                        recoveryPollRunIds.delete(runtime.runId);
                    }
                }));
            } finally {
                if (!cancelled) {
                    timeoutId = window.setTimeout(pollActiveRuns, RECOVERY_POLL_INTERVAL_MS);
                }
            }
        };

        timeoutId = window.setTimeout(pollActiveRuns, RECOVERY_POLL_INTERVAL_MS);

        return () => {
            cancelled = true;
            if (timeoutId) {
                window.clearTimeout(timeoutId);
            }
            recoveryPollRunIds.clear();
        };
    }, [applyRunSnapshot, isAuthenticated, refreshChatMessages, updateRuntime]);

    const runtimeEntries = Object.values(runtimes);

    const value = {
        getChatState: (chatType, chatId) => {
            const chatKey = buildChatKey(chatType, chatId);
            return chatKey ? (runtimes[chatKey] || createEmptyRuntime(chatType, chatId)) : createEmptyRuntime(chatType, chatId);
        },
        hydrateMessages,
        refreshChatMessages,
        clearChatRuntime,
        registerParsedMessageListener,
        sendMessage,
        runtimeEntries,
        activeRuns: runtimeEntries.filter(runtime => ACTIVE_STATUSES.has(runtime.runStatus))
    };

    return (
        <ChatRuntimeContext.Provider value={value}>
            {children}
        </ChatRuntimeContext.Provider>
    );
}
