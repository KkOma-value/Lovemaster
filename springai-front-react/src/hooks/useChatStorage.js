import { useCallback } from 'react';

/**
 * Custom hook for localStorage operations related to panel data
 * Handles saving, loading, and removing panel data for coach chats
 *
 * @param {string} chatType - 'loveapp' or 'coach'
 * @returns {Object} { savePanelDataToStorage, loadPanelDataFromStorage, removePanelDataFromStorage }
 */
export const useChatStorage = (chatType) => {
    const getPanelDataKey = useCallback((chatId) => `coach_panel_${chatId}`, []);

    const savePanelDataToStorage = useCallback((chatId, data) => {
        if (!chatId || chatType !== 'coach') return;
        try {
            // Only save if there's meaningful data
            if (data.tasks.length > 0 || data.terminalLines.length > 0 || data.files.length > 0) {
                localStorage.setItem(getPanelDataKey(chatId), JSON.stringify(data));
            }
        } catch (e) {
            console.warn('Failed to save panel data:', e);
        }
    }, [chatType, getPanelDataKey]);

    const loadPanelDataFromStorage = useCallback((chatId) => {
        if (!chatId || chatType !== 'coach') return null;
        try {
            const stored = localStorage.getItem(getPanelDataKey(chatId));
            if (stored) {
                return JSON.parse(stored);
            }
        } catch (e) {
            console.warn('Failed to load panel data:', e);
        }
        return null;
    }, [chatType, getPanelDataKey]);

    const removePanelDataFromStorage = useCallback((chatId) => {
        if (!chatId) return;
        try {
            localStorage.removeItem(getPanelDataKey(chatId));
        } catch (e) {
            console.warn('Failed to remove panel data:', e);
        }
    }, [getPanelDataKey]);

    return {
        savePanelDataToStorage,
        loadPanelDataFromStorage,
        removePanelDataFromStorage
    };
};
