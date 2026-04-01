import React, { useRef, useEffect, useState, useCallback } from 'react';
import { User, ChevronDown } from 'lucide-react';
import aiAvatarUrl from '../../assets/illustrations/ai-avatar.svg';
import MarkdownRenderer from './MarkdownRenderer';
import ImageWithLightbox from './ImageWithLightbox';
import StatusSteps from './StatusSteps';
import StreamingStatus from './StreamingStatus';
import styles from './ChatArea.module.css';

const ChatMessages = ({ messages, streamingStatus, onRetry }) => {
    const scrollContainerRef = useRef(null);
    const messagesEndRef = useRef(null);
    const isUserScrollingRef = useRef(false);
    const [showScrollBtn, setShowScrollBtn] = useState(false);

    // Detect user scroll
    const handleScroll = useCallback(() => {
        const el = scrollContainerRef.current;
        if (!el) return;
        const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60;
        isUserScrollingRef.current = !atBottom;
        setShowScrollBtn(!atBottom);
    }, []);

    // Auto-scroll to bottom
    const scrollToBottom = useCallback((smooth = true) => {
        messagesEndRef.current?.scrollIntoView({
            behavior: smooth ? 'smooth' : 'instant',
            block: 'end'
        });
    }, []);

    // Auto-scroll when new content arrives (unless user is scrolling up)
    useEffect(() => {
        if (!isUserScrollingRef.current) {
            scrollToBottom();
        }
    }, [messages, scrollToBottom]);

    // Also scroll when streamingStatus changes (thinking indicator)
    useEffect(() => {
        if (!isUserScrollingRef.current && streamingStatus) {
            scrollToBottom();
        }
    }, [streamingStatus, scrollToBottom]);

    const handleScrollToBottom = () => {
        isUserScrollingRef.current = false;
        setShowScrollBtn(false);
        scrollToBottom();
    };

    if (messages.length === 0 && !streamingStatus) {
        return null;
    }

    return (
        <div
            ref={scrollContainerRef}
            className={styles.messagesArea}
            onScroll={handleScroll}
        >
            <div className={styles.messages}>
                {messages.map((message, index) => (
                    <div
                        key={index}
                        className={`${styles.messageRow} ${message.role === 'user' ? styles.userRow : ''}`}
                    >
                        {message.role === 'assistant' && (
                            <div className={styles.aiIcon}>
                                <img src={aiAvatarUrl} alt="AI助手" width="30" height="30" style={{ borderRadius: '50%' }} />
                            </div>
                        )}
                        <div className={`${styles.bubble} ${message.role === 'user' ? styles.userBubble : styles.aiBubble}`}>
                            {message.role === 'assistant' ? (
                                <>
                                    {/* Status steps — always above content */}
                                    {message.statusSteps?.length > 0 && (
                                        <StatusSteps
                                            steps={message.statusSteps}
                                            isStreaming={message.isStreaming}
                                            hasContent={!!message.content}
                                        />
                                    )}
                                    {/* Content */}
                                    {message.content && (
                                        <MarkdownRenderer content={message.content} isStreaming={message.isStreaming} />
                                    )}
                                    {message.isStreaming && message.content && (
                                        <span className={styles.cursor} />
                                    )}
                                    {/* Fallback: streaming status when no status steps exist yet */}
                                    {message.isStreaming && !message.content && !message.statusSteps?.length && streamingStatus && (
                                        <StreamingStatus
                                            type={streamingStatus.type}
                                            content={streamingStatus.content}
                                            isVisible={true}
                                            onRetry={onRetry}
                                        />
                                    )}
                                    {/* Fallback images from file_created events */}
                                    {message.images?.length > 0 && (
                                        <div style={{
                                            marginTop: '12px',
                                            paddingTop: '12px',
                                            borderTop: '1px solid #E5E7EB',
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                                            gap: '8px',
                                        }}>
                                            {message.images.map((img, i) => (
                                                <ImageWithLightbox
                                                    key={img.url || i}
                                                    src={img.url}
                                                    alt={img.name || '图片'}
                                                />
                                            ))}
                                        </div>
                                    )}
                                    {message.isError && (
                                        <div className={styles.errorInline}>
                                            <span>{message.content}</span>
                                        </div>
                                    )}
                                </>
                            ) : (
                                <>
                                    {message.imageUrl && (
                                        <div style={{ marginBottom: '8px' }}>
                                            <img
                                                src={message.imageUrl}
                                                alt="Uploaded"
                                                style={{ maxWidth: '280px', maxHeight: '280px', borderRadius: '16px', objectFit: 'cover' }}
                                            />
                                        </div>
                                    )}
                                    {message.content}
                                </>
                            )}
                        </div>
                        {message.role === 'user' && (
                            <div className={styles.aiIcon}>
                                <User size={16} />
                            </div>
                        )}
                    </div>
                ))}

                <div ref={messagesEndRef} />
            </div>

            {/* Scroll to bottom button */}
            {showScrollBtn && (
                <button
                    className={styles.scrollBottomBtn}
                    onClick={handleScrollToBottom}
                    aria-label="滚动到底部"
                >
                    <ChevronDown size={18} />
                </button>
            )}
        </div>
    );
};

export default ChatMessages;
