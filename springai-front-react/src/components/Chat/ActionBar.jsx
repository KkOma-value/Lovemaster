import React, { useState } from 'react';
import { ThumbsUp, ThumbsDown, BookmarkPlus, BookmarkCheck, Copy, Check } from 'lucide-react';
import { createKnowledgeCandidate, createFeedbackEvent } from '../../services/chatApi';
import { useChatRuntime } from '../../contexts/ChatRuntimeContext';

const PERSISTED_WIKI_STATES = new Set(['candidate', 'unknown_topic', 'approved']);

const ActionBar = ({
    chatType,
    chatId,
    messageId,
    runId,
    question,
    answer,
    thumbsStatus = null,
    wikiStatus = 'idle',
    onCopyAction
}) => {
    const { updateMessage } = useChatRuntime();
    const [copied, setCopied] = useState(false);
    const [localWikiStatus, setLocalWikiStatus] = useState(null); // transient: submitting / rejected

    const effectiveWikiStatus = localWikiStatus || wikiStatus;

    const persistMessage = (patch) => {
        if (messageId) {
            updateMessage(chatType, chatId, messageId, patch);
        }
    };

    const handleCopy = () => {
        if (onCopyAction) onCopyAction(answer);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleThumbsUp = async () => {
        if (!chatId || thumbsStatus === 'up') return;
        const previous = thumbsStatus;
        persistMessage({ thumbs: 'up' });
        try {
            await createFeedbackEvent(null, chatId, runId, 'thumbs_up', '', 1.0, {});
        } catch {
            persistMessage({ thumbs: previous });
        }
    };

    const handleThumbsDown = async () => {
        if (!chatId || thumbsStatus === 'down') return;
        const previous = thumbsStatus;
        persistMessage({ thumbs: 'down' });
        try {
            await createFeedbackEvent(null, chatId, runId, 'thumbs_down', '', -1.0, {});
        } catch {
            persistMessage({ thumbs: previous });
        }
    };

    const handleSaveToWiki = async () => {
        if (effectiveWikiStatus === 'submitting' || PERSISTED_WIKI_STATES.has(effectiveWikiStatus)) return;
        if (!chatId || !answer?.trim()) {
            setLocalWikiStatus('rejected');
            return;
        }

        setLocalWikiStatus('submitting');
        try {
            const resp = await createKnowledgeCandidate(chatId, runId, question, answer, 'manual', 1.0);
            const unknownTopic = Boolean(resp?.unknownTopic) || resp?.status === 'unknown_topic';
            const finalStatus = unknownTopic ? 'unknown_topic' : 'candidate';
            setLocalWikiStatus(null);
            persistMessage({ wikiStatus: finalStatus });
        } catch {
            setLocalWikiStatus('rejected');
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
                    effectiveWikiStatus === 'candidate' ? 'text-[var(--sage)] bg-[rgba(143,176,159,0.16)]' :
                    effectiveWikiStatus === 'approved' ? 'text-[var(--primary)] bg-[rgba(236,135,68,0.14)]' :
                    effectiveWikiStatus === 'rejected' ? 'text-[#ef4444] bg-[#fef2f2]' :
                    effectiveWikiStatus === 'unknown_topic' ? 'text-[#b45309] bg-[#fffbeb]' :
                    'bg-transparent text-gray-500 hover:bg-gray-100 hover:text-gray-700'
                } ${effectiveWikiStatus === 'submitting' || effectiveWikiStatus === 'candidate' || !chatId ? 'cursor-not-allowed' : ''}`}
                onClick={handleSaveToWiki}
                disabled={effectiveWikiStatus === 'submitting' || effectiveWikiStatus === 'candidate' || !chatId}
                aria-label="提交到知识蒸馏"
            >
                {effectiveWikiStatus === 'candidate' || effectiveWikiStatus === 'unknown_topic' ? <BookmarkCheck size={16} /> : <BookmarkPlus size={16} />}
                {effectiveWikiStatus === 'candidate' && <span className="text-[12px] font-medium">知识贡献中</span>}
                {effectiveWikiStatus === 'unknown_topic' && <span className="text-[12px] font-medium">反馈积累中</span>}
                {effectiveWikiStatus === 'rejected' && <span className="text-[12px] font-medium">提交失败</span>}
                {effectiveWikiStatus === 'submitting' && <span className="text-[12px] font-medium">提交中...</span>}
                {effectiveWikiStatus === 'approved' && <span className="text-[12px] font-medium">已入知识库</span>}
            </button>
        </div>
    );
};

export default ActionBar;
