# Chat Background Runs (M1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users switch between chats while AI responses continue generating in the background, with clear status indicators in the sidebar, a global pill, and recovery banners.

**Architecture:** A new `useBackgroundRuns` hook tracks per-chat run status and keeps EventSource connections alive when users navigate away. The sidebar items show generating/completed/failed indicators. A global pill in ChatArea's header shows active background run count. A recovery banner appears when returning to a chat that completed in the background.

**Tech Stack:** React hooks, Framer Motion (animations), Lucide React (icons), CSS Modules

---

## File Structure

**Create:**
- `src/hooks/useBackgroundRuns.js` — Hook managing background SSE connections and per-chat run status
- `src/components/Chat/BackgroundRunsPill.jsx` — Capsule showing active background run count
- `src/components/Chat/BackgroundRunsPill.module.css` — Pill styles
- `src/components/Chat/RecoveryBanner.jsx` — Banner shown when returning to a chat with background result
- `src/components/Chat/RecoveryBanner.module.css` — Banner styles

**Modify:**
- `src/components/Sidebar/ChatSidebarItem.jsx` — Add run status indicator slot
- `src/components/Sidebar/ChatSidebar.module.css` — Status indicator styles (spinner, dot, warning)
- `src/components/Sidebar/ChatSidebar.jsx` — Pass `backgroundRuns` to items
- `src/pages/Chat/ChatPage.jsx` — Integrate hook, detach SSE on chat switch, wire pill/banner/sidebar
- `src/components/Chat/ChatArea.jsx` — Add pill and banner render slots

---

### Task 1: useBackgroundRuns hook

**Files:**
- Create: `src/hooks/useBackgroundRuns.js`

This hook tracks which chats have active/completed/failed background runs. It stores EventSource references so connections stay alive when the user navigates away.

- [ ] **Step 1: Create the hook file**

```javascript
import { useState, useCallback, useRef } from 'react';

/**
 * Tracks background SSE runs per chat.
 *
 * Run statuses: 'generating' | 'completed' | 'failed'
 *
 * When user switches away from a generating chat, the EventSource is
 * transferred here. When the run completes or fails, status updates.
 * When user returns, the run is cleared after they see the result.
 */
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
        // Close and remove the EventSource
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
```

- [ ] **Step 2: Commit**

```bash
git add src/hooks/useBackgroundRuns.js
git commit -m "feat: add useBackgroundRuns hook for tracking background SSE runs"
```

---

### Task 2: ChatSidebarItem status indicator

**Files:**
- Modify: `src/components/Sidebar/ChatSidebarItem.jsx`
- Modify: `src/components/Sidebar/ChatSidebar.module.css`

Add a status slot to the right side of each sidebar item showing generating (spinner), completed-unread (dot), or failed (warning icon).

- [ ] **Step 1: Add status indicator to ChatSidebarItem**

```jsx
import React from 'react';
import { MessageSquare, Trash2, AlertCircle } from 'lucide-react';
import styles from './ChatSidebar.module.css';

const ChatSidebarItem = ({ chat, isActive, onSelect, onDelete, runStatus }) => {
    const handleDelete = (e) => {
        e.stopPropagation();
        if (window.confirm(`确定要删除对话 "${chat.title}" 吗？`)) {
            onDelete(chat.id);
        }
    };

    const renderStatusIndicator = () => {
        if (!runStatus) return null;

        switch (runStatus.status) {
            case 'generating':
                return (
                    <span className={styles.runIndicator} title="生成中" aria-label="生成中">
                        <span className={styles.runSpinner} />
                    </span>
                );
            case 'completed':
                return (
                    <span className={styles.runIndicator} title="刚完成" aria-label="刚完成">
                        <span className={styles.runDot} />
                    </span>
                );
            case 'failed':
                return (
                    <span className={styles.runIndicator} title="失败" aria-label="失败">
                        <AlertCircle size={14} className={styles.runFailedIcon} />
                    </span>
                );
            default:
                return null;
        }
    };

    return (
        <div
            className={`${styles.item} ${isActive ? styles.active : ''}`}
            onClick={() => onSelect(chat.id)}
        >
            <MessageSquare size={16} className={styles.itemIcon} />
            <span className={styles.itemText}>{chat.title}</span>
            {renderStatusIndicator()}
            <button
                className={styles.deleteBtn}
                onClick={handleDelete}
                title="删除对话"
            >
                <Trash2 size={14} />
            </button>
        </div>
    );
};

export default ChatSidebarItem;
```

- [ ] **Step 2: Add CSS for status indicators**

Append to `ChatSidebar.module.css`:

```css
/* Run Status Indicators */
.runIndicator {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
}

.runSpinner {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    border: 1.5px solid rgba(196, 123, 90, 0.3);
    border-top-color: #C47B5A;
    animation: sidebarSpin 1s linear infinite;
}

@keyframes sidebarSpin {
    to { transform: rotate(360deg); }
}

.runDot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #C47B5A;
    animation: dotFadeIn 0.3s ease;
}

@keyframes dotFadeIn {
    from { opacity: 0; transform: scale(0); }
    to { opacity: 1; transform: scale(1); }
}

.runFailedIcon {
    color: #E87A5D;
    flex-shrink: 0;
}

@media (prefers-reduced-motion: reduce) {
    .runSpinner {
        animation: none;
        border-color: #C47B5A;
    }
    .runDot {
        animation: none;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/components/Sidebar/ChatSidebarItem.jsx src/components/Sidebar/ChatSidebar.module.css
git commit -m "feat: add run status indicators to sidebar chat items"
```

---

### Task 3: ChatSidebar passes runStatus to items

**Files:**
- Modify: `src/components/Sidebar/ChatSidebar.jsx`

- [ ] **Step 1: Add backgroundRuns prop and pass runStatus to each item**

In `ChatSidebar.jsx`, add `backgroundRuns` to the destructured props:

```jsx
const ChatSidebar = ({
    isOpen,
    onToggle,
    onNewChat,
    currentChatId,
    chatList = [],
    onSelectChat,
    onDeleteChat,
    backgroundRuns = {}
}) => {
```

Then in the chat list map, pass `runStatus`:

```jsx
<ChatSidebarItem
    chat={chat}
    isActive={currentChatId === chat.id}
    onSelect={onSelectChat}
    onDelete={onDeleteChat}
    runStatus={backgroundRuns[chat.id] || null}
/>
```

- [ ] **Step 2: Commit**

```bash
git add src/components/Sidebar/ChatSidebar.jsx
git commit -m "feat: pass background run status to sidebar items"
```

---

### Task 4: BackgroundRunsPill component

**Files:**
- Create: `src/components/Chat/BackgroundRunsPill.jsx`
- Create: `src/components/Chat/BackgroundRunsPill.module.css`

A small capsule in the chat header showing how many responses are generating in the background. Clicking it navigates to the first active run's chat.

- [ ] **Step 1: Create the pill component**

```jsx
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import styles from './BackgroundRunsPill.module.css';

const BackgroundRunsPill = ({ activeCount, onNavigate }) => {
    return (
        <AnimatePresence>
            {activeCount > 0 && (
                <motion.button
                    className={styles.pill}
                    onClick={onNavigate}
                    initial={{ opacity: 0, scale: 0.8, y: -4 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.8, y: -4 }}
                    transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
                    aria-live="polite"
                    aria-label={`${activeCount} 个回复正在后台生成`}
                >
                    <span className={styles.dot} />
                    <span className={styles.text}>
                        正在后台生成 {activeCount} 个回复
                    </span>
                </motion.button>
            )}
        </AnimatePresence>
    );
};

export default BackgroundRunsPill;
```

- [ ] **Step 2: Create the pill CSS**

```css
.pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    background: rgba(232, 122, 93, 0.12);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    border: 1px solid rgba(232, 122, 93, 0.2);
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.pill:hover {
    background: rgba(232, 122, 93, 0.2);
    border-color: rgba(232, 122, 93, 0.35);
}

.dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #E87A5D;
    animation: pillPulse 1.5s ease-in-out infinite;
    flex-shrink: 0;
}

@keyframes pillPulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(0.8); }
}

.text {
    font-size: 12px;
    font-weight: 500;
    color: #7A5C47;
    line-height: 1;
}

@media (prefers-reduced-motion: reduce) {
    .dot {
        animation: none;
    }
}

@media (max-width: 768px) {
    .text {
        font-size: 11px;
    }
    .pill {
        padding: 5px 10px;
        gap: 6px;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/components/Chat/BackgroundRunsPill.jsx src/components/Chat/BackgroundRunsPill.module.css
git commit -m "feat: add BackgroundRunsPill component for active run count"
```

---

### Task 5: RecoveryBanner component

**Files:**
- Create: `src/components/Chat/RecoveryBanner.jsx`
- Create: `src/components/Chat/RecoveryBanner.module.css`

A banner shown at the top of the message area when the user returns to a chat that had a background run. Auto-dismisses after 3 seconds.

- [ ] **Step 1: Create the banner component**

```jsx
import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, AlertCircle, Loader } from 'lucide-react';
import styles from './RecoveryBanner.module.css';

/**
 * @param {'generating'|'completed'|'failed'} status
 * @param {Function} onDismiss - Called when banner auto-hides or user dismisses
 */
const RecoveryBanner = ({ status, onDismiss }) => {
    const [visible, setVisible] = useState(true);

    useEffect(() => {
        if (status === 'completed' || status === 'failed') {
            const timer = setTimeout(() => {
                setVisible(false);
                onDismiss?.();
            }, 3000);
            return () => clearTimeout(timer);
        }
    }, [status, onDismiss]);

    const config = {
        generating: {
            icon: <Loader size={14} className={styles.spinIcon} />,
            text: '这条回复正在后台继续生成中',
            className: styles.generating
        },
        completed: {
            icon: <Check size={14} />,
            text: '该回复已在你离开期间生成完成',
            className: styles.completed
        },
        failed: {
            icon: <AlertCircle size={14} />,
            text: '后台生成失败，请尝试重新发送',
            className: styles.failed
        }
    };

    const c = config[status];
    if (!c) return null;

    return (
        <AnimatePresence>
            {visible && (
                <motion.div
                    className={`${styles.banner} ${c.className}`}
                    initial={{ opacity: 0, y: -8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    transition={{ duration: 0.3 }}
                    role="status"
                    aria-live="polite"
                >
                    {c.icon}
                    <span className={styles.text}>{c.text}</span>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default RecoveryBanner;
```

- [ ] **Step 2: Create the banner CSS**

```css
.banner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 500;
    margin: 8px auto;
    max-width: 400px;
    width: fit-content;
    pointer-events: none;
}

.generating {
    background: rgba(232, 122, 93, 0.1);
    color: #B0624A;
    border: 1px solid rgba(232, 122, 93, 0.2);
}

.completed {
    background: rgba(74, 160, 98, 0.1);
    color: #3A7D4F;
    border: 1px solid rgba(74, 160, 98, 0.2);
}

.failed {
    background: rgba(220, 80, 60, 0.1);
    color: #B84A3A;
    border: 1px solid rgba(220, 80, 60, 0.2);
}

.text {
    line-height: 1.2;
}

.spinIcon {
    animation: bannerSpin 1.2s linear infinite;
}

@keyframes bannerSpin {
    to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
    .spinIcon {
        animation: none;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/components/Chat/RecoveryBanner.jsx src/components/Chat/RecoveryBanner.module.css
git commit -m "feat: add RecoveryBanner component for background run recovery"
```

---

### Task 6: Integrate into ChatArea

**Files:**
- Modify: `src/components/Chat/ChatArea.jsx`

Add render slots for the pill (in header) and the recovery banner (above messages).

- [ ] **Step 1: Update ChatArea to accept and render new props**

```jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion as Motion, AnimatePresence } from 'framer-motion';
import { Heart, Home } from 'lucide-react';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import BackgroundRunsPill from './BackgroundRunsPill';
import RecoveryBanner from './RecoveryBanner';
import styles from './ChatArea.module.css';

const ChatArea = ({
    messages = [],
    onSendMessage,
    inputValue,
    setInputValue,
    isLoading,
    streamingStatus,
    activeRunCount = 0,
    onNavigateToRun,
    recoveryStatus,
    onRecoveryDismiss,
}) => {
    const navigate = useNavigate();
    const isWelcomeState = messages.length === 0;

    const chatConfig = {
        title: '恋爱教练',
        welcomeTitle: '我是你的恋爱助手，今天有什么可以帮忙的？',
    };

    const handleHomeClick = () => {
        navigate('/');
    };

    return (
        <div className={styles.container}>
            {/* Header */}
            <header className={styles.header}>
                <div className={styles.headerLeft}>
                    <div className={styles.titleBadge}>
                        <Heart size={14} className={styles.titleIcon} />
                        <span className={styles.headerTitle}>{chatConfig.title}</span>
                    </div>
                    <BackgroundRunsPill
                        activeCount={activeRunCount}
                        onNavigate={onNavigateToRun}
                    />
                </div>
                <button
                    onClick={handleHomeClick}
                    className={styles.homeBtn}
                    title="回到首页"
                >
                    <Home size={16} />
                    <span>首页</span>
                </button>
            </header>

            {/* Content area */}
            <div className={styles.contentArea}>
                <AnimatePresence mode="wait">
                    {isWelcomeState ? (
                        <Motion.div
                            key="welcome"
                            className={styles.welcomeContent}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -20, scale: 0.95 }}
                            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
                        >
                            <Motion.div
                                className={styles.welcomeIcon}
                                initial={{ scale: 0.5, opacity: 0 }}
                                animate={{ scale: 1, opacity: 1 }}
                                transition={{ duration: 0.5, ease: [0.34, 1.56, 0.64, 1] }}
                            >
                                <Heart size={28} />
                            </Motion.div>
                            <h2 className={styles.welcomeTitle}>
                                {chatConfig.welcomeTitle}
                            </h2>
                        </Motion.div>
                    ) : (
                        <Motion.div
                            key="messages"
                            className={styles.messagesArea}
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
                        >
                            {recoveryStatus && (
                                <RecoveryBanner
                                    status={recoveryStatus}
                                    onDismiss={onRecoveryDismiss}
                                />
                            )}
                            <ChatMessages
                                messages={messages}
                                streamingStatus={streamingStatus}
                            />
                        </Motion.div>
                    )}
                </AnimatePresence>
            </div>

            {/* Input dock */}
            <div className={styles.inputDock}>
                <ChatInput
                    inputValue={inputValue}
                    setInputValue={setInputValue}
                    onSend={onSendMessage}
                    isLoading={isLoading}
                />
            </div>
        </div>
    );
};

export default ChatArea;
```

- [ ] **Step 2: Commit**

```bash
git add src/components/Chat/ChatArea.jsx
git commit -m "feat: integrate pill and recovery banner into ChatArea"
```

---

### Task 7: Wire everything in ChatPage

**Files:**
- Modify: `src/pages/Chat/ChatPage.jsx`

This is the core integration task. Key changes:
1. Import and use `useBackgroundRuns`
2. On chat switch: if currently loading, detach SSE to background instead of cleaning up
3. On chat switch: if target chat has a completed/failed run, show recovery banner and reload messages
4. Pass `backgroundRuns` to sidebar and pill/banner props to ChatArea
5. Wire pill click to navigate to first active run

- [ ] **Step 1: Update ChatPage with full background run integration**

Replace the full `ChatPage.jsx` with:

```jsx
import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import ChatSidebar from '../../components/Sidebar/ChatSidebar';
import ChatArea from '../../components/Chat/ChatArea';
import ManusPanel from '../../components/ManusPanel/ManusPanel';
import { getChatMessages, getChatImages } from '../../services/chatApi';
import { useSSEConnection } from '../../hooks/useSSEConnection';
import { useChatMessages } from '../../hooks/useChatMessages';
import { useChatStorage } from '../../hooks/useChatStorage';
import { usePanelData } from '../../hooks/usePanelData';
import { useChatSessions } from '../../hooks/useChatSessions';
import { useBackgroundRuns } from '../../hooks/useBackgroundRuns';
import styles from './ChatPage.module.css';

const ChatPage = () => {
    const { type } = useParams();
    const chatType = type || 'loveapp';
    const showManusPanel = chatType === 'coach';
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [recoveryStatus, setRecoveryStatus] = useState(null);

    // Background runs
    const {
        runs: backgroundRuns,
        activeRunCount,
        registerRun,
        completeRun,
        failRun,
        clearRun,
        getRunStatus
    } = useBackgroundRuns();

    // SSE connection
    const { connectSSE, cleanupSSE, eventSourceRef } = useSSEConnection(chatType);

    // Message state
    const {
        messages,
        inputValue,
        isLoading,
        streamingStatus,
        setInputValue,
        setStreamingStatus,
        setIsLoading,
        addStatusStep,
        addStreamingContent,
        finalizeStreaming,
        addUserMessage,
        addErrorMessage,
        setMessagesDirect
    } = useChatMessages();

    // Storage
    const storageHooks = useChatStorage(chatType);

    // Panel data
    const panelDataHook = usePanelData(null, showManusPanel, storageHooks);
    const {
        setChatId: setPanelChatId,
        setPanelData,
        setIsPanelOpen
    } = panelDataHook;

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

    // Sync currentChatId to panelData hook
    useEffect(() => {
        setPanelChatId(currentChatId);
    }, [currentChatId, setPanelChatId]);

    // Load messages when currentChatId changes
    useEffect(() => {
        if (!currentChatId) return;

        let cancelled = false;
        const activeChatId = currentChatId;

        const loadMessages = async () => {
            try {
                const msgs = await getChatMessages(activeChatId, chatType);
                if (!cancelled) {
                    setMessagesDirect(msgs.length > 0 ? msgs : []);
                }
            } catch (error) {
                console.error('Failed to load messages:', error);
                if (!cancelled) {
                    setMessagesDirect([]);
                }
            }
        };

        const loadImages = async () => {
            if (!showManusPanel) return;
            try {
                const images = await getChatImages(activeChatId, chatType);
                if (!cancelled) {
                    setPanelData(prev => ({
                        ...prev,
                        files: Array.isArray(images) ? images : []
                    }));
                    if (images?.length > 0) {
                        setIsPanelOpen(true);
                    }
                }
            } catch (error) {
                console.error('Failed to load images:', error);
            }
        };

        // Check if this chat has a background run
        const runStatus = getRunStatus(activeChatId);
        if (runStatus) {
            setRecoveryStatus(runStatus.status);
            if (runStatus.status === 'completed' || runStatus.status === 'failed') {
                clearRun(activeChatId);
            }
        } else {
            setRecoveryStatus(null);
        }

        loadMessages();
        loadImages();
        setStreamingStatus(null);
        return () => {
            cancelled = true;
        };
    }, [
        currentChatId,
        chatType,
        showManusPanel,
        setMessagesDirect,
        setStreamingStatus,
        setPanelData,
        setIsPanelOpen,
        getRunStatus,
        clearRun
    ]);

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
    const handleSendMessage = useCallback(async (msgText = inputValue, imageUrl = null) => {
        const textToUse = msgText || inputValue;
        if ((!textToUse.trim() && !imageUrl) || isLoading) return;

        let activeChatId = currentChatId;
        if (!activeChatId) {
            const newChat = autoCreateChat(textToUse.trim());
            activeChatId = newChat.id;
        }

        const userMessage = textToUse.trim();

        cleanupSSE({ resetResponse: true });

        addUserMessage(userMessage, imageUrl);
        setInputValue('');
        setIsLoading(true);
        const initialType = imageUrl ? 'intake_status' : 'thinking';
        const initialContent = imageUrl
            ? (showManusPanel
                ? '正在读取聊天截图，并判断这件事要不要进入任务执行...'
                : '正在识别聊天截图，准备给你回复建议...')
            : (showManusPanel
                ? '正在整理问题，并判断是否需要继续执行任务...'
                : '正在分析对话语气，准备回复建议...');
        addStatusStep(initialType, initialContent);
        setStreamingStatus({ type: initialType, content: initialContent });

        if (showManusPanel) {
            panelDataHook.resetPanel();
        }

        const currentChat = chatList.find(c => c.id === activeChatId);
        if (currentChat?.title === '新的对话') {
            const title = userMessage.length > 20
                ? userMessage.substring(0, 20) + '...'
                : userMessage;
            updateChatTitle(activeChatId, title);
        }

        // SSE message handlers
        const handleMessage = (parsed) => {
            if (showManusPanel) {
                panelDataHook.handlePanelMessage(parsed);
            }

            switch (parsed.type) {
                case 'thinking':
                case 'status':
                case 'intake_status':
                case 'ocr_result':
                case 'rewrite_result':
                    addStatusStep(parsed.type, parsed.content);
                    setStreamingStatus({ type: parsed.type, content: parsed.content });
                    if (showManusPanel && panelDataHook.isPanelOpen && parsed.content) {
                        panelDataHook.addTerminalOutput(`[${parsed.type}] ${parsed.content}`);
                    }
                    break;

                case 'rag_status':
                    addStatusStep(parsed.type, parsed.content);
                    setStreamingStatus({ type: parsed.type, content: parsed.content });
                    break;

                case 'tool_call':
                    addStatusStep(parsed.type, parsed.content);
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
            const errorMessage = error?.name === 'AuthExpiredError'
                ? error.message
                : '抱歉，连接出现问题，请稍后重试。';

            addErrorMessage(errorMessage);
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

        await connectSSE(userMessage, activeChatId, imageUrl, {
            onData: handleMessage,
            onError: handleError,
            onComplete: handleComplete
        });
    }, [
        inputValue, isLoading, currentChatId, chatList,
        cleanupSSE, connectSSE, showManusPanel,
        addUserMessage, setInputValue, setIsLoading, setStreamingStatus,
        addStatusStep, addStreamingContent, finalizeStreaming, addErrorMessage,
        updateChatTitle, refreshMessages, panelDataHook, autoCreateChat
    ]);

    // Handle chat selection — detach SSE to background if generating
    const handleSelectChat = useCallback((chatId) => {
        if (chatId === currentChatId) return;

        // If currently generating, detach SSE to background
        if (isLoading && currentChatId && eventSourceRef.current) {
            const bgChatId = currentChatId;
            const es = eventSourceRef.current;
            eventSourceRef.current = null; // Prevent cleanupSSE from closing it

            registerRun(bgChatId, es);

            // Override the EventSource handlers for background operation
            es.onmessage = (event) => {
                const data = event.data;
                if (data === '[DONE]' || data === '') {
                    completeRun(bgChatId);
                    es.close();
                    return;
                }
                try {
                    const parsed = JSON.parse(data);
                    if (parsed?.type === 'error') {
                        failRun(bgChatId, parsed.content || 'Unknown error');
                        es.close();
                    } else if (parsed?.type === 'done') {
                        completeRun(bgChatId);
                        es.close();
                    }
                } catch {
                    // content chunk — ignore in background
                }
            };
            es.onerror = () => {
                failRun(bgChatId, 'Connection lost');
                es.close();
            };

            // Reset current chat's loading state
            finalizeStreaming();
            setIsLoading(false);
            setStreamingStatus(null);
        }

        setCurrentChatId(chatId);
    }, [
        currentChatId, isLoading, eventSourceRef,
        registerRun, completeRun, failRun,
        finalizeStreaming, setIsLoading, setStreamingStatus,
        setCurrentChatId
    ]);

    // Navigate to first active background run
    const handleNavigateToRun = useCallback(() => {
        const activeChat = Object.entries(backgroundRuns).find(
            ([, run]) => run.status === 'generating'
        );
        if (activeChat) {
            handleSelectChat(activeChat[0]);
        }
    }, [backgroundRuns, handleSelectChat]);

    // Recovery banner dismiss
    const handleRecoveryDismiss = useCallback(() => {
        setRecoveryStatus(null);
    }, []);

    // New chat handler
    const handleNewChat = useCallback(() => {
        createNewChat();
        setMessagesDirect([]);
        cleanupSSE();
        panelDataHook.resetPanel();
    }, [createNewChat, setMessagesDirect, cleanupSSE, panelDataHook]);

    // Delete chat handler
    const handleDeleteChat = useCallback(async (chatId) => {
        clearRun(chatId); // Clear any background run for deleted chat
        await deleteChat(chatId, storageHooks.removePanelDataFromStorage);
    }, [deleteChat, storageHooks.removePanelDataFromStorage, clearRun]);

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
                    onSelectChat={handleSelectChat}
                    onNewChat={handleNewChat}
                    onDeleteChat={handleDeleteChat}
                    backgroundRuns={backgroundRuns}
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
                        activeRunCount={activeRunCount}
                        onNavigateToRun={handleNavigateToRun}
                        recoveryStatus={recoveryStatus}
                        onRecoveryDismiss={handleRecoveryDismiss}
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
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/Chat/ChatPage.jsx
git commit -m "feat: integrate background runs into ChatPage orchestration"
```

---

### Task 8: Manual verification

- [ ] **Step 1: Start frontend dev server**

```bash
cd springai-front-react && npm run dev
```

- [ ] **Step 2: Verify sidebar status indicators**

1. Open chat, send a message
2. While AI is generating, switch to a different chat
3. Confirm the original chat shows a spinner in the sidebar
4. Wait for generation to complete
5. Confirm spinner changes to a solid dot

- [ ] **Step 3: Verify global pill**

1. While a background run is active, confirm the pill appears in the header
2. Confirm pill text reads "正在后台生成 1 个回复"
3. Click the pill — confirm it navigates to the generating chat
4. When the run completes, confirm the pill disappears

- [ ] **Step 4: Verify recovery banner**

1. Switch to a chat that completed in background
2. Confirm "该回复已在你离开期间生成完成" banner appears
3. Confirm banner auto-dismisses after ~3 seconds
4. Confirm the completed response is visible in the chat

- [ ] **Step 5: Verify no-interrupt behavior**

1. Confirm switching chats does NOT show "stop generating?" dialog
2. Confirm user can freely navigate between chats
3. Confirm typing in another chat while background run is active works fine
