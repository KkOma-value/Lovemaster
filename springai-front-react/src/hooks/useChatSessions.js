import { useState, useCallback, useEffect } from 'react';
import { getChatSessions, deleteChatSession } from '../services/chatApi';

/**
 * Custom hook for managing chat sessions
 * Handles loading sessions, selecting chats, creating new chats, deleting chats
 *
 * @param {string} chatType - 'loveapp' or 'coach'
 * @param {Function} stopChatRuntime - Stop function for active chat runtime
 * @param {Function} resetPanel - Reset function for panel data
 * @returns {Object} Session state and handlers
 */
export const useChatSessions = (chatType, stopChatRuntime, resetPanel) => {
    const [chatList, setChatList] = useState([]);
    const [currentChatId, setCurrentChatId] = useState(null);

    const createNewChat = useCallback(() => {
        const newId = `chat_${Date.now()}`;
        const newChat = { id: newId, title: '新的对话' };
        setChatList(prev => [newChat, ...prev]);
        setCurrentChatId(newId);
        return newChat;
    }, []);

    // Auto-create a new chat when user sends first message
    const autoCreateChat = useCallback((messageText) => {
        const newId = `chat_${Date.now()}`;
        const title = messageText.length > 20
            ? messageText.substring(0, 20) + '...'
            : messageText;
        const newChat = { id: newId, title };
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
                }
                // Note: No longer auto-create initial chats - they will be created on first message
            } catch (error) {
                console.error('Failed to load sessions:', error);
            }
        };

        loadSessions();
    }, [chatType]);

    const deleteChat = useCallback(async (chatId, removePanelDataFromStorage) => {
        try {
            await deleteChatSession(chatId, chatType);
            removePanelDataFromStorage(chatId);
            stopChatRuntime(chatId);

            setChatList(prev => {
                const remaining = prev.filter(c => c.id !== chatId);

                if (chatId === currentChatId) {
                    if (remaining.length > 0) {
                        setCurrentChatId(remaining[0].id);
                    } else {
                        const newChat = createNewChat();
                        resetPanel();
                        return [newChat];
                    }
                }

                return remaining;
            });
        } catch (error) {
            console.error('Failed to delete chat:', error);
        }
    }, [chatType, currentChatId, stopChatRuntime, createNewChat, resetPanel]);

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
        autoCreateChat,
        deleteChat,
        updateChatTitle,
        setChatList
    };
};
