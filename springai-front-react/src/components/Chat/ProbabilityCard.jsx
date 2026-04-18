import React, { useEffect } from 'react';
import {
    motion as Motion,
    useMotionValue,
    useTransform,
    useSpring,
    animate
} from 'framer-motion';
import {
    TrendingUp,
    ShieldAlert,
    Copy,
    ArrowRight,
    BookmarkCheck
} from 'lucide-react';
import styles from './ProbabilityCard.module.css';

const TIER_COLORS = {
    极低: '#B8482E',
    偏低: '#D88B4A',
    一般: '#C47B5A',
    较高: '#8FA776',
    很高: '#6E8A5A'
};

const CONFIDENCE_LABELS = {
    low: '置信度较低',
    medium: '中等置信度',
    high: '高置信度'
};

const RING_RADIUS = 52;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

const clampProbability = (value) => {
    const num = typeof value === 'number' ? value : Number(value);
    if (!Number.isFinite(num)) return 0;
    return Math.max(0, Math.min(100, Math.round(num)));
};

const tierColor = (tier) => TIER_COLORS[tier] || TIER_COLORS.一般;

const AnimatedNumber = ({ value }) => {
    const motionValue = useMotionValue(0);
    const spring = useSpring(motionValue, { damping: 20, stiffness: 80 });
    const display = useTransform(spring, (latest) => Math.round(latest));

    useEffect(() => {
        const controls = animate(motionValue, value, {
            duration: 1.2,
            ease: [0.34, 1.56, 0.64, 1]
        });
        return () => controls.stop();
    }, [motionValue, value]);

    return <Motion.span>{display}</Motion.span>;
};

const ProbabilityCard = ({
    probability,
    tier,
    confidence,
    summary,
    greenFlags = [],
    redFlags = [],
    nextActions = [],
    onCopyAction
}) => {
    const clamped = clampProbability(probability);
    const color = tierColor(tier);
    const confidenceLabel = CONFIDENCE_LABELS[confidence] || CONFIDENCE_LABELS.medium;
    const dashOffset = RING_CIRCUMFERENCE * (1 - clamped / 100);

    const handleCopy = (actionText) => {
        if (typeof onCopyAction === 'function') {
            onCopyAction(actionText);
        }
    };

    return (
        <Motion.section
            className={styles.card}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
            aria-label={`成功概率 ${clamped}%，${tier || ''}，${confidenceLabel}`}
        >
            <div className={styles.header}>
                <div className={styles.ringWrapper}>
                    <svg viewBox="0 0 120 120" role="img" aria-hidden="true">
                        <circle
                            className={styles.ringTrack}
                            cx="60"
                            cy="60"
                            r={RING_RADIUS}
                        />
                        <Motion.circle
                            className={styles.ringProgress}
                            cx="60"
                            cy="60"
                            r={RING_RADIUS}
                            style={{ stroke: color }}
                            strokeDasharray={RING_CIRCUMFERENCE}
                            initial={{ strokeDashoffset: RING_CIRCUMFERENCE }}
                            animate={{ strokeDashoffset: dashOffset }}
                            transition={{ duration: 1.2, ease: [0.34, 1.56, 0.64, 1] }}
                        />
                    </svg>
                    <div className={styles.ringCenter}>
                        <div style={{ color, display: 'flex', alignItems: 'baseline' }}>
                            <span className={styles.ringNumber}>
                                <AnimatedNumber value={clamped} />
                            </span>
                            <span className={styles.ringUnit}>%</span>
                        </div>
                    </div>
                </div>

                <div className={styles.headerText}>
                    <div className={styles.headerLabel}>成功概率（在一起的可能性）</div>
                    <div className={styles.headerTier}>
                        <span style={{ color }}>{tier || '一般'}</span>
                        <span className={styles.headerTierDivider}>·</span>
                        <span className={styles.headerConfidence}>{confidenceLabel}</span>
                    </div>
                    {summary && (
                        <p className={styles.headerSummary}>{summary}</p>
                    )}
                </div>
            </div>

            {greenFlags.length > 0 && (
                <>
                    <div className={styles.sectionTitle}>
                        <span>正面信号<span className={styles.sectionCount}>{greenFlags.length}</span></span>
                    </div>
                    <div className={styles.flagList}>
                        {greenFlags.map((flag, idx) => (
                            <div key={`g-${idx}`} className={`${styles.flagItem} ${styles.flagGreen}`}>
                                <TrendingUp size={18} className={styles.flagIcon} aria-hidden="true" />
                                <div className={styles.flagBody}>
                                    <div className={styles.flagTitleRow}>
                                        <span className={styles.flagTitle}>{flag.title}</span>
                                        {flag.weight === 'high' && (
                                            <span className={`${styles.weightBadge} ${styles.weightHigh}`}>高</span>
                                        )}
                                        {flag.weight === 'medium' && (
                                            <span className={`${styles.weightBadge} ${styles.weightMedium}`}>中</span>
                                        )}
                                    </div>
                                    {flag.evidence && (
                                        <div className={styles.flagEvidence}>证据：{flag.evidence}</div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}

            {redFlags.length > 0 && (
                <>
                    <div className={styles.sectionTitle}>
                        <span>风险信号<span className={styles.sectionCount}>{redFlags.length}</span></span>
                    </div>
                    <div className={styles.flagList}>
                        {redFlags.map((flag, idx) => {
                            const isHigh = flag.weight === 'high';
                            return (
                                <div
                                    key={`r-${idx}`}
                                    className={`${styles.flagItem} ${isHigh ? styles.flagRedHigh : styles.flagRed}`}
                                >
                                    <ShieldAlert size={18} className={styles.flagIcon} aria-hidden="true" />
                                    <div className={styles.flagBody}>
                                        <div className={styles.flagTitleRow}>
                                            <span className={styles.flagTitle}>{flag.title}</span>
                                            {isHigh && (
                                                <span className={`${styles.weightBadge} ${styles.weightHigh}`}>高</span>
                                            )}
                                            {flag.weight === 'medium' && (
                                                <span className={`${styles.weightBadge} ${styles.weightMedium}`}>中</span>
                                            )}
                                        </div>
                                        {flag.evidence && (
                                            <div className={styles.flagEvidence}>证据：{flag.evidence}</div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </>
            )}

            {nextActions.length > 0 && (
                <>
                    <div className={styles.sectionTitle}>
                        <span>下一步怎么做</span>
                    </div>
                    <div className={styles.actionList}>
                        {nextActions.map((action, idx) => (
                            <button
                                key={`a-${idx}`}
                                type="button"
                                className={styles.actionItem}
                                onClick={() => handleCopy(action.action)}
                                aria-label={`复制话术到输入框：${action.action}`}
                            >
                                <span className={styles.actionIndex}>{idx + 1}</span>
                                <span className={styles.actionText}>{action.action}</span>
                                {action.tone && (
                                    <span className={`${styles.actionTone} ${styles[`tone${action.tone}`] || ''}`}>
                                        {action.tone}
                                    </span>
                                )}
                                {typeof onCopyAction === 'function' ? (
                                    <Copy size={14} className={styles.actionCopyIcon} aria-hidden="true" />
                                ) : (
                                    <ArrowRight size={14} className={styles.actionCopyIcon} aria-hidden="true" />
                                )}
                            </button>
                        ))}
                    </div>
                </>
            )}
            
            {/* Auto submission note when clamped >= 82 */}
            {clamped >= 82 && (
                <div style={{
                    marginTop: '12px',
                    padding: '8px 12px',
                    background: '#f0fdf4',
                    color: '#10b981',
                    borderRadius: '8px',
                    fontSize: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontWeight: 500
                }}>
                    <BookmarkCheck size={14} />
                    <span>该条回复已自动进入高质量知识蒸馏队列</span>
                </div>
            )}
        </Motion.section>
    );
};

export default ProbabilityCard;
