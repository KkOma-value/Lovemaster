import React from 'react';
// eslint-disable-next-line no-unused-vars
import { motion, AnimatePresence } from 'framer-motion';
import styles from './BackgroundRunsPill.module.css';

const BackgroundRunsPill = ({ activeCount, onNavigate }) => {
    return (
        <AnimatePresence>
            {activeCount > 0 && (
                <motion.button
                    className={styles.pill}
                    onClick={onNavigate}
                    initial={{ opacity: 0, scale: 0.8, y: -4 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.8, y: -4 }}
                    transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
                    aria-live="polite"
                    aria-label={`${activeCount} 个回复正在后台生成`}
                >
                    <span className={styles.dot} />
                    <span className={styles.text}>
                        正在后台生成 {activeCount} 个回复
                    </span>
                </motion.button>
            )}
        </AnimatePresence>
    );
};

export default BackgroundRunsPill;
