import React, { useEffect, useRef } from 'react';
import { Send, Home } from 'lucide-react';
import styles from './ChatArea.module.css';

const ChatInput = ({ inputValue, setInputValue, onSend, isLoading, hasMessages, onHomeClick }) => {
    const textareaRef = useRef(null);

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            const scrollHeight = textareaRef.current.scrollHeight;
            const newHeight = Math.min(scrollHeight, 200);
            textareaRef.current.style.height = `${newHeight}px`;
        }
    }, [inputValue]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            onSend();
        }
    };

    return (
        <div className={styles.inputArea}>
            <div className={styles.inputWrapper}>
                <textarea
                    ref={textareaRef}
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="输入消息..."
                    className={styles.textarea}
                    rows={1}
                    disabled={isLoading}
                />
                <button
                    onClick={onSend}
                    disabled={!inputValue.trim() || isLoading}
                    className={styles.sendBtn}
                >
                    <Send size={18} />
                </button>
            </div>

            {hasMessages && (
                <button onClick={onHomeClick} className={styles.homeBtn}>
                    <Home size={16} />
                    <span>新对话</span>
                </button>
            )}
        </div>
    );
};

export default ChatInput;
