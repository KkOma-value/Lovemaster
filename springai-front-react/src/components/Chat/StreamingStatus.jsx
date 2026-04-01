import React from 'react';
import { AlertCircle } from 'lucide-react';
import styles from './ChatArea.module.css';

/**
 * StreamingStatus — Three-phase thinking indicator
 * Phase 1 (thinking): Pulsing dot + status text
 * Phase 2 (processing): Status text + shimmer bar
 * Phase 3 (error): Error message + retry button
 */
const StreamingStatus = ({ type, content, isVisible = true, onRetry }) => {
    if (!isVisible) return null;

    // Phase 3: Error
    if (type === 'error') {
        return (
            <div className={styles.errorInline}>
                <AlertCircle size={14} />
                <span>{content || '连接出现问题，请稍后重试'}</span>
                {onRetry && (
                    <button className={styles.retryBtn} onClick={onRetry}>
                        重新生成
                    </button>
                )}
            </div>
        );
    }

    // Phase 1 & 2: Show status text with indicator
    const statusText = getStatusText(type, content);

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className={styles.thinkingDot} />
                <span className={styles.thinkingText}>{statusText}</span>
            </div>
            <div className={styles.shimmerBar} />
        </div>
    );
};

function getStatusText(type, content) {
    // Don't show rewrite content to user
    if (content && type !== 'rewrite_result') return content;

    switch (type) {
        case 'thinking':
            return '正在思考...';
        case 'intake_status':
            return '正在读取聊天截图...';
        case 'ocr_result':
            return '截图识别完成';
        case 'rewrite_result':
            return '问题已整理';
        case 'rag_status':
            return '正在查阅恋爱知识库...';
        case 'status':
            return '正在生成回复建议...';
        case 'tool_call':
            return '正在调用工具...';
        default:
            return '正在处理...';
    }
}

export default StreamingStatus;
