import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './MarkdownRenderer.module.css';

const MarkdownRenderer = ({ content }) => {
    return (
        <div className={styles.markdown}>
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                    // Custom code block rendering
                    code({ node, inline, className, children, ...props }) {
                        const match = /language-(\w+)/.exec(className || '');
                        return !inline ? (
                            <div className={styles.codeBlock}>
                                {match && (
                                    <div className={styles.codeHeader}>
                                        <span className={styles.codeLang}>{match[1]}</span>
                                    </div>
                                )}
                                <pre className={styles.pre}>
                                    <code className={className} {...props}>
                                        {children}
                                    </code>
                                </pre>
                            </div>
                        ) : (
                            <code className={styles.inlineCode} {...props}>
                                {children}
                            </code>
                        );
                    },
                    // Custom link rendering
                    a({ href, children, ...props }) {
                        return (
                            <a
                                href={href}
                                target="_blank"
                                rel="noopener noreferrer"
                                className={styles.link}
                                {...props}
                            >
                                {children}
                            </a>
                        );
                    }
                }}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
};

export default MarkdownRenderer;
