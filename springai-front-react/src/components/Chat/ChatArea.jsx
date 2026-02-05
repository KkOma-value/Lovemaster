import React, { useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
        if (chatType === 'loveapp') return '💕 恋爱陪伴';
        if (chatType === 'coach') return '🎯 恋爱教练';
        return 'Chat';
    };

    const getWelcomeMessage = () => {
        if (chatType === 'coach') {
            return '我是你的恋爱教练，有什么可以帮忙的？';
        }
        return '我是你的恋爱助手，今天有什么可以帮忙的？';
    };

    const styles = {
        container: {
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            fontFamily: "'Inter', -apple-system, sans-serif",
            ...(isWelcomeState ? { justifyContent: 'center', alignItems: 'center' } : {})
        },
        header: {
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            height: '72px',
            background: 'linear-gradient(90deg, #FFF5F7 0%, #FFFFFF 50%, #FFF5F7 100%)',
            borderBottom: '1px solid #FECDD3',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 24px',
            zIndex: 10
        },
        headerTitle: {
            fontWeight: 700,
            fontSize: '18px',
            background: 'linear-gradient(135deg, #F43F5E 0%, #EC4899 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text'
        },
        homeButton: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 18px',
            background: 'linear-gradient(135deg, #FFE4E6 0%, #FECDD3 100%)',
            border: 'none',
            borderRadius: '12px',
            fontWeight: 600,
            fontSize: '14px',
            color: '#BE123C',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            boxShadow: '0 4px 12px rgba(253, 164, 175, 0.2)'
        },
        welcomeContainer: {
            width: '100%',
            maxWidth: '600px',
            padding: '0 24px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '32px',
            marginTop: '40px'
        },
        welcomeIcon: {
            width: '88px',
            height: '88px',
            background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 50%, #F472B6 100%)',
            borderRadius: '28px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 16px 48px rgba(253, 164, 175, 0.4)'
        },
        welcomeTitle: {
            fontSize: 'clamp(1.5rem, 4vw, 2rem)',
            fontWeight: 700,
            textAlign: 'center',
            background: 'linear-gradient(135deg, #1F2937 0%, #374151 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            lineHeight: 1.3
        },
        inputWrapper: {
            width: '100%',
            position: 'relative',
            background: 'linear-gradient(135deg, #FFFFFF 0%, #FFF5F6 100%)',
            borderRadius: '24px',
            boxShadow: '0 8px 32px rgba(253, 164, 175, 0.2)',
            padding: '8px',
            border: '1px solid #FECDD3',
            transition: 'all 0.3s ease'
        },
        textarea: {
            width: '100%',
            padding: '16px 18px',
            paddingRight: '60px',
            fontSize: '15px',
            fontFamily: "'Inter', -apple-system, sans-serif",
            fontWeight: 400,
            resize: 'none',
            border: 'none',
            outline: 'none',
            minHeight: '52px',
            maxHeight: '160px',
            backgroundColor: 'transparent',
            borderRadius: '16px',
            lineHeight: 1.5
        },
        sendButton: {
            position: 'absolute',
            right: '16px',
            bottom: '16px',
            padding: '12px',
            background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 100%)',
            color: '#fff',
            border: 'none',
            borderRadius: '14px',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            boxShadow: '0 6px 20px rgba(253, 164, 175, 0.4)'
        },
        messagesContainer: {
            flex: 1,
            width: '100%',
            overflowY: 'auto',
            paddingTop: '90px',
            paddingBottom: '140px',
            paddingLeft: '24px',
            paddingRight: '24px'
        },
        messageRow: {
            display: 'flex',
            gap: '14px',
            maxWidth: '800px',
            margin: '0 auto 24px'
        },
        avatar: {
            width: '40px',
            height: '40px',
            minWidth: '40px',
            background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 100%)',
            borderRadius: '14px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(253, 164, 175, 0.3)'
        },
        userMessage: {
            maxWidth: '75%',
            padding: '16px 20px',
            fontSize: '15px',
            borderRadius: '22px',
            borderBottomRightRadius: '6px',
            background: 'linear-gradient(135deg, #1F2937 0%, #374151 100%)',
            color: '#fff',
            lineHeight: 1.6,
            boxShadow: '0 4px 16px rgba(0,0,0,0.1)'
        },
        assistantMessage: {
            maxWidth: '75%',
            padding: '16px 20px',
            fontSize: '15px',
            borderRadius: '22px',
            borderBottomLeftRadius: '6px',
            background: 'linear-gradient(135deg, #FFFFFF 0%, #FFF5F6 100%)',
            color: '#1F2937',
            boxShadow: '0 4px 20px rgba(253, 164, 175, 0.15)',
            border: '1px solid #FECDD3',
            lineHeight: 1.6
        },
        bottomBar: {
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            padding: '20px 24px',
            background: 'linear-gradient(180deg, rgba(255,241,242,0) 0%, #FFF1F2 100%)',
            zIndex: 10
        },
        bottomInputWrapper: {
            maxWidth: '800px',
            margin: '0 auto',
            position: 'relative',
            background: 'linear-gradient(135deg, #FFFFFF 0%, #FFF5F6 100%)',
            borderRadius: '22px',
            boxShadow: '0 8px 32px rgba(253, 164, 175, 0.25)',
            padding: '6px',
            border: '1px solid #FECDD3'
        }
    };

    return (
        <div style={styles.container}>
            {/* Header */}
            <header style={styles.header}>
                <div style={styles.headerTitle}>{getChatTitle()}</div>
                <button
                    type="button"
                    style={styles.homeButton}
                    onClick={() => navigate('/')}
                    title="返回主页"
                    onMouseEnter={(e) => {
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 8px 24px rgba(253, 164, 175, 0.35)';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = '0 4px 12px rgba(253, 164, 175, 0.2)';
                    }}
                >
                    <Home size={16} />
                    <span>首页</span>
                </button>
            </header>

            {/* Welcome State */}
            {isWelcomeState ? (
                <div style={styles.welcomeContainer}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '24px' }}>
                        <div style={styles.welcomeIcon}>
                            <Heart size={40} color="#fff" fill="#fff" />
                        </div>
                        <h2 style={styles.welcomeTitle}>
                            {getWelcomeMessage()}
                        </h2>
                    </div>

                    {/* Centered Input */}
                    <div style={styles.inputWrapper}>
                        <textarea
                            style={styles.textarea}
                            placeholder="输入消息..."
                            rows={1}
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
                            onKeyDown={handleKeyDown}
                        />
                        <button
                            style={{
                                ...styles.sendButton,
                                opacity: !inputValue.trim() || isLoading ? 0.5 : 1,
                                cursor: !inputValue.trim() || isLoading ? 'not-allowed' : 'pointer'
                            }}
                            onClick={onSendMessage}
                            disabled={!inputValue.trim() || isLoading}
                            onMouseEnter={(e) => {
                                if (inputValue.trim() && !isLoading) {
                                    e.currentTarget.style.transform = 'scale(1.05)';
                                    e.currentTarget.style.boxShadow = '0 10px 28px rgba(253, 164, 175, 0.5)';
                                }
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.transform = 'scale(1)';
                                e.currentTarget.style.boxShadow = '0 6px 20px rgba(253, 164, 175, 0.4)';
                            }}
                        >
                            <Send size={20} />
                        </button>
                    </div>
                </div>
            ) : (
                <>
                    {/* Chat Messages */}
                    <div style={styles.messagesContainer}>
                        {messages.map((msg, idx) => (
                            <div
                                key={idx}
                                style={{
                                    ...styles.messageRow,
                                    justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start'
                                }}
                            >
                                {msg.role === 'assistant' && (
                                    <div style={styles.avatar}>
                                        <Sparkles size={20} color="#fff" />
                                    </div>
                                )}

                                <div style={msg.role === 'user' ? styles.userMessage : styles.assistantMessage}>
                                    {msg.role === 'assistant' ? (
                                        <>
                                            <MarkdownRenderer content={msg.content} />
                                            {msg.isStreaming && (
                                                <span style={{
                                                    display: 'inline-block',
                                                    width: '10px',
                                                    height: '18px',
                                                    background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 100%)',
                                                    marginLeft: '6px',
                                                    borderRadius: '3px',
                                                    animation: 'pulse 1s infinite'
                                                }} />
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
                            <div style={{ ...styles.messageRow, justifyContent: 'flex-start' }}>
                                <div style={styles.avatar}>
                                    <Sparkles size={20} color="#fff" />
                                </div>
                                <div style={{
                                    background: 'linear-gradient(135deg, #FFFFFF 0%, #FFF5F6 100%)',
                                    borderRadius: '18px',
                                    padding: '14px 18px',
                                    boxShadow: '0 4px 16px rgba(253, 164, 175, 0.15)',
                                    border: '1px solid #FECDD3'
                                }}>
                                    <StreamingStatus
                                        type={streamingStatus.type}
                                        content={streamingStatus.content}
                                        isVisible={true}
                                    />
                                </div>
                            </div>
                        )}

                        {/* Loading */}
                        {isLoading && !streamingStatus && messages[messages.length - 1]?.role !== 'assistant' && (
                            <div style={{ ...styles.messageRow, justifyContent: 'flex-start' }}>
                                <div style={{ ...styles.avatar }}>
                                    <div style={{ animation: 'spin 1s linear infinite' }}>
                                        <Sparkles size={20} color="#fff" />
                                    </div>
                                </div>
                                <div style={styles.assistantMessage}>
                                    <span style={{ color: '#9CA3AF' }}>思考中...</span>
                                </div>
                            </div>
                        )}
                        <div ref={endRef} />
                    </div>

                    {/* Bottom Input */}
                    <div style={styles.bottomBar}>
                        <div style={styles.bottomInputWrapper}>
                            <textarea
                                style={{ ...styles.textarea, minHeight: '46px', maxHeight: '120px' }}
                                placeholder="输入消息..."
                                rows={1}
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                onKeyDown={handleKeyDown}
                            />
                            <button
                                style={{
                                    ...styles.sendButton,
                                    right: '14px',
                                    bottom: '14px',
                                    padding: '10px',
                                    opacity: !inputValue.trim() || isLoading ? 0.5 : 1,
                                    cursor: !inputValue.trim() || isLoading ? 'not-allowed' : 'pointer'
                                }}
                                onClick={onSendMessage}
                                disabled={!inputValue.trim() || isLoading}
                                onMouseEnter={(e) => {
                                    if (inputValue.trim() && !isLoading) {
                                        e.currentTarget.style.boxShadow = '0 10px 28px rgba(253, 164, 175, 0.5)';
                                    }
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.boxShadow = '0 6px 20px rgba(253, 164, 175, 0.4)';
                                }}
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
