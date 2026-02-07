import React, { useRef, useEffect } from 'react';
import { User, Sparkles } from 'lucide-react';
import MarkdownRenderer from './MarkdownRenderer';
import StreamingStatus from './StreamingStatus';
import styles from './ChatArea.module.css';

const ChatMessages = ({ messages, streamingStatus }) => {
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, streamingStatus]);

    if (messages.length === 0 && !streamingStatus) {
        return null;
    }

    return (
        <div className={styles.messages}>
            {messages.map((message, index) => (
                <div
                    key={index}
                    className={`${styles.messageRow} ${message.role === 'user' ? styles.userRow : ''}`}
                >
                    {message.role === 'assistant' && (
                        <div className={styles.aiIcon}>
                            <Sparkles size={16} />
                        </div>
                    )}
                    <div className={`${styles.bubble} ${message.role === 'user' ? styles.userBubble : styles.aiBubble}`}>
                        {message.role === 'assistant' ? (
                            <>
                                <MarkdownRenderer content={message.content} />
                                {message.isStreaming && (
                                    <span className={styles.cursor}>|</span>
                                )}
                            </>
                        ) : (
                            message.content
                        )}
                    </div>
                    {message.role === 'user' && (
                        <div className={styles.aiIcon} style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
                            <User size={16} />
                        </div>
                    )}
                </div>
            ))}

            {streamingStatus && (
                <div className={styles.messageRow}>
                    <div className={styles.aiIcon}>
                        <Sparkles size={16} />
                    </div>
                    <div className={`${styles.bubble} ${styles.aiBubble}`}>
                        <StreamingStatus
                            type={streamingStatus.type}
                            content={streamingStatus.content}
                            isVisible={true}
                        />
                    </div>
                </div>
            )}

            <div ref={messagesEndRef} />
        </div>
    );
};

export default ChatMessages;
