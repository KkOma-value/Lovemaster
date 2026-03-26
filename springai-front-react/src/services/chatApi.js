/**
 * Chat API Service - SSE Streaming
 * Ported from Vue implementation
 */

const API_BASE = '/api';

const authFetch = async (url, options = {}) => {
    const token = localStorage.getItem('accessToken');
    const headers = {
        ...options.headers,
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
        if (response.status === 401) {
            console.error('Unauthorized request. Token might be invalid or expired.');
            // Add automatic login redirect or token refresh strategy here if preferred
        }
        throw new Error(`Failed request: ${response.statusText}`);
    }
    return response.json();
};

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
export function createLoveAppSSE(message, chatId, imageUrl, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }
    if (imageUrl) {
        params.append('imageUrl', imageUrl);
    }
    const token = localStorage.getItem('accessToken');
    if (token) {
        params.append('token', token);
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
export function createCoachSSE(message, chatId, imageUrl, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }
    if (imageUrl) {
        params.append('imageUrl', imageUrl);
    }
    const token = localStorage.getItem('accessToken');
    if (token) {
        params.append('token', token);
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
    return authFetch(`${API_BASE}/ai/sessions?chatType=${chatType}`);
}

/**
 * Delete a chat session
 * @param {string} chatId 
 * @param {string} chatType - 'loveapp' or 'coach'
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function deleteChatSession(chatId, chatType = 'loveapp') {
    return authFetch(`${API_BASE}/ai/sessions/${encodeURIComponent(chatId)}?chatType=${chatType}`, {
        method: 'DELETE'
    });
}

/**
 * Get messages for a specific chat session
 * @param {string} chatId 
 * @param {string} chatType - 'loveapp' or 'coach'
 * @param {number} limit 
 * @returns {Promise<Array<{role: string, content: string}>>}
 */
export async function getChatMessages(chatId, chatType = 'loveapp', limit = 100) {
    return authFetch(`${API_BASE}/ai/sessions/${encodeURIComponent(chatId)}/messages?chatType=${chatType}&limit=${limit}`);
}
