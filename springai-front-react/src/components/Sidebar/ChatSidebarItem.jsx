import React from 'react';
import { MessageSquare, Trash2 } from 'lucide-react';
import styles from './ChatSidebar.module.css';

const ChatSidebarItem = ({ chat, isActive, onSelect, onDelete }) => {
    const handleDelete = (e) => {
        e.stopPropagation();
        if (window.confirm(`确定要删除对话 "${chat.title}" 吗？`)) {
            onDelete(chat.id);
        }
    };

    return (
        <div
            className={`${styles.item} ${isActive ? styles.active : ''}`}
            onClick={() => onSelect(chat.id)}
        >
            <MessageSquare size={16} className={styles.itemIcon} />
            <span className={styles.itemText}>{chat.title}</span>
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
