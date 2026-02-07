/**
 * Chat API Service - SSE Streaming
 * Ported from Vue implementation
 */

const API_BASE = '/api';

const createSSEConnection = (url, label, { onData, onError, onComplete }, isDone) => {
    console.log(`[${label}] Creating SSE connection to:`, url);
    const eventSource = new EventSource(url);
    let isClosed = false;

    const safeClose = () => {
        if (!isClosed) {
            isClosed = true;
            eventSource.close();
            console.log(`[${label}] Connection closed`);
        }
    };

    eventSource.onopen = () => {
        console.log(`[${label}] Connection opened`);
    };

    eventSource.onmessage = (event) => {
        const data = event.data;
        console.log(`[${label}] Received data:`, data);

        if (isDone(data)) {
            console.log(`[${label}] Done signal received`);
            safeClose();
            onComplete?.();
            return;
        }

        onData?.(data);
    };

    eventSource.onerror = (error) => {
        console.error(`[${label}] Error:`, error);

        if (eventSource.readyState === EventSource.CLOSED && !isClosed) {
            safeClose();
            onComplete?.();
            return;
        }

        safeClose();
        onError?.(error);
    };

    return eventSource;
};

/**
 * Create LoveApp SSE connection
 * @param {string} message - User message
 * @param {string} chatId - Chat session ID (optional)
 * @param {Object} callbacks - { onData, onError, onComplete }
 * @returns {EventSource}
 */
export function createLoveAppSSE(message, chatId, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }

    const url = `${API_BASE}/ai/love_app/chat/sse?${params.toString()}`;
    return createSSEConnection(
        url,
        'LoveApp SSE',
        { onData, onError, onComplete },
        (data) => data === '[DONE]'
    );
}

/**
 * Create Coach/Manus SSE connection
 * @param {string} message - User message
 * @param {string} chatId - Chat session ID (optional)
 * @param {Object} callbacks - { onData, onError, onComplete }
 * @returns {EventSource}
 */
export function createCoachSSE(message, chatId, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }

    const url = `${API_BASE}/ai/manus/chat?${params.toString()}`;
    return createSSEConnection(
        url,
        'Coach SSE',
        { onData, onError, onComplete },
        (data) => data === '[DONE]' || data === ''
    );
}

/**
 * Get all chat sessions
 * @param {string} chatType - 'loveapp' or 'coach'
 * @returns {Promise<Array<{id: string, title: string}>>}
 */
export async function getChatSessions(chatType = 'loveapp') {
    const response = await fetch(`${API_BASE}/ai/sessions?chatType=${chatType}`);
    if (!response.ok) {
        throw new Error('Failed to fetch sessions');
    }
    return response.json();
}

/**
 * Delete a chat session
 * @param {string} chatId 
 * @param {string} chatType - 'loveapp' or 'coach'
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function deleteChatSession(chatId, chatType = 'loveapp') {
    const response = await fetch(`${API_BASE}/ai/sessions/${encodeURIComponent(chatId)}?chatType=${chatType}`, {
        method: 'DELETE'
    });
    if (!response.ok) {
        throw new Error('Failed to delete session');
    }
    return response.json();
}

/**
 * Get messages for a specific chat session
 * @param {string} chatId 
 * @param {string} chatType - 'loveapp' or 'coach'
 * @param {number} limit 
 * @returns {Promise<Array<{role: string, content: string}>>}
 */
export async function getChatMessages(chatId, chatType = 'loveapp', limit = 100) {
    const response = await fetch(`${API_BASE}/ai/sessions/${encodeURIComponent(chatId)}/messages?chatType=${chatType}&limit=${limit}`);
    if (!response.ok) {
        throw new Error('Failed to fetch messages');
    }
    return response.json();
}
