import React, { useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import styles from './ChatArea.module.css';

const STEP_LABELS = {
    thinking: '思考中',
    intake_status: '读取截图',
    ocr_result: '识别完成',
    rewrite_result: '整理问题',
    rag_status: '检索知识库',
    status: '生成回复',
    tool_call: '调用工具',
};

const StatusSteps = ({ steps, isStreaming, hasContent }) => {
    const [userExpanded, setUserExpanded] = useState(false);

    if (!steps || steps.length === 0) return null;

    const isActive = isStreaming && !hasContent;

    // Active state: show full steps with animation
    if (isActive) {
        return (
            <div className={styles.statusStepsContainer}>
                {steps.map((step, idx) => {
                    const isLast = idx === steps.length - 1;
                    return (
                        <div key={idx} className={`${styles.statusStep} ${isLast ? styles.statusStepActive : styles.statusStepDone}`}>
                            {isLast ? (
                                <span className={styles.thinkingDot} />
                            ) : (
                                <Check size={12} className={styles.statusCheckIcon} />
                            )}
                            <span className={styles.statusStepText}>
                                {step.content || STEP_LABELS[step.type] || '处理中'}
                            </span>
                        </div>
                    );
                })}
                <div className={styles.shimmerBar} />
            </div>
        );
    }

    // Collapsed summary after content starts (only expand on user click)
    const summaryText = steps
        .map(s => STEP_LABELS[s.type] || s.type)
        .join(' · ');

    return (
        <div className={styles.statusStepsContainer}>
            <button
                className={styles.statusSummary}
                onClick={() => setUserExpanded(prev => !prev)}
                type="button"
            >
                <Check size={12} className={styles.statusCheckIcon} />
                <span>{summaryText}</span>
                <ChevronDown
                    size={12}
                    className={`${styles.statusChevron} ${userExpanded ? styles.statusChevronOpen : ''}`}
                />
            </button>
            {userExpanded && (
                <div className={styles.statusStepsExpanded}>
                    {steps.map((step, idx) => (
                        <div key={idx} className={`${styles.statusStep} ${styles.statusStepDone}`}>
                            <Check size={12} className={styles.statusCheckIcon} />
                            <span className={styles.statusStepText}>
                                {step.content || STEP_LABELS[step.type] || '处理中'}
                            </span>
                        </div>
                    ))}
                </div>
            )}
            <div className={styles.statusDivider} />
        </div>
    );
};

export default StatusSteps;
