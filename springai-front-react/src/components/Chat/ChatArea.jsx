import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, Heart } from 'lucide-react';
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
    chatType
}) => {
    const navigate = useNavigate();

    const isWelcomeState = messages.length === 0;

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

    const handleHomeClick = () => {
        navigate('/');
    };

    return (
        <div className={`${styles.container} ${isWelcomeState ? styles.welcomeMode : styles.chatMode}`}>
            {/* Header */}
            <header className={styles.header}>
                <div className={styles.headerTitle}>{getChatTitle()}</div>
                <div className={styles.headerActions}>
                    <button
                        type="button"
                        className={styles.homeBtn}
                        onClick={handleHomeClick}
                        title="返回主页"
                    >
                        <Home size={16} />
                        <span>首页</span>
                    </button>
                </div>
            </header>

            {/* Welcome State */}
            {isWelcomeState ? (
                <div className={styles.welcomeContainer}>
                    <div className={styles.welcomeContent}>
                        <div className={styles.welcomeIcon}>
                            <Heart size={32} />
                        </div>
                        <h2 className={styles.welcomeTitle}>
                            {getWelcomeMessage()}
                        </h2>
                    </div>

                    <div className={styles.welcomeInputArea}>
                        <ChatInput
                            inputValue={inputValue}
                            setInputValue={setInputValue}
                            onSend={onSendMessage}
                            isLoading={isLoading}
                            hasMessages={false}
                            onHomeClick={handleHomeClick}
                        />
                    </div>
                </div>
            ) : (
                <>
                    {/* Chat Messages */}
                    <ChatMessages
                        messages={messages}
                        streamingStatus={streamingStatus}
                    />

                    {/* Bottom Input */}
                    <div className={styles.inputDock}>
                        <ChatInput
                            inputValue={inputValue}
                            setInputValue={setInputValue}
                            onSend={onSendMessage}
                            isLoading={isLoading}
                        />
                    </div>
                </>
            )}
        </div>
    );
};

export default ChatArea;
