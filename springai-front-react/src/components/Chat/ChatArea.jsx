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
    chatId,
    activeRunCount = 0,
    onNavigateToRun,
    recoveryStatus = null,
    onRecoveryDismiss
}) => {
    const navigate = useNavigate();
    const isWelcomeState = messages.length === 0;

    const chatConfig = {
        welcomeTitle: '今天，我有什么能帮到你的吗？',
    };

    const handleCopyAction = (text) => {
        if (!text) return;
        setInputValue(text);
    };

    const handleHomeClick = () => {
        navigate('/');
    };

    return (
        <div className={styles.container}>
            {/* Header */}
            <header className={styles.header}>
                <div className={styles.headerLeft}>
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

            {/* Content area — welcome or messages, cross-fade between them */}
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
                            <RecoveryBanner
                                status={recoveryStatus}
                                onDismiss={onRecoveryDismiss}
                            />
                            <ChatMessages
                                messages={messages}
                                streamingStatus={streamingStatus}
                                onCopyAction={handleCopyAction}
                                chatId={chatId}
                            />
                        </Motion.div>
                    )}
                </AnimatePresence>
            </div>

            {/* Input dock — always at bottom */}
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
