import React from 'react';
import styles from './StreamingStatus.module.css';
import { Loader2, Search, Brain, Sparkles, CheckCircle, AlertCircle, Wrench } from 'lucide-react';

/**
 * 流式状态显示组件
 * 根据不同类型显示对应的状态动画
 */
const StreamingStatus = ({ type, content, isVisible = true }) => {
    if (!isVisible) return null;

    const getStatusConfig = () => {
        switch (type) {
            case 'thinking':
                return {
                    icon: <Brain className={styles.pulseIcon} size={16} />,
                    label: content || '正在思考...',
                    className: styles.thinking
                };
            case 'status':
                // 根据内容关键词选择图标
                if (content?.includes('搜索') || content?.includes('查找')) {
                    return {
                        icon: <Search className={styles.pulseIcon} size={16} />,
                        label: content,
                        className: styles.searching
                    };
                }
                if (content?.includes('分析') || content?.includes('处理')) {
                    return {
                        icon: <Brain className={styles.pulseIcon} size={16} />,
                        label: content,
                        className: styles.analyzing
                    };
                }
                if (content?.includes('工具') || content?.includes('调用')) {
                    return {
                        icon: <Wrench className={styles.pulseIcon} size={16} />,
                        label: content,
                        className: styles.tooling
                    };
                }
                return {
                    icon: <Loader2 className={styles.spinIcon} size={16} />,
                    label: content || '处理中...',
                    className: styles.processing
                };
            case 'tool_call':
                return {
                    icon: <Wrench className={styles.pulseIcon} size={16} />,
                    label: content || '正在调用工具...',
                    className: styles.tooling
                };
            case 'done':
                return {
                    icon: <CheckCircle size={16} />,
                    label: '完成',
                    className: styles.done
                };
            case 'error':
                return {
                    icon: <AlertCircle size={16} />,
                    label: content || '发生错误',
                    className: styles.error
                };
            default:
                return {
                    icon: <Sparkles className={styles.pulseIcon} size={16} />,
                    label: content || '处理中...',
                    className: styles.default
                };
        }
    };

    const config = getStatusConfig();

    return (
        <div className={`${styles.container} ${config.className}`}>
            <span className={styles.iconWrapper}>
                {config.icon}
            </span>
            <span className={styles.label}>{config.label}</span>
        </div>
    );
};

export default StreamingStatus;
