import { useState, useCallback, useEffect } from 'react';
import { getChatSessions, deleteChatSession } from '../services/chatApi';

/**
 * Custom hook for managing chat sessions
 * Handles loading sessions, selecting chats, creating new chats, deleting chats
 *
 * @param {string} chatType - 'loveapp' or 'coach'
 * @param {Function} cleanupSSE - Cleanup function for SSE connections
 * @param {Function} resetPanel - Reset function for panel data
 * @returns {Object} Session state and handlers
 */
export const useChatSessions = (chatType, cleanupSSE, resetPanel) => {
    const [chatList, setChatList] = useState([]);
    const [currentChatId, setCurrentChatId] = useState(null);

    const createNewChat = useCallback(() => {
        const newId = `chat_${Date.now()}`;
        const newChat = { id: newId, title: '新的对话' };
        setChatList(prev => [newChat, ...prev]);
        setCurrentChatId(newId);
        return newChat;
    }, []);

    // Load sessions from backend on mount
    useEffect(() => {
        const loadSessions = async () => {
            try {
                const sessions = await getChatSessions(chatType);
                if (sessions.length > 0) {
                    setChatList(sessions);
                    setCurrentChatId(sessions[0].id);
                } else {
                    createNewChat();
                }
            } catch (error) {
                console.error('Failed to load sessions:', error);
                createNewChat();
            }
        };

        loadSessions();
    }, [chatType, createNewChat]);

    const deleteChat = useCallback(async (chatId, removePanelDataFromStorage) => {
        try {
            await deleteChatSession(chatId, chatType);
            removePanelDataFromStorage(chatId);

            setChatList(prev => {
                const remaining = prev.filter(c => c.id !== chatId);

                if (chatId === currentChatId) {
                    if (remaining.length > 0) {
                        setCurrentChatId(remaining[0].id);
                        cleanupSSE();
                    } else {
                        const newChat = createNewChat();
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
    }, [chatType, currentChatId, cleanupSSE, createNewChat, resetPanel]);

    const updateChatTitle = useCallback((chatId, title) => {
        setChatList(prev => prev.map(chat => {
            if (chat.id === chatId) {
                return { ...chat, title };
            }
            return chat;
        }));
    }, []);

    return {
        // State
        chatList,
        currentChatId,
        setCurrentChatId,

        // Actions
        createNewChat,
        deleteChat,
        updateChatTitle,
        setChatList
    };
};
