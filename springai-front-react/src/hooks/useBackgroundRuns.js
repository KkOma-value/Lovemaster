import { useState, useCallback, useRef } from 'react';

export const useBackgroundRuns = () => {
    // Map<chatId, { status: 'generating'|'completed'|'failed', error?: string }>
    const [runs, setRuns] = useState({});
    // Map<chatId, EventSource> — kept in ref so state updates don't close connections
    const sourcesRef = useRef({});

    const registerRun = useCallback((chatId, eventSource) => {
        sourcesRef.current[chatId] = eventSource;
        setRuns(prev => ({ ...prev, [chatId]: { status: 'generating' } }));
    }, []);

    const completeRun = useCallback((chatId) => {
        const es = sourcesRef.current[chatId];
        if (es) {
            es.close();
            delete sourcesRef.current[chatId];
        }
        setRuns(prev => {
            if (!prev[chatId]) return prev;
            return { ...prev, [chatId]: { status: 'completed' } };
        });
    }, []);

    const failRun = useCallback((chatId, error) => {
        const es = sourcesRef.current[chatId];
        if (es) {
            es.close();
            delete sourcesRef.current[chatId];
        }
        setRuns(prev => {
            if (!prev[chatId]) return prev;
            return { ...prev, [chatId]: { status: 'failed', error } };
        });
    }, []);

    const clearRun = useCallback((chatId) => {
        const es = sourcesRef.current[chatId];
        if (es) {
            es.close();
            delete sourcesRef.current[chatId];
        }
        setRuns(prev => {
            const next = { ...prev };
            delete next[chatId];
            return next;
        });
    }, []);

    const getRunStatus = useCallback((chatId) => {
        return runs[chatId] || null;
    }, [runs]);

    const activeRunCount = Object.values(runs).filter(r => r.status === 'generating').length;

    return {
        runs,
        activeRunCount,
        registerRun,
        completeRun,
        failRun,
        clearRun,
        getRunStatus
    };
};
