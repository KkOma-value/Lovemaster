import React, { useState } from 'react';
import { ThumbsUp, ThumbsDown, BookmarkPlus, BookmarkCheck, Copy, Check } from 'lucide-react';
import { createKnowledgeCandidate, createFeedbackEvent } from '../../services/chatApi';
import styles from './ActionBar.module.css';

const ActionBar = ({ chatId, runId, question, answer, onCopyAction }) => {
    const [copied, setCopied] = useState(false);
    const [wikiStatus, setWikiStatus] = useState('idle'); // idle, submitting, candidate, rejected, unknown_topic
    const [thumbsStatus, setThumbsStatus] = useState(null); // up, down, null

    const handleCopy = () => {
        if (onCopyAction) onCopyAction(answer);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleThumbsUp = async () => {
        if (!chatId || thumbsStatus === 'up') return;
        const previous = thumbsStatus;
        setThumbsStatus('up');
        try {
            await createFeedbackEvent(null, chatId, runId, 'thumbs_up', '', 1.0, {});
        } catch {
            setThumbsStatus(previous);
        }
    };

    const handleThumbsDown = async () => {
        if (!chatId || thumbsStatus === 'down') return;
        const previous = thumbsStatus;
        setThumbsStatus('down');
        try {
            await createFeedbackEvent(null, chatId, runId, 'thumbs_down', '', -1.0, {});
        } catch {
            setThumbsStatus(previous);
        }
    };

    const handleSaveToWiki = async () => {
        if (wikiStatus === 'submitting') return;
        if (!chatId || !answer?.trim()) {
            setWikiStatus('rejected');
            return;
        }

        setWikiStatus('submitting');
        try {
            const resp = await createKnowledgeCandidate(chatId, runId, question, answer, 'manual', 1.0);
            const unknownTopic = Boolean(resp?.unknownTopic) || resp?.status === 'unknown_topic';
            if (unknownTopic) {
                setWikiStatus('unknown_topic');
            } else {
                setWikiStatus('candidate');
            }
        } catch {
            setWikiStatus('rejected');
        }
    };

    return (
        <div className={styles.actionBar}>
            <button type="button" className={styles.actionBtn} onClick={handleCopy} aria-label="复制回复到输入框">
                {copied ? <Check size={16} /> : <Copy size={16} />}
            </button>
            <button
                type="button"
                className={`${styles.actionBtn} ${thumbsStatus === 'up' ? styles.active : ''}`}
                onClick={handleThumbsUp}
                aria-label="点赞"
                disabled={!chatId}
            >
                <ThumbsUp size={16} />
            </button>
            <button
                type="button"
                className={`${styles.actionBtn} ${thumbsStatus === 'down' ? styles.active : ''}`}
                onClick={handleThumbsDown}
                aria-label="点踩"
                disabled={!chatId}
            >
                <ThumbsDown size={16} />
            </button>

            <button
                type="button"
                className={`${styles.actionBtn} ${styles.wikiBtn} ${wikiStatus === 'candidate' ? styles.candidate : ''} ${wikiStatus === 'rejected' ? styles.rejected : ''} ${wikiStatus === 'unknown_topic' ? styles.unknownTopic : ''}`}
                onClick={handleSaveToWiki}
                disabled={wikiStatus === 'submitting' || wikiStatus === 'candidate' || !chatId}
                aria-label="提交到知识蒸馏"
            >
                {wikiStatus === 'candidate' || wikiStatus === 'unknown_topic' ? <BookmarkCheck size={16} /> : <BookmarkPlus size={16} />}
                {wikiStatus === 'candidate' && <span className={styles.wikiText}>已提交蒸馏队列</span>}
                {wikiStatus === 'unknown_topic' && <span className={styles.wikiText}>待人工归类</span>}
                {wikiStatus === 'rejected' && <span className={styles.wikiText}>提交失败</span>}
                {wikiStatus === 'submitting' && <span className={styles.wikiText}>提交中...</span>}
            </button>
        </div>
    );
};

export default ActionBar;