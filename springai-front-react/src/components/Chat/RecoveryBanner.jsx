import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, AlertCircle, Loader } from 'lucide-react';
import styles from './RecoveryBanner.module.css';

/**
 * @param {'generating'|'completed'|'failed'} status
 * @param {Function} onDismiss - Called when banner auto-hides or user dismisses
 */
const RecoveryBanner = ({ status, onDismiss }) => {
    const [visible, setVisible] = useState(true);

    useEffect(() => {
        if (status === 'completed' || status === 'failed') {
            const timer = setTimeout(() => {
                setVisible(false);
                onDismiss?.();
            }, 3000);
            return () => clearTimeout(timer);
        }
    }, [status, onDismiss]);

    const config = {
        generating: {
            icon: <Loader size={14} className={styles.spinIcon} />,
            text: '这条回复正在后台继续生成中',
            className: styles.generating
        },
        completed: {
            icon: <Check size={14} />,
            text: '该回复已在你离开期间生成完成',
            className: styles.completed
        },
        failed: {
            icon: <AlertCircle size={14} />,
            text: '后台生成失败，请尝试重新发送',
            className: styles.failed
        }
    };

    const c = config[status];
    if (!c) return null;

    return (
        <AnimatePresence>
            {visible && (
                <motion.div
                    className={`${styles.banner} ${c.className}`}
                    initial={{ opacity: 0, y: -8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    transition={{ duration: 0.3 }}
                    role="status"
                    aria-live="polite"
                >
                    {c.icon}
                    <span className={styles.text}>{c.text}</span>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default RecoveryBanner;
