import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion as Motion, AnimatePresence } from 'framer-motion';
import { Heart } from 'lucide-react';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import styles from './ChatArea.module.css';

const ChatArea = ({
    messages = [],
    onSendMessage,
    inputValue,
    setInputValue,
    isLoading,
    streamingStatus,
}) => {
    const navigate = useNavigate();
    const isWelcomeState = messages.length === 0;

    const chatConfig = {
        title: '恋爱陪伴',
        welcomeTitle: '我是你的恋爱助手，今天有什么可以帮忙的？',
    };

    const handleHomeClick = () => {
        navigate('/');
    };

    return (
        <div className={styles.container}>
            {/* Minimal transparent header */}
            <header className={styles.header}>
                <div className={styles.headerLeft}>
                    <span className={styles.headerTitle}>{chatConfig.title}</span>
                </div>
            </header>

            {/* Chat messages area - only visible when there are messages */}
            <AnimatePresence>
                {!isWelcomeState && (
                    <Motion.div
                        key="messages"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
                        className={styles.messagesArea}
                    >
                        <ChatMessages
                            messages={messages}
                            streamingStatus={streamingStatus}
                        />
                    </Motion.div>
                )}
            </AnimatePresence>

            {/* Bottom area — welcome content + input, animates from center to bottom */}
            <Motion.div
                className={styles.bottomArea}
                layout
                style={{
                    flex: isWelcomeState ? 1 : '0 0 auto',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: isWelcomeState ? 'center' : 'flex-end',
                    alignItems: 'center',
                    padding: isWelcomeState ? '0 20px' : '0',
                }}
                transition={{ layout: { duration: 0.55, ease: [0.4, 0, 0.2, 1] } }}
            >
                <AnimatePresence>
                    {isWelcomeState && (
                        <Motion.div
                            key="welcome"
                            className={styles.welcomeContent}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -30, scale: 0.95 }}
                            transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
                        >
                            <Motion.div
                                className={styles.welcomeIcon}
                                initial={{ scale: 0.5, opacity: 0 }}
                                animate={{ scale: 1, opacity: 1 }}
                                transition={{ duration: 0.6, ease: [0.34, 1.56, 0.64, 1] }}
                            >
                                <Heart size={28} />
                            </Motion.div>
                            <h2 className={styles.welcomeTitle}>
                                {chatConfig.welcomeTitle}
                            </h2>
                        </Motion.div>
                    )}
                </AnimatePresence>

                <div className={styles.inputDock}>
                    <ChatInput
                        inputValue={inputValue}
                        setInputValue={setInputValue}
                        onSend={onSendMessage}
                        isLoading={isLoading}
                    />
                </div>
            </Motion.div>
        </div>
    );
};

export default ChatArea;
