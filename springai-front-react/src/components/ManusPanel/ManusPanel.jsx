import React, { useState } from 'react';
import { Monitor, X, Terminal, FileText } from 'lucide-react';
import ManusTerminal from './ManusTerminal';
import ManusPreview from './ManusPreview';
import ManusTasks from './ManusTasks';
import styles from './ManusPanel.module.css';

/**
 * ManusPanel - Manus-style computer panel for displaying AI task execution
 *
 * @param {boolean} isOpen - Whether the panel is open
 * @param {function} onToggle - Toggle panel open/close
 * @param {object} panelData - Panel content data
 * @param {array} panelData.tasks - Task progress items [{name, status: 'pending'|'active'|'completed'}]
 * @param {array} panelData.terminalLines - Terminal output lines [{type: 'prompt'|'command'|'output'|'error', content}]
 * @param {array} panelData.files - Generated files [{type: 'pdf'|'image'|'code', name, url, path}]
 */
const ManusPanel = ({
    isOpen,
    onToggle,
    panelData = {}
}) => {
    const [activeTab, setActiveTab] = useState('terminal');

    const {
        tasks = [],
        terminalLines = [],
        files = []
    } = panelData;

    return (
        <>
            {/* Toggle button when panel is closed */}
            <button
                className={`${styles.toggleBtn} ${isOpen ? styles.toggleBtnHidden : ''}`}
                onClick={onToggle}
                title="打开 Manus 电脑"
            >
                <Monitor size={22} />
            </button>

            {/* Main panel */}
            <div className={`${styles.panelContainer} ${!isOpen ? styles.panelClosed : ''}`}>
                {/* Header */}
                <div className={styles.header}>
                    <div className={styles.headerTitle}>
                        <div className={styles.headerIcon}>
                            <Monitor size={16} />
                        </div>
                        <span>Manus 的电脑</span>
                    </div>
                    <div className={styles.headerActions}>
                        <button
                            className={styles.actionBtn}
                            onClick={onToggle}
                            title="关闭面板"
                        >
                            <X size={18} />
                        </button>
                    </div>
                </div>

                {/* Tabs */}
                <div className={styles.tabs}>
                    <button
                        className={`${styles.tab} ${activeTab === 'terminal' ? styles.tabActive : ''}`}
                        onClick={() => setActiveTab('terminal')}
                    >
                        <Terminal size={16} />
                        <span>终端</span>
                    </button>
                    <button
                        className={`${styles.tab} ${activeTab === 'preview' ? styles.tabActive : ''}`}
                        onClick={() => setActiveTab('preview')}
                    >
                        <FileText size={16} />
                        <span>预览</span>
                    </button>
                </div>

                {/* Content */}
                <div className={styles.content}>
                    {activeTab === 'terminal' && <ManusTerminal lines={terminalLines} />}
                    {activeTab === 'preview' && <ManusPreview files={files} />}
                </div>

                {/* Task Progress */}
                <ManusTasks tasks={tasks} />
            </div>
        </>
    );
};

export default ManusPanel;
