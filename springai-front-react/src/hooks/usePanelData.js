import { useState, useCallback, useEffect } from 'react';
/* eslint-disable react-hooks/set-state-in-effect */

/**
 * Custom hook for managing Manus panel data
 * Handles tasks, terminal lines, file previews, and panel open state
 *
 * @param {string} currentChatId - Current chat ID
 * @param {boolean} showManusPanel - Whether the panel should be shown
 * @param {Object} storageHooks - Storage hooks from useChatStorage
 * @returns {Object} Panel state and handlers
 */
export const usePanelData = (currentChatId, showManusPanel, storageHooks) => {
    const { loadPanelDataFromStorage, savePanelDataToStorage } = storageHooks;

    // Combine state to reduce renders
    const [state, setState] = useState({
        isPanelOpen: false,
        panelData: {
            tasks: [],
            terminalLines: [],
            files: []
        }
    });

    const resetPanel = useCallback(() => {
        setState({
            isPanelOpen: false,
            panelData: {
                tasks: [],
                terminalLines: [],
                files: []
            }
        });
    }, []);

    // Load panel data when chat changes
    useEffect(() => {
        if (currentChatId && showManusPanel) {
            const stored = loadPanelDataFromStorage(currentChatId);
            const defaultPanelData = {
                tasks: [],
                terminalLines: [],
                files: []
            };

            const newData = stored || defaultPanelData;
            const shouldOpen = stored && (stored.files.length > 0 || stored.terminalLines.length > 0);

            setState({
                isPanelOpen: shouldOpen,
                panelData: newData
            });
        } else {
            setState(prev => ({ ...prev, isPanelOpen: false }));
        }
    }, [currentChatId, showManusPanel, loadPanelDataFromStorage]);

    // Save panel data to storage when it changes
    useEffect(() => {
        if (currentChatId && showManusPanel) {
            savePanelDataToStorage(currentChatId, state.panelData);
        }
    }, [state.panelData, currentChatId, showManusPanel, savePanelDataToStorage]);

    // Handle panel-related SSE messages
    const handlePanelMessage = useCallback((parsed) => {
        setState(prev => {
            const newPanelData = { ...prev.panelData };

            switch (parsed.type) {
                case 'task_start':
                    if (parsed.data?.tasks) {
                        newPanelData.tasks = parsed.data.tasks.map((name, idx) => ({
                            name,
                            status: idx === 0 ? 'active' : 'pending'
                        }));
                    }
                    newPanelData.terminalLines = [
                        { type: 'prompt', content: 'ubuntu@sandbox:~/Center_ $ ' },
                        { type: 'command', content: 'manus --start-task' },
                        { type: 'output', content: '正在启动任务...' }
                    ];
                    return { isPanelOpen: true, panelData: newPanelData };

                case 'task_progress':
                    if (parsed.data?.step !== undefined) {
                        newPanelData.tasks = prev.panelData.tasks.map((task, idx) => ({
                            ...task,
                            status: idx < parsed.data.step ? 'completed' :
                                idx === parsed.data.step ? 'active' : 'pending'
                        }));
                    }
                    return { ...prev, panelData: newPanelData };

                case 'terminal':
                    newPanelData.terminalLines = [
                        ...prev.panelData.terminalLines,
                        { type: 'prompt', content: 'ubuntu@sandbox:~/Center_ $ ' },
                        { type: 'command', content: parsed.data?.command || parsed.content },
                        ...(parsed.data?.output ? [{ type: 'output', content: parsed.data.output }] : [])
                    ];
                    return { ...prev, panelData: newPanelData };

                case 'file_created':
                    if (parsed.data) {
                        newPanelData.files = [...prev.panelData.files, {
                            type: parsed.data.type || 'pdf',
                            name: parsed.data.name || 'file',
                            path: parsed.data.path || '',
                            url: parsed.data.url || ''
                        }];
                        newPanelData.terminalLines = [
                            ...prev.panelData.terminalLines,
                            { type: 'output', content: `✓ 文件已创建: ${parsed.data.name}` }
                        ];
                    }
                    return { ...prev, panelData: newPanelData };

                default:
                    return prev;
            }
        });
    }, []);

    const addTerminalOutput = useCallback((content, type = 'output') => {
        setState(prev => ({
            ...prev,
            panelData: {
                ...prev.panelData,
                terminalLines: [...prev.panelData.terminalLines, { type, content }]
            }
        }));
    }, []);

    const completeAllTasks = useCallback(() => {
        setState(prev => ({
            ...prev,
            panelData: {
                ...prev.panelData,
                tasks: prev.panelData.tasks.map(task => ({ ...task, status: 'completed' })),
                terminalLines: [
                    ...prev.panelData.terminalLines,
                    { type: 'output', content: '✓ 任务完成' }
                ]
            }
        }));
    }, []);

    const togglePanel = useCallback(() => {
        setState(prev => ({ ...prev, isPanelOpen: !prev.isPanelOpen }));
    }, []);

    const setIsPanelOpen = useCallback((value) => {
        setState(prev => ({ ...prev, isPanelOpen: value }));
    }, []);

    const setPanelData = useCallback((data) => {
        setState(prev => ({ ...prev, panelData: data }));
    }, []);

    return {
        // State
        isPanelOpen: state.isPanelOpen,
        panelData: state.panelData,

        // Setters
        setIsPanelOpen,
        setPanelData,

        // Actions
        resetPanel,
        handlePanelMessage,
        addTerminalOutput,
        completeAllTasks,
        togglePanel
    };
};
