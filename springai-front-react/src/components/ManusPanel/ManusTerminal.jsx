import React, { useRef, useEffect } from 'react';
import styles from './ManusPanel.module.css';

const ManusTerminal = ({ lines }) => {
    const terminalRef = useRef(null);

    useEffect(() => {
        if (terminalRef.current) {
            terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
        }
    }, [lines]);

    const getLineClass = (type) => {
        switch (type) {
            case 'prompt': return styles.terminalPrompt;
            case 'command': return styles.terminalCommand;
            case 'output': return styles.terminalOutput;
            case 'error': return styles.terminalError;
            default: return '';
        }
    };

    if (lines.length === 0) {
        return (
            <div className={styles.terminal}>
                <div className={styles.terminalHeader}>
                    <span className={`${styles.terminalDot} ${styles.dotRed}`}></span>
                    <span className={`${styles.terminalDot} ${styles.dotYellow}`}></span>
                    <span className={`${styles.terminalDot} ${styles.dotGreen}`}></span>
                    <span style={{ marginLeft: '8px' }}>Terminal</span>
                </div>
                <div style={{ padding: '16px', color: '#9CA3AF', fontSize: '0.85rem' }}>
                    等待任务启动...
                </div>
            </div>
        );
    }

    return (
        <div className={styles.terminal} ref={terminalRef}>
            <div className={styles.terminalHeader}>
                <span className={`${styles.terminalDot} ${styles.dotRed}`}></span>
                <span className={`${styles.terminalDot} ${styles.dotYellow}`}></span>
                <span className={`${styles.terminalDot} ${styles.dotGreen}`}></span>
                <span style={{ marginLeft: '8px' }}>Terminal</span>
            </div>
            {lines.map((line, index) => (
                <div key={index} className={`${styles.terminalLine} ${getLineClass(line.type)}`}>
                    {line.content}
                </div>
            ))}
        </div>
    );
};

export default ManusTerminal;
