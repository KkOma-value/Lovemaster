import React, { useState } from 'react';
import { ThumbsUp, ThumbsDown, BookmarkPlus, BookmarkCheck, Copy, Check } from 'lucide-react';
import { createKnowledgeCandidate, createFeedbackEvent } from '../../services/chatApi';

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
        <div className="flex items-center gap-1.5 mt-2 pt-2">
            <button 
                type="button" 
                className="flex items-center justify-center min-w-[32px] h-[32px] rounded-md transition-colors text-gray-500 hover:bg-gray-100 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed border-none bg-transparent cursor-pointer" 
                onClick={handleCopy} 
                aria-label="复制回复到输入框"
            >
                {copied ? <Check size={16} /> : <Copy size={16} />}
            </button>
            <button
                type="button"
                className={`flex items-center justify-center min-w-[32px] h-[32px] rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed border-none cursor-pointer ${
                    thumbsStatus === 'up' 
                        ? 'text-[var(--primary-dark)] bg-[#FCE7D5]' 
                        : 'bg-transparent text-gray-500 hover:bg-gray-100 hover:text-gray-700'
                }`}
                onClick={handleThumbsUp}
                aria-label="点赞"
                disabled={!chatId}
            >
                <ThumbsUp size={16} />
            </button>
            <button
                type="button"
                className={`flex items-center justify-center min-w-[32px] h-[32px] rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed border-none cursor-pointer ${
                    thumbsStatus === 'down' 
                        ? 'text-[#A55F5F] bg-[#F8E0E0]' 
                        : 'bg-transparent text-gray-500 hover:bg-gray-100 hover:text-gray-700'
                }`}
                onClick={handleThumbsDown}
                aria-label="点踩"
                disabled={!chatId}
            >
                <ThumbsDown size={16} />
            </button>

            <button
                type="button"
                className={`flex items-center justify-center h-[32px] w-auto px-2 gap-1.5 rounded-md transition-colors disabled:opacity-[0.7] border-none cursor-pointer ${
                    wikiStatus === 'candidate' ? 'text-[var(--sage)] bg-[rgba(143,176,159,0.16)]' : 
                    wikiStatus === 'rejected' ? 'text-[#ef4444] bg-[#fef2f2]' : 
                    wikiStatus === 'unknown_topic' ? 'text-[#b45309] bg-[#fffbeb]' : 
                    'bg-transparent text-gray-500 hover:bg-gray-100 hover:text-gray-700'
                } ${wikiStatus === 'submitting' || wikiStatus === 'candidate' || !chatId ? 'cursor-not-allowed' : ''}`}
                onClick={handleSaveToWiki}
                disabled={wikiStatus === 'submitting' || wikiStatus === 'candidate' || !chatId}
                aria-label="提交到知识蒸馏"
            >
                {wikiStatus === 'candidate' || wikiStatus === 'unknown_topic' ? <BookmarkCheck size={16} /> : <BookmarkPlus size={16} />}
                {wikiStatus === 'candidate' && <span className="text-[12px] font-medium">已提交蒸馏队列</span>}
                {wikiStatus === 'unknown_topic' && <span className="text-[12px] font-medium">待人工归类</span>}
                {wikiStatus === 'rejected' && <span className="text-[12px] font-medium">提交失败</span>}
                {wikiStatus === 'submitting' && <span className="text-[12px] font-medium">提交中...</span>}
            </button>
        </div>
    );
};

export default ActionBar;
