/**
 * Chat API Service - SSE Streaming
 * Ported from Vue implementation
 */

import { AuthExpiredError, getStoredRefreshToken, getValidAccessToken } from './authSession';

const API_BASE = '/api';

const authFetch = async (url, options = {}) => {
    const request = async (token) => {
        const headers = {
            ...options.headers,
        };

        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        return fetch(url, { ...options, headers });
    };

    let token = await getValidAccessToken();
    let response = await request(token);

    if ((response.status === 401 || response.status === 403) && getStoredRefreshToken()) {
        token = await getValidAccessToken({ forceRefresh: true });
        response = await request(token);
    }

    if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
            throw new AuthExpiredError();
        }
        throw new Error(`Failed request: ${response.statusText}`);
    }
    return response.json();
};

const createSSEConnection = (url, label, { onData, onError, onComplete }, isDone) => {
    console.log(`[${label}] Creating SSE connection to:`, url);
    const eventSource = new EventSource(url);
    let isClosed = false;
    let terminalMessageReceived = false;

    const safeClose = () => {
        if (!isClosed) {
            isClosed = true;
            eventSource.close();
            console.log(`[${label}] Connection closed`);
        }
    };

    const isTerminalServerMessage = (rawData) => {
        try {
            const parsed = JSON.parse(rawData);
            return parsed?.type === 'error' || parsed?.type === 'done';
        } catch {
            return false;
        }
    };

    eventSource.onopen = () => {
        console.log(`[${label}] Connection opened`);
    };

    eventSource.onmessage = (event) => {
        const data = event.data;
        console.log(`[${label}] Received data:`, data);

        if (isDone(data)) {
            terminalMessageReceived = true;
            console.log(`[${label}] Done signal received`);
            safeClose();
            onComplete?.();
            return;
        }

        onData?.(data);

        if (isTerminalServerMessage(data)) {
            terminalMessageReceived = true;
            safeClose();
        }
    };

    eventSource.onerror = () => {
        // Suppress all errors after terminal message or intentional close
        if (terminalMessageReceived || isClosed) {
            safeClose();
            return;
        }

        console.warn(`[${label}] Connection error (readyState: ${eventSource.readyState})`);

        if (eventSource.readyState === EventSource.CLOSED) {
            safeClose();
            onError?.(new Error('SSE connection closed unexpectedly'));
            return;
        }

        safeClose();
        onError?.(new Error('SSE connection error'));
    };

    return eventSource;
};

const buildSSEUrl = async (path, params) => {
    const token = await getValidAccessToken();
    params.set('token', token);
    return `${API_BASE}${path}?${params.toString()}`;
};

/**
 * Create LoveApp SSE connection
 * @param {string} message - User message
 * @param {string} chatId - Chat session ID (optional)
 * @param {Object} callbacks - { onData, onError, onComplete }
 * @returns {EventSource}
 */
export async function createLoveAppSSE(message, chatId, imageUrl, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }
    if (imageUrl) {
        params.append('imageUrl', imageUrl);
    }
    const url = await buildSSEUrl('/ai/love_app/chat/sse', params);
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
export async function createCoachSSE(message, chatId, imageUrl, { onData, onError, onComplete }) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (chatId) {
        params.append('chatId', chatId);
    }
    if (imageUrl) {
        params.append('imageUrl', imageUrl);
    }
    const url = await buildSSEUrl('/ai/manus/chat', params);
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

/**
 * Get images for a specific chat session
 * @param {string} chatId
 * @param {string} chatType - 'loveapp' or 'coach'
 * @returns {Promise<Array<{type: string, name: string, url: string}>>}
 */
export async function getChatImages(chatId, chatType = 'coach') {
    return authFetch(`${API_BASE}/ai/sessions/${encodeURIComponent(chatId)}/images?chatType=${chatType}`);
}
