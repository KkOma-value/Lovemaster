import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle, XCircle, Clock } from 'lucide-react';
import styles from './KnowledgeReviewPage.module.css';

// Mock data initially for frontend verification step
const mockCandidates = [
    {
        id: 'c1',
        topic: '认识阶段.意图破冰.如何打招呼',
        content: '在认识初期的破冰阶段，不要过度查户口，应从对方可能感兴趣的共同点（如环境、朋友圈细节）切入，提供轻松、低压力的互动入口。',
        status: 'pending_review',
        createdAt: '2026-04-18T10:30:00Z',
    },
    {
        id: 'c2',
        topic: '暧昧阶段.意图升级.肢体接触时机',
        content: '不要突兀地升级肢体接触，可以在过马路时轻拉包带，或者在并排走时肩膀不经意地轻微摩擦，观察对方是否有后退或抵触的反应，以此判断舒适度。',
        status: 'pending_review',
        createdAt: '2026-04-19T09:12:00Z',
    }
];

export default function KnowledgeReviewPage() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('pending_review');
    const [candidates, setCandidates] = useState(mockCandidates);
    const [selectedIndex, setSelectedIndex] = useState(0);

    const filteredCandidates = candidates.filter(c => c.status === activeTab);
    const selectedCardInfo = filteredCandidates[selectedIndex];

    const handleAction = useCallback((id, newStatus) => {
        setCandidates(prev => prev.map(c => c.id === id ? { ...c, status: newStatus } : c));
        // Reset index so it doesn't out-of-bounds
        setSelectedIndex(0);
    }, []);

    // Keyboard Shortcuts (J/K to navigate, Y/N to approve/reject)
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (activeTab !== 'pending_review') return;
            if (filteredCandidates.length === 0) return;

            switch (e.key.toLowerCase()) {
                case 'j':
                    setSelectedIndex(i => Math.min(i + 1, filteredCandidates.length - 1));
                    break;
                case 'k':
                    setSelectedIndex(i => Math.max(i - 1, 0));
                    break;
                case 'y':
                    if (selectedCardInfo) handleAction(selectedCardInfo.id, 'approved');
                    break;
                case 'n':
                    if (selectedCardInfo) handleAction(selectedCardInfo.id, 'rejected');
                    break;
                default:
                    break;
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [activeTab, filteredCandidates, selectedCardInfo, handleAction]);

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <button 
                    onClick={() => navigate('/')} 
                    style={{ background: 'transparent', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', color: '#6b7280', marginBottom: '16px' }}
                >
                    <ArrowLeft size={16} /> 返回首页
                </button>
                <h1 className={styles.title}>知识蒸馏审核区</h1>
            </div>

            <div className={styles.tabs}>
                {['pending_review', 'approved', 'rejected'].map(tab => (
                    <button 
                        key={tab}
                        className={`${styles.tab} ${activeTab === tab ? styles.activeTab : ''}`}
                        onClick={() => { setActiveTab(tab); setSelectedIndex(0); }}
                    >
                        {tab === 'pending_review' ? '待审核' : (tab === 'approved' ? '已通过' : '已驳回')}
                        <span style={{ marginLeft: '8px', background: '#f3f4f6', padding: '2px 8px', borderRadius: '12px', fontSize: '12px' }}>
                            {candidates.filter(c => c.status === tab).length}
                        </span>
                    </button>
                ))}
            </div>

            <div className={styles.list}>
                {filteredCandidates.length === 0 ? (
                    <div style={{ padding: '48px', textAlign: 'center', color: '#9ca3af' }}>暂无数据</div>
                ) : (
                    filteredCandidates.map((item, index) => {
                        const tags = item.topic !== 'unknown' ? item.topic.split('.') : ['待分类'];
                        return (
                            <div 
                                key={item.id} 
                                className={`${styles.card} ${selectedIndex === index ? styles.selectedCard : ''}`}
                                onClick={() => setSelectedIndex(index)}
                            >
                                <div className={styles.cardHeader}>
                                    <div className={styles.topicTags}>
                                        {tags.map((tag, idx) => (
                                            <span key={idx} className={styles.tag}>{tag}</span>
                                        ))}
                                    </div>
                                    <span className={`${styles.status} ${styles[item.status]}`}>
                                        {item.status === 'pending_review' && <span style={{ display: 'flex', alignItems: 'center', gap: '4px'}}><Clock size={12} />待审核</span>}
                                        {item.status === 'approved' && <span style={{ display: 'flex', alignItems: 'center', gap: '4px'}}><CheckCircle size={12} />已通过</span>}
                                        {item.status === 'rejected' && <span style={{ display: 'flex', alignItems: 'center', gap: '4px'}}><XCircle size={12} />已驳回</span>}
                                    </span>
                                </div>
                                <div className={styles.contentPreview}>
                                    <h4>经验提取摘要</h4>
                                    {item.content}
                                </div>
                                
                                {activeTab === 'pending_review' && selectedIndex === index && (
                                    <div className={styles.actions}>
                                        <button className={`${styles.btn} ${styles.btnReject}`} onClick={(e) => { e.stopPropagation(); handleAction(item.id, 'rejected'); }}>
                                            驳回 (N)
                                        </button>
                                        <button className={`${styles.btn} ${styles.btnApprove}`} onClick={(e) => { e.stopPropagation(); handleAction(item.id, 'approved'); }}>
                                            采纳入库 (Y)
                                        </button>
                                    </div>
                                )}
                            </div>
                        );
                    })
                )}
            </div>

            {activeTab === 'pending_review' && filteredCandidates.length > 0 && (
                <div className={styles.keyboardHints}>
                    <span>使用快捷键进行审核：</span>
                    <span><span className={styles.key}>J</span> 下一个</span>
                    <span><span className={styles.key}>K</span> 上一个</span>
                    <span><span className={styles.key}>Y</span> 通过</span>
                    <span><span className={styles.key}>N</span> 驳回</span>
                </div>
            )}
        </div>
    );
}