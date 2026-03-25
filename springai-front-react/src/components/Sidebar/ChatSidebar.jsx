import React from 'react';
import { Plus, ChevronLeft, ChevronRight, Heart } from 'lucide-react';
import { motion } from 'framer-motion';
import ChatSidebarItem from './ChatSidebarItem';
import styles from './ChatSidebar.module.css';

const listVariants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: {
            staggerChildren: 0.06
        }
    }
};

const itemVariants = {
    hidden: { opacity: 0, x: -10 },
    visible: { opacity: 1, x: 0 }
};

const ChatSidebar = ({
    isOpen,
    onToggle,
    onNewChat,
    currentChatId,
    chatList = [],
    onSelectChat,
    onDeleteChat
}) => {
    // Collapsed state - floating toggle button
    if (!isOpen) {
        return (
            <div className={styles.collapsedSidebar}>
                <button
                    className={styles.toggleBtn}
                    onClick={onToggle}
                    title="展开侧边栏"
                >
                    <ChevronRight size={20} />
                </button>
            </div>
        );
    }

    return (
        <aside className={styles.sidebar}>
            {/* Header / Top Actions */}
            <div className={styles.header}>
                <button
                    onClick={onNewChat}
                    className={styles.newChatBtn}
                >
                    <Plus size={18} />
                    <span className={styles.newChatText}>新对话</span>
                </button>
                <button
                    className={styles.toggleBtn}
                    onClick={onToggle}
                    title="收起侧边栏"
                >
                    <ChevronLeft size={20} color="#7A5C47" />
                </button>
            </div>

            {/* Chat List */}
            <motion.div 
                className={styles.list}
                variants={listVariants}
                initial="hidden"
                animate="visible"
            >
                {chatList.map(chat => (
                    <motion.div key={chat.id} variants={itemVariants}>
                        <ChatSidebarItem
                            chat={chat}
                            isActive={currentChatId === chat.id}
                            onSelect={onSelectChat}
                            onDelete={onDeleteChat}
                        />
                    </motion.div>
                ))}
            </motion.div>

            {/* Footer */}
            <div className={styles.footer}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}>
                    <span>💕</span>
                    <span style={{
                        fontSize: '14px',
                        fontWeight: 600,
                        background: 'linear-gradient(135deg, #F43F5E 0%, #EC4899 100%)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text'
                    }}>
                        Love Master
                    </span>
                </div>
            </div>
        </aside>
    );
};

export default ChatSidebar;
