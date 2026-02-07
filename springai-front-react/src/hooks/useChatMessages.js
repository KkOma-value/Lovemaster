import { useState, useCallback, useRef } from 'react';

/**
 * Custom hook for managing chat messages and streaming state
 * Handles message list, input value, loading state, and streaming status
 *
 * @returns {Object} Message state and handlers
 */
export const useChatMessages = () => {
    const [messages, setMessages] = useState([]);
    const [inputValue, setInputValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [streamingStatus, setStreamingStatus] = useState(null);
    const currentResponseRef = useRef('');

    const addStreamingContent = useCallback((newChunk) => {
        setMessages(prev => {
            const lastMsg = prev[prev.length - 1];
            if (lastMsg?.role === 'assistant' && lastMsg.isStreaming) {
                const updatedContent = lastMsg.content + newChunk;
                currentResponseRef.current = updatedContent;
                return [
                    ...prev.slice(0, -1),
                    { ...lastMsg, content: updatedContent }
                ];
            }
            currentResponseRef.current = newChunk;
            return [
                ...prev,
                { role: 'assistant', content: newChunk, isStreaming: true }
            ];
        });
    }, []);

    const finalizeStreaming = useCallback(() => {
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
        setIsLoading(false);
    }, []);

    const addUserMessage = useCallback((content) => {
        setMessages(prev => [...prev, { role: 'user', content }]);
    }, []);

    const addErrorMessage = useCallback((message) => {
        setMessages(prev => [...prev, { role: 'assistant', content: message }]);
    }, []);

    const resetStreaming = useCallback(() => {
        currentResponseRef.current = '';
        setStreamingStatus(null);
        setIsLoading(false);
    }, []);

    const setMessagesDirect = useCallback((msgs) => {
        setMessages(msgs);
    }, []);

    const clearMessages = useCallback(() => {
        setMessages([]);
    }, []);

    return {
        // State
        messages,
        inputValue,
        isLoading,
        streamingStatus,
        currentResponseRef,

        // Setters
        setInputValue,
        setStreamingStatus,
        setIsLoading,

        // Actions
        addStreamingContent,
        finalizeStreaming,
        addUserMessage,
        addErrorMessage,
        resetStreaming,
        setMessagesDirect,
        clearMessages
    };
};
