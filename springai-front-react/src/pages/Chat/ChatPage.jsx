import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import ChatSidebar from '../../components/Sidebar/ChatSidebar';
import ChatArea from '../../components/Chat/ChatArea';
import { getChatImages, getChatMessages } from '../../services/chatApi';
import { useChatSessions } from '../../hooks/useChatSessions';
import { useChatRuntime } from '../../contexts/ChatRuntimeContext';
import { useBackgroundRuns } from '../../hooks/useBackgroundRuns';
import styles from './ChatPage.module.css';

const noop = () => {};

const ChatPage = () => {
    const { type } = useParams();
    const chatType = type || 'loveapp';
    const isCoach = chatType === 'coach';
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [inputValue, setInputValue] = useState('');

    const {
        runs,
        activeRunCount,
        registerRun,
        completeRun,
        failRun,
        clearRun
    } = useBackgroundRuns();

    const {
        getChatState,
        hydrateMessages,
        clearChatRuntime,
        sendMessage,
        runtimeEntries
    } = useChatRuntime();

    const stopChatRuntime = useCallback((chatId) => {
        clearChatRuntime(chatType, chatId);
    }, [chatType, clearChatRuntime]);

    // Chat sessions
    const {
        chatList,
        currentChatId,
        setCurrentChatId,
        createNewChat,
        autoCreateChat,
        deleteChat,
        updateChatTitle
    } = useChatSessions(chatType, stopChatRuntime, noop);

    const runtime = getChatState(chatType, currentChatId);
    const runtimeRef = useRef(runtime);
    const messages = runtime.messages;
    const isLoading = runtime.isLoading;
    const streamingStatus = runtime.streamingStatus;
    const recoveryStatus = (() => {
        if (!currentChatId) {
            return null;
        }

        const runStatus = runs[currentChatId]?.status;
        if (runStatus === 'completed') {
            return 'completed';
        }
        if (runStatus === 'failed') {
            return 'failed';
        }
        if (runStatus === 'generating' && !runtime.hasLocalConnection) {
            return 'generating';
        }
        return null;
    })();

    useEffect(() => {
        runtimeRef.current = runtime;
    }, [runtime]);

    // Load messages (and fallback images for coach) when currentChatId changes
    useEffect(() => {
        if (!currentChatId) return;

        let cancelled = false;
        const activeChatId = currentChatId;

        const loadMessages = async () => {
            const runtimeAtLoadStart = runtimeRef.current;

            if (runtimeAtLoadStart.messages.length > 0) {
                return;
            }

            if (runtimeAtLoadStart.isLoading && runtimeAtLoadStart.messages.length > 0) {
                return;
            }

            try {
                let msgs = await getChatMessages(activeChatId, chatType);
                if (cancelled) return;

                // For coach mode, load conversation images and attach to the last assistant message
                // as fallback (in case markdown content doesn't already include images)
                if (isCoach && msgs.length > 0) {
                    try {
                        const images = await getChatImages(activeChatId, chatType);
                        if (!cancelled && Array.isArray(images) && images.length > 0) {
                            // Find the last assistant message and attach images
                            const lastAssistantIdx = msgs.findLastIndex(m => m.role === 'assistant');
                            if (lastAssistantIdx >= 0) {
                                msgs = [...msgs];
                                msgs[lastAssistantIdx] = {
                                    ...msgs[lastAssistantIdx],
                                    images
                                };
                            }
                        }
                    } catch (error) {
                        console.error('Failed to load images:', error);
                    }
                }

                hydrateMessages(chatType, activeChatId, msgs.length > 0 ? msgs : []);
            } catch (error) {
                console.error('Failed to load messages:', error);
                if (!cancelled) {
                    hydrateMessages(chatType, activeChatId, []);
                }
            }
        };

        loadMessages();
        return () => {
            cancelled = true;
        };
    }, [currentChatId, chatType, isCoach, hydrateMessages]);

    // Send message handler
    const handleSendMessage = useCallback(async (msgText = inputValue, imageUrl = null) => {
        const textToUse = msgText || inputValue;
        if ((!textToUse.trim() && !imageUrl) || isLoading) return;

        // Auto-create chat if no current chat exists
        let activeChatId = currentChatId;
        if (!activeChatId) {
            const newChat = autoCreateChat(textToUse.trim());
            activeChatId = newChat.id;
        }

        const userMessage = textToUse.trim();
        setInputValue('');

        // Update chat title if this is the first user message
        const currentChat = chatList.find(chat => chat.id === activeChatId);
        if (currentChat?.title === '新的对话') {
            const title = userMessage.length > 20
                ? `${userMessage.substring(0, 20)}...`
                : userMessage;
            updateChatTitle(activeChatId, title);
        }

        await sendMessage({
            chatType,
            chatId: activeChatId,
            message: userMessage,
            imageUrl
        });
    }, [
        inputValue,
        isLoading,
        currentChatId,
        autoCreateChat,
        chatList,
        updateChatTitle,
        sendMessage,
        chatType
    ]);

    // New chat handler
    const handleNewChat = useCallback(() => {
        createNewChat();
        setInputValue('');
    }, [createNewChat]);

    // Delete chat handler
    const handleDeleteChat = useCallback(async (chatId) => {
        await deleteChat(chatId, noop);
    }, [deleteChat]);

    const handleToggleSidebar = useCallback(() => {
        setIsSidebarOpen(prev => !prev);
    }, []);

    // Sync background badges from the runtime store so terminal states clear correctly.
    useEffect(() => {
        const relevantRuntimes = new Map(
            runtimeEntries
                .filter(runtimeEntry => runtimeEntry.chatType === chatType && runtimeEntry.chatId)
                .map(runtimeEntry => [runtimeEntry.chatId, runtimeEntry])
        );

        relevantRuntimes.forEach((runtimeEntry, runChatId) => {
            const isCurrentChat = runChatId === currentChatId;
            const currentStatus = runs[runChatId]?.status;

            if (runtimeEntry.runStatus === 'QUEUED' || runtimeEntry.runStatus === 'RUNNING') {
                if (!isCurrentChat && !currentStatus) {
                    registerRun(runChatId, null);
                }
                return;
            }

            if (runtimeEntry.runStatus === 'COMPLETED') {
                if (isCurrentChat) {
                    if (currentStatus) {
                        clearRun(runChatId);
                    }
                } else if (currentStatus !== 'completed') {
                    completeRun(runChatId);
                }
                return;
            }

            if (runtimeEntry.runStatus === 'FAILED') {
                if (isCurrentChat) {
                    if (currentStatus) {
                        clearRun(runChatId);
                    }
                } else if (currentStatus !== 'failed') {
                    failRun(runChatId, runtimeEntry.errorMessage);
                }
                return;
            }

            if (currentStatus) {
                clearRun(runChatId);
            }
        });

        Object.keys(runs).forEach(runChatId => {
            if (!relevantRuntimes.has(runChatId)) {
                clearRun(runChatId);
            }
        });
    }, [runtimeEntries, chatType, currentChatId, runs, registerRun, completeRun, failRun, clearRun]);

    // Navigate to first active background run when pill is clicked
    const handleNavigateToRun = useCallback(() => {
        const generatingRun = Object.entries(runs).find(([, run]) => run.status === 'generating');
        if (generatingRun) {
            const [runChatId] = generatingRun;
            setCurrentChatId(runChatId);
        }
    }, [runs, setCurrentChatId]);

    // Clear recovery status
    const handleRecoveryDismiss = useCallback(() => {
        if (currentChatId) {
            clearRun(currentChatId);
        }
    }, [currentChatId, clearRun]);

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.98 }}
            transition={{ duration: 0.3 }}
            className={styles.page}
        >
            <div className={`${styles.sidebarSlot} ${!isSidebarOpen ? styles.sidebarCollapsed : ''}`}>
                <ChatSidebar
                    isOpen={isSidebarOpen}
                    onToggle={handleToggleSidebar}
                    chatList={chatList}
                    currentChatId={currentChatId}
                    onSelectChat={setCurrentChatId}
                    onNewChat={handleNewChat}
                    onDeleteChat={handleDeleteChat}
                    backgroundRuns={runs}
                />
            </div>

            <main className={styles.main}>
                <div className={styles.chatColumn}>
                    <ChatArea
                        messages={messages}
                        inputValue={inputValue}
                        setInputValue={setInputValue}
                        onSendMessage={handleSendMessage}
                        isLoading={isLoading}
                        streamingStatus={streamingStatus}
                        chatType={chatType}
                        activeRunCount={activeRunCount}
                        onNavigateToRun={handleNavigateToRun}
                        recoveryStatus={recoveryStatus}
                        onRecoveryDismiss={handleRecoveryDismiss}
                    />
                </div>
            </main>
        </motion.div>
    );
};

export default ChatPage;
