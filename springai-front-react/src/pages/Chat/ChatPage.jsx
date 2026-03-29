import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import ChatSidebar from '../../components/Sidebar/ChatSidebar';
import ChatArea from '../../components/Chat/ChatArea';
import ManusPanel from '../../components/ManusPanel/ManusPanel';
import { getChatMessages } from '../../services/chatApi';
import { useSSEConnection } from '../../hooks/useSSEConnection';
import { useChatMessages } from '../../hooks/useChatMessages';
import { useChatStorage } from '../../hooks/useChatStorage';
import { usePanelData } from '../../hooks/usePanelData';
import { useChatSessions } from '../../hooks/useChatSessions';
import styles from './ChatPage.module.css';

const ChatPage = () => {
    const { type } = useParams();
    const chatType = type || 'loveapp';
    const showManusPanel = chatType === 'coach';
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    // SSE connection
    const { connectSSE, cleanupSSE } = useSSEConnection(chatType);

    // Message state
    const {
        messages,
        inputValue,
        isLoading,
        streamingStatus,
        setInputValue,
        setStreamingStatus,
        setIsLoading,
        addStreamingContent,
        finalizeStreaming,
        addUserMessage,
        addErrorMessage,
        setMessagesDirect
    } = useChatMessages();

    // Storage
    const storageHooks = useChatStorage(chatType);

    // Panel data (initial instance, will be re-created with currentChatId)
    const panelDataHook = usePanelData(null, showManusPanel, storageHooks);

    // Chat sessions
    const {
        chatList,
        currentChatId,
        setCurrentChatId,
        createNewChat,
        autoCreateChat,
        deleteChat,
        updateChatTitle
    } = useChatSessions(chatType, cleanupSSE, panelDataHook.resetPanel);

    // Sync currentChatId to panelData hook (breaks circular dependency)
    const { setChatId: setPanelChatId } = panelDataHook;
    useEffect(() => {
        setPanelChatId(currentChatId);
    }, [currentChatId, setPanelChatId]);

    // Load messages when currentChatId changes
    useEffect(() => {
        if (!currentChatId) return;

        const loadMessages = async () => {
            try {
                const msgs = await getChatMessages(currentChatId, chatType);
                setMessagesDirect(msgs.length > 0 ? msgs : []);
            } catch (error) {
                console.error('Failed to load messages:', error);
                setMessagesDirect([]);
            }
        };

        loadMessages();
        setStreamingStatus(null);
    }, [currentChatId, chatType, setMessagesDirect, setStreamingStatus]);

    // Refresh messages from backend
    const refreshMessages = useCallback(async (chatId = currentChatId) => {
        if (!chatId) return;
        try {
            const msgs = await getChatMessages(chatId, chatType);
            if (msgs.length > 0) {
                setMessagesDirect(msgs);
            }
        } catch (error) {
            console.error('Failed to refresh messages:', error);
        }
    }, [currentChatId, chatType, setMessagesDirect]);

    // Send message handler
    const handleSendMessage = useCallback((msgText = inputValue, imageUrl = null) => {
        const textToUse = msgText || inputValue;
        if ((!textToUse.trim() && !imageUrl) || isLoading) return;

        // Auto-create chat if no current chat exists
        let activeChatId = currentChatId;
        if (!activeChatId) {
            const newChat = autoCreateChat(textToUse.trim());
            activeChatId = newChat.id;
        }

        const userMessage = textToUse.trim();

        // Cleanup any existing connection
        cleanupSSE({ resetResponse: true });

        // Add user message
        addUserMessage(userMessage, imageUrl);
        setInputValue('');
        setIsLoading(true);
        setStreamingStatus({
            type: imageUrl ? 'intake_status' : 'thinking',
            content: imageUrl
                ? (showManusPanel
                    ? '正在读取聊天截图，并判断这件事要不要进入任务执行...'
                    : '正在识别聊天截图，准备给你回复建议...')
                : (showManusPanel
                    ? '正在整理问题，并判断是否需要继续执行任务...'
                    : '正在分析对话语气，准备回复建议...')
        });

        // Reset panel data for new message
        if (showManusPanel) {
            panelDataHook.resetPanel();
        }

        // Update chat title if this is the first user message
        const currentChat = chatList.find(c => c.id === activeChatId);
        if (currentChat?.title === '新的对话') {
            const title = userMessage.length > 20
                ? userMessage.substring(0, 20) + '...'
                : userMessage;
            updateChatTitle(activeChatId, title);
        }

        // SSE message handlers
        const handleMessage = (parsed) => {
            // Handle panel messages
            if (showManusPanel) {
                panelDataHook.handlePanelMessage(parsed);
            }

            switch (parsed.type) {
                case 'thinking':
                case 'status':
                case 'intake_status':
                case 'ocr_result':
                case 'rewrite_result':
                    setStreamingStatus({ type: parsed.type, content: parsed.content });
                    if (showManusPanel && panelDataHook.isPanelOpen && parsed.content) {
                        panelDataHook.addTerminalOutput(`[${parsed.type}] ${parsed.content}`);
                    }
                    break;

                case 'tool_call':
                    setStreamingStatus({ type: parsed.type, content: parsed.content });
                    if (showManusPanel && parsed.content) {
                        panelDataHook.setIsPanelOpen(true);
                        panelDataHook.addTerminalOutput(`[${parsed.type}] ${parsed.content}`);
                    }
                    break;

                case 'content':
                    if (parsed.content) {
                        setStreamingStatus(null);
                        addStreamingContent(parsed.content);
                    }
                    break;

                case 'done':
                    // finalizeStreaming now handles skipping typewriter and showing all content
                    finalizeStreaming();
                    if (showManusPanel) {
                        panelDataHook.completeAllTasks();
                    }
                    refreshMessages();
                    break;

                case 'error':
                    setStreamingStatus({ type: 'error', content: parsed.content });
                    addErrorMessage(parsed.content || '抱歉，AI 服务暂时不可用。');
                    finalizeStreaming();
                    if (showManusPanel) {
                        panelDataHook.addTerminalOutput(`错误: ${parsed.content}`, 'error');
                    }
                    break;

                default:
                    if (parsed.content) {
                        addStreamingContent(parsed.content);
                    }
            }
        };

        const handleError = (error) => {
            console.error('SSE Error:', error);
            // Always add error message and reset loading state
            addErrorMessage('抱歉，连接出现问题，请稍后重试。');
            finalizeStreaming();
            setIsLoading(false);
            setStreamingStatus(null);
        };

        const handleComplete = () => {
            setStreamingStatus(null);
            finalizeStreaming();
            setIsLoading(false);
            refreshMessages();
        };

        // Create SSE connection
        connectSSE(userMessage, activeChatId, imageUrl, {
            onData: handleMessage,
            onError: handleError,
            onComplete: handleComplete
        });
    }, [
        inputValue, isLoading, currentChatId, chatList,
        cleanupSSE, connectSSE, showManusPanel,
        addUserMessage, setInputValue, setIsLoading, setStreamingStatus,
        addStreamingContent, finalizeStreaming, addErrorMessage,
        updateChatTitle, refreshMessages, panelDataHook, autoCreateChat
    ]);

    // New chat handler
    const handleNewChat = useCallback(() => {
        createNewChat();
        setMessagesDirect([]);
        cleanupSSE();
        panelDataHook.resetPanel();
    }, [createNewChat, setMessagesDirect, cleanupSSE, panelDataHook]);

    // Delete chat handler
    const handleDeleteChat = useCallback(async (chatId) => {
        await deleteChat(chatId, storageHooks.removePanelDataFromStorage);
    }, [deleteChat, storageHooks.removePanelDataFromStorage]);

    const handleToggleSidebar = useCallback(() => {
        setIsSidebarOpen(prev => !prev);
    }, []);

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
                    />
                </div>

                {showManusPanel && (
                    <ManusPanel
                        isOpen={panelDataHook.isPanelOpen}
                        onToggle={panelDataHook.togglePanel}
                        panelData={panelDataHook.panelData}
                    />
                )}
            </main>
        </motion.div>
    );
};

export default ChatPage;
