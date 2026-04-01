import React from 'react';
import { MessageSquare, Trash2, AlertCircle } from 'lucide-react';
import styles from './ChatSidebar.module.css';

const ChatSidebarItem = ({ chat, isActive, onSelect, onDelete, runStatus }) => {
    const handleDelete = (e) => {
        e.stopPropagation();
        if (window.confirm(`确定要删除对话 "${chat.title}" 吗？`)) {
            onDelete(chat.id);
        }
    };

    const renderStatusIndicator = () => {
        if (!runStatus || !runStatus.status) return null;

        switch (runStatus.status) {
            case 'generating':
                return (
                    <span className={styles.runIndicator} title="生成中" aria-label="生成中">
                        <span className={styles.runSpinner} />
                    </span>
                );
            case 'completed':
                return (
                    <span className={styles.runIndicator} title="刚完成" aria-label="刚完成">
                        <span className={styles.runDot} />
                    </span>
                );
            case 'failed':
                return (
                    <span className={styles.runIndicator} title="失败" aria-label="失败">
                        <AlertCircle size={14} className={styles.runFailedIcon} />
                    </span>
                );
            default:
                return null;
        }
    };

    return (
        <div
            className={`${styles.item} ${isActive ? styles.active : ''}`}
            onClick={() => onSelect(chat.id)}
        >
            <MessageSquare size={16} className={styles.itemIcon} />
            <span className={styles.itemText}>{chat.title}</span>
            {renderStatusIndicator()}
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
