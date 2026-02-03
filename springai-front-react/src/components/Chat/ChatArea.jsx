import React, { useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './ChatArea.module.css';
import { Send, Sparkles, Home, Heart } from 'lucide-react';
import MarkdownRenderer from './MarkdownRenderer';
import StreamingStatus from './StreamingStatus';

const ChatArea = ({
    messages = [],
    onSendMessage,
    inputValue,
    setInputValue,
    isLoading,
    streamingStatus,
    chatType
}) => {
    const endRef = useRef(null);
    const navigate = useNavigate();

    // Check if this is the initial welcome state (only greeting message)
    const isWelcomeState = messages.length <= 1 &&
        (messages.length === 0 || messages[0]?.role === 'assistant');

    const scrollToBottom = () => {
        endRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        if (!isWelcomeState) {
            scrollToBottom();
        }
    }, [messages, isLoading, streamingStatus, isWelcomeState]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            onSendMessage();
        }
    };

    const getChatTitle = () => {
        if (chatType === 'loveapp') return '恋爱陪伴';
        if (chatType === 'coach') return '恋爱教练';
        return 'Chat';
    };

    const getWelcomeMessage = () => {
        if (chatType === 'coach') {
            return '我是你的恋爱教练，有什么可以帮忙的？';
        }
        return '我是你的恋爱助手，今天有什么可以帮忙的？';
    };

    return (
        <div className={`${styles.container} ${isWelcomeState ? styles.welcomeMode : ''}`}>
            {/* Header */}
            <header className={styles.header}>
                <div className={styles.headerTitle}>
                    <span>{getChatTitle()}</span>
                </div>
                <div className={styles.headerActions}>
                    <button
                        type="button"
                        className={styles.homeBtn}
                        onClick={() => navigate('/')}
                        title="返回主页"
                    >
                        <Home size={18} />
                        <span>主页</span>
                    </button>
                </div>
            </header>

            {/* Welcome State - Centered Layout */}
            {isWelcomeState ? (
                <div className={styles.welcomeContainer}>
                    <div className={styles.welcomeContent}>
                        <div className={styles.welcomeIcon}>
                            <Heart size={32} />
                        </div>
                        <h2 className={styles.welcomeTitle}>{getWelcomeMessage()}</h2>
                    </div>

                    {/* Centered Input */}
                    <div className={styles.welcomeInputArea}>
                        <div className={styles.inputWrapper}>
                            <textarea
                                className={styles.textarea}
                                placeholder="输入消息..."
                                rows={1}
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                onKeyDown={handleKeyDown}
                                autoComplete="off"
                                autoCorrect="off"
                                autoCapitalize="off"
                                spellCheck="false"
                            />
                            <button
                                className={styles.sendBtn}
                                onClick={onSendMessage}
                                disabled={!inputValue.trim() || isLoading}
                            >
                                <Send size={18} />
                            </button>
                        </div>
                    </div>
                </div>
            ) : (
                <>
                    {/* Chat Messages - Normal Layout */}
                    <div className={styles.messages}>
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`${styles.messageRow} ${msg.role === 'user' ? styles.userRow : ''}`}>
                                {msg.role === 'assistant' && (
                                    <div className={styles.aiIcon}>
                                        <Sparkles size={18} />
                                    </div>
                                )}

                                <div className={`${styles.bubble} ${msg.role === 'user' ? styles.userBubble : styles.aiBubble}`}>
                                    {msg.role === 'assistant' ? (
                                        <>
                                            <MarkdownRenderer content={msg.content} />
                                            {msg.isStreaming && (
                                                <span className={styles.cursor}>▋</span>
                                            )}
                                        </>
                                    ) : (
                                        msg.content
                                    )}
                                </div>
                            </div>
                        ))}

                        {/* Streaming Status */}
                        {streamingStatus && (
                            <div className={styles.messageRow}>
                                <div className={styles.aiIcon}>
                                    <Sparkles size={18} />
                                </div>
                                <div className={styles.statusBubble}>
                                    <StreamingStatus
                                        type={streamingStatus.type}
                                        content={streamingStatus.content}
                                        isVisible={true}
                                    />
                                </div>
                            </div>
                        )}

                        {/* Loading state */}
                        {isLoading && !streamingStatus && messages[messages.length - 1]?.role !== 'assistant' && (
                            <div className={styles.messageRow}>
                                <div className={styles.aiIcon}>
                                    <Sparkles size={18} />
                                </div>
                                <div className={`${styles.bubble} ${styles.aiBubble}`}>
                                    <span className={styles.thinking}>正在思考...</span>
                                </div>
                            </div>
                        )}
                        <div ref={endRef} />
                    </div>

                    {/* Bottom Input */}
                    <div className={styles.inputArea}>
                        <div className={styles.inputWrapper}>
                            <textarea
                                className={styles.textarea}
                                placeholder="输入消息..."
                                rows={1}
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                onKeyDown={handleKeyDown}
                                autoComplete="off"
                                autoCorrect="off"
                                autoCapitalize="off"
                                spellCheck="false"
                            />
                            <button
                                className={styles.sendBtn}
                                onClick={onSendMessage}
                                disabled={!inputValue.trim() || isLoading}
                            >
                                <Send size={18} />
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ChatArea;
