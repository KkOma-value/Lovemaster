import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import ChatSidebar from '../../components/Sidebar/ChatSidebar';
import ChatArea from '../../components/Chat/ChatArea';
import ManusPanel from '../../components/ManusPanel/ManusPanel';
import { createLoveAppSSE, createCoachSSE, getChatSessions, deleteChatSession, getChatMessages } from '../../services/chatApi';

const ChatPage = () => {
    const { type } = useParams(); // 'loveapp' or 'coach'
    const chatType = type || 'loveapp';
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    // Message state
    const [messages, setMessages] = useState([]);
    const [inputValue, setInputValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Streaming status state
    const [streamingStatus, setStreamingStatus] = useState(null);

    // SSE connection ref
    const eventSourceRef = useRef(null);
    const currentResponseRef = useRef('');

    // Chat history state - loaded from backend
    const [chatList, setChatList] = useState([]);
    const [currentChatId, setCurrentChatId] = useState(null);
    // Manus Panel state (only for coach mode)
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const [panelData, setPanelData] = useState({
        tasks: [],
        terminalLines: [],
        files: []
    });

    // Check if should show Manus panel (only for coach/manus mode)
    const showManusPanel = chatType === 'coach';

    // Cleanup SSE connection
    const cleanupSSE = useCallback((options = { resetResponse: true }) => {
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
            eventSourceRef.current = null;
        }
        if (options.resetResponse) {
            currentResponseRef.current = '';
        }
        setIsLoading(false);
        setStreamingStatus(null);
    }, []);

    // Cleanup on unmount
    useEffect(() => {
        return () => cleanupSSE();
    }, [cleanupSSE]);

    // Reset panel data when chat changes
    useEffect(() => {
        setPanelData({
            tasks: [],
            terminalLines: [],
            files: []
        });
        setIsPanelOpen(false);
    }, [currentChatId]);

    // Load sessions from backend and initialize
    useEffect(() => {
        const loadSessions = async () => {
            try {
                // Pass chat type to load correct directory's sessions
                const sessions = await getChatSessions(chatType);

                if (sessions.length > 0) {
                    setChatList(sessions);
                    // Select the first (most recent) session
                    setCurrentChatId(sessions[0].id);
                } else {
                    // No existing sessions, create a new one
                    const newId = `chat_${Date.now()}`;
                    setChatList([{ id: newId, title: '新的对话' }]);
                    setCurrentChatId(newId);
                    setMessages([]);
                }
            } catch (error) {
                console.error('Failed to load sessions:', error);
                // Fallback: create a new session
                const newId = `chat_${Date.now()}`;
                setChatList([{ id: newId, title: '新的对话' }]);
                setCurrentChatId(newId);
                setMessages([]);
            } finally {
                // no-op
            }
        };

        loadSessions();
    }, [chatType]);  // Reload when chat type changes

    // Load messages when currentChatId changes
    useEffect(() => {
        if (!currentChatId) return;

        const loadMessages = async () => {
            try {
                // Pass chat type for correct directory
                const msgs = await getChatMessages(currentChatId, chatType);
                if (msgs.length > 0) {
                    setMessages(msgs);
                } else {
                    // New chat, start with empty messages for welcome state
                    setMessages([]);
                }
            } catch (error) {
                console.error('Failed to load messages:', error);
                // Start with empty messages for welcome state
                setMessages([]);
            }
        };

        loadMessages();
        setStreamingStatus(null);
    }, [currentChatId, chatType]);

    /**
     * 解析SSE消息
     * 后端发送格式: {"type":"thinking|status|content|done|error|task_start|task_progress|terminal|file_created","content":"...","data":{...}}
     */
    const parseSSEMessage = useCallback((rawData) => {
        // 如果是 [DONE] 标记
        if (rawData === '[DONE]') {
            return { type: 'done', content: '', data: null };
        }

        // 尝试解析JSON
        try {
            const parsed = JSON.parse(rawData);
            return {
                type: parsed.type || 'content',
                content: parsed.content || '',
                data: parsed.data || null
            };
        } catch (e) {
            // 如果解析失败，当作普通文本内容
            return { type: 'content', content: rawData, data: null };
        }
    }, []);

    /**
     * 处理Manus面板相关的SSE消息
     */
    const handlePanelMessage = useCallback((parsed) => {
        switch (parsed.type) {
            case 'task_start':
                // 任务开始，自动打开面板并初始化任务列表
                setIsPanelOpen(true);
                if (parsed.data?.tasks) {
                    setPanelData(prev => ({
                        ...prev,
                        tasks: parsed.data.tasks.map((name, idx) => ({
                            name,
                            status: idx === 0 ? 'active' : 'pending'
                        }))
                    }));
                }
                // 添加终端启动信息
                setPanelData(prev => ({
                    ...prev,
                    terminalLines: [
                        { type: 'prompt', content: 'ubuntu@sandbox:~/Center_ $ ' },
                        { type: 'command', content: 'manus --start-task' },
                        { type: 'output', content: '正在启动任务...' }
                    ]
                }));
                break;

            case 'task_progress':
                // 更新任务进度
                if (parsed.data?.step !== undefined) {
                    setPanelData(prev => ({
                        ...prev,
                        tasks: prev.tasks.map((task, idx) => ({
                            ...task,
                            status: idx < parsed.data.step ? 'completed' :
                                idx === parsed.data.step ? 'active' : 'pending'
                        }))
                    }));
                }
                break;

            case 'terminal':
                // 添加终端输出
                setPanelData(prev => ({
                    ...prev,
                    terminalLines: [
                        ...prev.terminalLines,
                        { type: 'prompt', content: 'ubuntu@sandbox:~/Center_ $ ' },
                        { type: 'command', content: parsed.data?.command || parsed.content },
                        ...(parsed.data?.output ? [{ type: 'output', content: parsed.data.output }] : [])
                    ]
                }));
                break;

            case 'file_created':
                // 文件生成通知
                if (parsed.data) {
                    setPanelData(prev => ({
                        ...prev,
                        files: [...prev.files, {
                            type: parsed.data.type || 'pdf',
                            name: parsed.data.name || 'file',
                            path: parsed.data.path || '',
                            url: parsed.data.url || ''
                        }],
                        terminalLines: [
                            ...prev.terminalLines,
                            { type: 'output', content: `✓ 文件已创建: ${parsed.data.name}` }
                        ]
                    }));
                }
                break;

            default:
                break;
        }
    }, []);

    // Send message handler
    const handleSendMessage = useCallback(() => {
        if (!inputValue.trim() || isLoading) return;

        if (!currentChatId) {
            window.alert('请先新建或选择对话再发送。');
            return;
        }

        const userMessage = inputValue.trim();

        // Add user message
        setMessages(prev => [...prev, { role: 'user', content: userMessage }]);
        setInputValue('');
        setIsLoading(true);
        setStreamingStatus({ type: 'thinking', content: '正在思考中...' });

        // Reset panel data for new message (only for coach mode)
        if (showManusPanel) {
            setPanelData({
                tasks: [],
                terminalLines: [],
                files: []
            });
        }

        // Update chat title if this is the first user message
        setChatList(prev => prev.map(chat => {
            if (chat.id === currentChatId && chat.title === '新的对话') {
                const title = userMessage.length > 20
                    ? userMessage.substring(0, 20) + '...'
                    : userMessage;
                return { ...chat, title };
            }
            return chat;
        }));

        // Cleanup any existing connection
        cleanupSSE();
        currentResponseRef.current = '';

        // Choose SSE function based on chat type
        const createSSE = chatType === 'coach' ? createCoachSSE : createLoveAppSSE;

        // SSE callbacks
        const callbacks = {
            onData: (data) => {
                const parsed = parseSSEMessage(data);
                // Handle Manus panel messages (only for coach mode)
                if (showManusPanel) {
                    handlePanelMessage(parsed);
                }

                switch (parsed.type) {
                    case 'thinking':
                    case 'status':
                    case 'tool_call':
                        // 更新状态显示
                        setStreamingStatus({ type: parsed.type, content: parsed.content });

                        // For coach mode, also add to terminal
                        if (showManusPanel && parsed.content) {
                            // Auto-open panel when AI starts working
                            setIsPanelOpen(true);
                            setPanelData(prev => ({
                                ...prev,
                                terminalLines: [
                                    ...prev.terminalLines,
                                    { type: 'output', content: `[${parsed.type}] ${parsed.content}` }
                                ]
                            }));
                        }
                        break;

                    case 'content':
                        // 有实际内容时，隐藏状态并追加内容
                        if (parsed.content) {
                            setStreamingStatus(null);
                            currentResponseRef.current += parsed.content;

                            // Update streaming message in real-time
                            setMessages(prev => {
                                const lastMsg = prev[prev.length - 1];
                                if (lastMsg?.role === 'assistant' && lastMsg.isStreaming) {
                                    // Update existing streaming message
                                    return [
                                        ...prev.slice(0, -1),
                                        { ...lastMsg, content: currentResponseRef.current }
                                    ];
                                } else {
                                    // Add new streaming message
                                    return [
                                        ...prev,
                                        { role: 'assistant', content: currentResponseRef.current, isStreaming: true }
                                    ];
                                }
                            });
                        }
                        break;

                    case 'done':
                        // 完成，清理状态并移除光标
                        setStreamingStatus(null);
                        setMessages(prev => {
                            const lastMsg = prev[prev.length - 1];
                            if (lastMsg?.isStreaming) {
                                return [
                                    ...prev.slice(0, -1),
                                    { ...lastMsg, isStreaming: false }
                                ];
                            }
                            return prev;
                        });

                        // Mark all tasks as completed
                        if (showManusPanel) {
                            setPanelData(prev => ({
                                ...prev,
                                tasks: prev.tasks.map(task => ({ ...task, status: 'completed' })),
                                terminalLines: [
                                    ...prev.terminalLines,
                                    { type: 'output', content: '✓ 任务完成' }
                                ]
                            }));
                        }
                        break;

                    case 'error':
                        // 显示错误状态
                        setStreamingStatus({ type: 'error', content: parsed.content });

                        // Add error to terminal
                        if (showManusPanel) {
                            setPanelData(prev => ({
                                ...prev,
                                terminalLines: [
                                    ...prev.terminalLines,
                                    { type: 'error', content: `错误: ${parsed.content}` }
                                ]
                            }));
                        }
                        break;

                    case 'task_start':
                    case 'task_progress':
                    case 'terminal':
                    case 'file_created':
                        // Already handled by handlePanelMessage
                        break;

                    default:
                        // 未知类型，当作内容处理
                        if (parsed.content) {
                            currentResponseRef.current += parsed.content;
                            setMessages(prev => {
                                const lastMsg = prev[prev.length - 1];
                                if (lastMsg?.role === 'assistant' && lastMsg.isStreaming) {
                                    return [
                                        ...prev.slice(0, -1),
                                        { ...lastMsg, content: currentResponseRef.current }
                                    ];
                                } else {
                                    return [
                                        ...prev,
                                        { role: 'assistant', content: currentResponseRef.current, isStreaming: true }
                                    ];
                                }
                            });
                        }
                }
            },
            onError: (error) => {
                console.error('SSE Error:', error);

                // Show error message if no response received
                if (!currentResponseRef.current) {
                    setMessages(prev => [
                        ...prev,
                        { role: 'assistant', content: '抱歉，连接出现问题，请稍后重试。' }
                    ]);
                }

                cleanupSSE({ resetResponse: false });
                currentResponseRef.current = '';
            },
            onComplete: () => {
                setStreamingStatus(null);

                // Finalize the streaming message
                setMessages(prev => {
                    const lastMsg = prev[prev.length - 1];
                    if (lastMsg?.isStreaming) {
                        return [
                            ...prev.slice(0, -1),
                            { role: 'assistant', content: currentResponseRef.current }
                        ];
                    }
                    return prev;
                });

                cleanupSSE({ resetResponse: false });
                currentResponseRef.current = '';
            }
        };

        // Create SSE connection
        try {
            eventSourceRef.current = createSSE(userMessage, currentChatId, callbacks);
        } catch (error) {
            console.error('Failed to create SSE:', error);
            setIsLoading(false);
            setStreamingStatus(null);
            setMessages(prev => [
                ...prev,
                { role: 'assistant', content: '无法连接到服务器，请检查后端是否运行。' }
            ]);
        }
    }, [inputValue, isLoading, chatType, currentChatId, cleanupSSE, parseSSEMessage, showManusPanel, handlePanelMessage]);

    // New chat handler
    const buildNewChat = useCallback(() => ({ id: `chat_${Date.now()}`, title: '新的对话' }), []);

    const resetPanel = useCallback(() => {
        setPanelData({
            tasks: [],
            terminalLines: [],
            files: []
        });
        setIsPanelOpen(false);
    }, []);

    const handleNewChat = useCallback(() => {
        const newChat = buildNewChat();
        setChatList(prev => [newChat, ...prev]);
        setCurrentChatId(newChat.id);
        setMessages([]);
        cleanupSSE();
        resetPanel();
    }, [buildNewChat, cleanupSSE, resetPanel]);

    // Delete chat handler
    const handleDeleteChat = useCallback(async (chatId) => {
        try {
            // Pass chat type for correct directory
            await deleteChatSession(chatId, chatType);
            setChatList(prev => {
                const remaining = prev.filter(c => c.id !== chatId);

                if (chatId === currentChatId) {
                    if (remaining.length > 0) {
                        setCurrentChatId(remaining[0].id);
                        cleanupSSE();
                    } else {
                        const newChat = buildNewChat();
                        setCurrentChatId(newChat.id);
                        setMessages([]);
                        resetPanel();
                        cleanupSSE();
                        return [newChat];
                    }
                }

                return remaining;
            });
        } catch (error) {
            console.error('Failed to delete chat:', error);
        }
    }, [buildNewChat, cleanupSSE, currentChatId, resetPanel, chatType]);

    // Toggle panel handler
    const handleTogglePanel = useCallback(() => {
        setIsPanelOpen(prev => !prev);
    }, []);

    return (
        <div style={{
            display: 'flex',
            height: '100vh',
            overflow: 'hidden',
            background: 'linear-gradient(180deg, #FFF1F2 0%, #FFE4E6 50%, #FDF2F8 100%)'
        }}>
            <ChatSidebar
                isOpen={isSidebarOpen}
                onToggle={() => setIsSidebarOpen(!isSidebarOpen)}
                chatList={chatList}
                currentChatId={currentChatId}
                onSelectChat={setCurrentChatId}
                onNewChat={handleNewChat}
                onDeleteChat={handleDeleteChat}
            />

            <main style={{
                flex: 1,
                height: '100vh',
                position: 'relative',
                display: 'flex',
                marginLeft: isSidebarOpen ? '280px' : '0',
                transition: 'margin-left 0.2s ease'
            }}>
                <div style={{ flex: 1, height: '100%', minWidth: 0 }}>
                    <ChatArea
                        messages={messages}
                        inputValue={inputValue}
                        setInputValue={setInputValue}
                        onSendMessage={handleSendMessage}
                        isLoading={isLoading}
                        streamingStatus={streamingStatus}
                        chatType={chatType}
                    />
                </div>

                {/* Manus Panel (only for coach mode) */}
                {showManusPanel && (
                    <ManusPanel
                        isOpen={isPanelOpen}
                        onToggle={handleTogglePanel}
                        panelData={panelData}
                    />
                )}
            </main>
        </div>
    );
};

export default ChatPage;
