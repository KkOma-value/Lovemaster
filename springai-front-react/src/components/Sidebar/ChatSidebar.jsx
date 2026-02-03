import React from 'react';
import styles from './ChatSidebar.module.css';
import { Plus, MessageSquare, Trash2, ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '../ui/Button';

const ChatSidebar = ({
    isOpen,
    onToggle,
    onNewChat,
    currentChatId,
    chatList = [],
    onSelectChat,
    onDeleteChat
}) => {
    const handleDelete = (e, chatId) => {
        e.stopPropagation();
        if (confirm('确定要删除这个对话吗？')) {
            onDeleteChat?.(chatId);
        }
    };

    // Collapsed state - show only toggle button
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
            {/* Header with collapse button */}
            <div className={styles.header}>
                <button
                    className={styles.toggleBtn}
                    onClick={onToggle}
                    title="收起侧边栏"
                >
                    <ChevronLeft size={20} />
                </button>
                <span className={styles.title}>History</span>
            </div>

            <Button
                variant="primary"
                className={styles.newChatBtn}
                onClick={onNewChat}
            >
                <Plus size={18} style={{ marginRight: 8 }} />
                New Chat
            </Button>

            <div className={styles.list}>
                {chatList.map(chat => (
                    <div
                        key={chat.id}
                        className={`${styles.item} ${currentChatId === chat.id ? styles.active : ''}`}
                        onClick={() => onSelectChat(chat.id)}
                    >
                        <MessageSquare size={16} className={styles.itemIcon} />
                        <span className={styles.itemText}>
                            {chat.title || 'New Conversation'}
                        </span>
                        <button
                            className={styles.deleteBtn}
                            onClick={(e) => handleDelete(e, chat.id)}
                            title="删除对话"
                        >
                            <Trash2 size={14} />
                        </button>
                    </div>
                ))}
            </div>

            <div className={styles.footer}>
                <span>💕 恋爱大师</span>
            </div>
        </aside>
    );
};

export default ChatSidebar;
