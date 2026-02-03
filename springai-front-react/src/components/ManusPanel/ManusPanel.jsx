import React, { useState, useEffect, useRef } from 'react';
import styles from './ManusPanel.module.css';
import {
    Monitor,
    X,
    Terminal,
    FileText,
    ListChecks,
    Maximize2,
    Minimize2,
    CheckCircle,
    Circle,
    Loader,
    Image as ImageIcon,
    Code,
    ChevronRight
} from 'lucide-react';

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
    const terminalRef = useRef(null);

    const {
        tasks = [],
        terminalLines = [],
        files = []
    } = panelData;

    // Auto-scroll terminal to bottom
    useEffect(() => {
        if (terminalRef.current && activeTab === 'terminal') {
            terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
        }
    }, [terminalLines, activeTab]);

    // Count completed tasks
    const completedCount = tasks.filter(t => t.status === 'completed').length;

    // Render terminal content
    const renderTerminal = () => {
        if (terminalLines.length === 0) {
            return (
                <div className={styles.emptyState}>
                    <div className={styles.emptyIcon}>
                        <Terminal size={28} />
                    </div>
                    <div className={styles.emptyTitle}>等待任务执行</div>
                    <div className={styles.emptyDescription}>
                        当 AI 开始执行任务时，终端输出将显示在这里
                    </div>
                </div>
            );
        }

        return (
            <div className={styles.terminal} ref={terminalRef}>
                {terminalLines.map((line, idx) => (
                    <div key={idx} className={styles.terminalLine}>
                        {line.type === 'prompt' && (
                            <span className={styles.terminalPrompt}>{line.content}</span>
                        )}
                        {line.type === 'command' && (
                            <span className={styles.terminalCommand}>{line.content}</span>
                        )}
                        {line.type === 'output' && (
                            <span className={styles.terminalOutput}>{line.content}</span>
                        )}
                        {line.type === 'error' && (
                            <span className={styles.terminalError}>{line.content}</span>
                        )}
                    </div>
                ))}
            </div>
        );
    };

    // Render preview content
    const renderPreview = () => {
        if (files.length === 0) {
            return (
                <div className={styles.emptyState}>
                    <div className={styles.emptyIcon}>
                        <FileText size={28} />
                    </div>
                    <div className={styles.emptyTitle}>暂无文件</div>
                    <div className={styles.emptyDescription}>
                        生成的 PDF、图片、代码文件将显示在这里
                    </div>
                </div>
            );
        }

        return (
            <div className={styles.previewContainer}>
                {files.map((file, idx) => (
                    <div key={idx} className={styles.previewCard}>
                        <div className={styles.previewHeader}>
                            <div className={`${styles.previewIcon} ${file.type === 'pdf' ? styles.previewIconPdf :
                                    file.type === 'image' ? styles.previewIconImage :
                                        styles.previewIconCode
                                }`}>
                                {file.type === 'pdf' && <FileText size={18} />}
                                {file.type === 'image' && <ImageIcon size={18} />}
                                {file.type === 'code' && <Code size={18} />}
                            </div>
                            <div className={styles.previewInfo}>
                                <div className={styles.previewTitle}>{file.name}</div>
                                <div className={styles.previewMeta}>{file.path}</div>
                            </div>
                        </div>
                        <div className={styles.previewContent}>
                            {file.type === 'image' && file.url && (
                                <img
                                    src={file.url}
                                    alt={file.name}
                                    className={styles.previewImage}
                                />
                            )}
                            {file.type === 'pdf' && file.url && (
                                <iframe
                                    src={file.url}
                                    className={styles.previewPdf}
                                    title={file.name}
                                />
                            )}
                            {file.type === 'code' && (
                                <pre className={styles.codePreview}>
                                    {file.content || '// Code content...'}
                                </pre>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        );
    };

    // Render task progress
    const renderTaskProgress = () => {
        if (tasks.length === 0) {
            return null;
        }

        return (
            <div className={styles.taskProgress}>
                <div className={styles.progressHeader}>
                    <span>任务进度</span>
                    <span className={styles.progressCount}>
                        {completedCount} / {tasks.length}
                    </span>
                </div>
                <div className={styles.taskList}>
                    {tasks.map((task, idx) => (
                        <div
                            key={idx}
                            className={`${styles.taskItem} ${task.status === 'completed' ? styles.completed :
                                    task.status === 'active' ? styles.active : ''
                                }`}
                        >
                            <div className={`${styles.taskIcon} ${task.status === 'completed' ? styles.completed :
                                    task.status === 'active' ? styles.active : ''
                                }`}>
                                {task.status === 'completed' && <CheckCircle size={18} />}
                                {task.status === 'active' && <Loader size={18} />}
                                {task.status === 'pending' && <Circle size={18} />}
                            </div>
                            <span>{task.name}</span>
                        </div>
                    ))}
                </div>
            </div>
        );
    };

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

                {/* Terminal Header */}
                {activeTab === 'terminal' && terminalLines.length > 0 && (
                    <div className={styles.terminalHeader}>
                        <span className={`${styles.terminalDot} ${styles.dotRed}`}></span>
                        <span className={`${styles.terminalDot} ${styles.dotYellow}`}></span>
                        <span className={`${styles.terminalDot} ${styles.dotGreen}`}></span>
                        <span style={{ marginLeft: '8px' }}>ubuntu@sandbox:~/Center_</span>
                    </div>
                )}

                {/* Content */}
                <div className={styles.content}>
                    {activeTab === 'terminal' && renderTerminal()}
                    {activeTab === 'preview' && renderPreview()}
                </div>

                {/* Task Progress */}
                {renderTaskProgress()}
            </div>
        </>
    );
};

export default ManusPanel;
