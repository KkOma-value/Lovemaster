import React, { useState } from 'react';
import { FileText, Image as ImageIcon, Code, File } from 'lucide-react';
import styles from './ManusPanel.module.css';

const ManusPreview = ({ files }) => {
    const [loadingStates, setLoadingStates] = useState({});

    if (files.length === 0) {
        return (
            <div className={styles.emptyState}>
                <div className={styles.emptyIcon}>
                    <File size={32} />
                </div>
                <h3 className={styles.emptyTitle}>暂无文件</h3>
                <p className={styles.emptyDescription}>
                    生成的文件将在这里显示
                </p>
            </div>
        );
    }

    const getIcon = (type) => {
        switch (type) {
            case 'pdf':
                return <FileText size={18} />;
            case 'image':
                return <ImageIcon size={18} />;
            case 'code':
                return <Code size={18} />;
            default:
                return <File size={18} />;
        }
    };

    const getIconClass = (type) => {
        switch (type) {
            case 'pdf': return styles.previewIconPdf;
            case 'image': return styles.previewIconImage;
            case 'code': return styles.previewIconCode;
            default: return styles.previewIconPdf;
        }
    };

    const renderContent = (file) => {
        if (file.type === 'image' && file.url) {
            return (
                <div className={styles.previewContent}>
                    {loadingStates[file.name] !== false && (
                        <div style={{ color: '#9CA3AF', fontSize: '0.85rem' }}>加载中...</div>
                    )}
                    <img
                        src={file.url}
                        alt={file.name}
                        className={styles.previewImage}
                        onLoad={() => setLoadingStates(prev => ({ ...prev, [file.name]: false }))}
                        onError={() => setLoadingStates(prev => ({ ...prev, [file.name]: false }))}
                        style={{ display: loadingStates[file.name] === false ? 'block' : 'none' }}
                    />
                </div>
            );
        }

        if (file.type === 'pdf' && file.url) {
            return (
                <div className={styles.previewContent}>
                    <iframe
                        src={file.url}
                        className={styles.previewPdf}
                        title={file.name}
                    />
                </div>
            );
        }

        if (file.type === 'code' && file.content) {
            return (
                <div className={styles.previewContent}>
                    <pre className={styles.codePreview}>{file.content}</pre>
                </div>
            );
        }

        return (
            <div className={styles.previewContent}>
                <div className={styles.previewError}>
                    <File size={32} className={styles.previewErrorIcon} />
                    <span>无法预览此文件类型</span>
                </div>
            </div>
        );
    };

    return (
        <div className={styles.previewContainer}>
            {files.map((file, index) => (
                <div key={index} className={styles.previewCard}>
                    <div className={styles.previewHeader}>
                        <div className={`${styles.previewIcon} ${getIconClass(file.type)}`}>
                            {getIcon(file.type)}
                        </div>
                        <div className={styles.previewInfo}>
                            <div className={styles.previewTitle}>{file.name}</div>
                            <div className={styles.previewMeta}>
                                {file.type.toUpperCase()} {file.size ? `· ${file.size}` : ''}
                            </div>
                        </div>
                    </div>
                    {renderContent(file)}
                </div>
            ))}
        </div>
    );
};

export default ManusPreview;
