import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle, XCircle, Clock } from 'lucide-react';

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
        <div
            className="min-h-screen relative"
            style={{
                backgroundImage: 'url(/4.png)',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundAttachment: 'fixed',
            }}
        >
            {/* Blur + semi-transparent overlay */}
            <div
                className="absolute inset-0 backdrop-blur-sm"
                style={{ backgroundColor: 'rgba(255, 252, 245, 0.88)' }}
            />
            <div className="relative z-10 max-w-[1000px] mx-auto px-6 py-8">
            <div className="mb-6">
                <button 
                    onClick={() => navigate('/')} 
                    className="flex items-center gap-2 mb-4 bg-transparent border-none cursor-pointer text-gray-500 hover:text-gray-700 transition-colors"
                >
                    <ArrowLeft size={16} /> 返回首页
                </button>
                <h1 className="text-2xl font-bold text-gray-900 m-0">知识蒸馏审核区</h1>
            </div>

            <div className="flex gap-6 border-b border-gray-200 mb-6">
                {['pending_review', 'approved', 'rejected'].map(tab => (
                    <button 
                        key={tab}
                        className={`px-1 py-3 bg-transparent border-none cursor-pointer text-sm font-medium border-b-2 transition-colors ${
                            activeTab === tab ? 'text-blue-600 border-blue-600' : 'text-gray-500 border-transparent hover:text-gray-700 hover:border-gray-300'
                        }`}
                        onClick={() => { setActiveTab(tab); setSelectedIndex(0); }}
                    >
                        {tab === 'pending_review' ? '待审核' : (tab === 'approved' ? '已通过' : '已驳回')}
                        <span className="ml-2 bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full text-xs">
                            {candidates.filter(c => c.status === tab).length}
                        </span>
                    </button>
                ))}
            </div>

            <div className="flex flex-col gap-4">
                {filteredCandidates.length === 0 ? (
                    <div className="p-12 text-center text-gray-400">暂无数据</div>
                ) : (
                    filteredCandidates.map((item, index) => {
                        const tags = item.topic !== 'unknown' ? item.topic.split('.') : ['待分类'];
                        return (
                            <div 
                                key={item.id} 
                                className={`bg-white border rounded-xl p-5 transition-all cursor-pointer ${
                                    selectedIndex === index ? 'border-blue-500 ring-4 ring-blue-500/10' : 'border-gray-200 hover:border-gray-300'
                                }`}
                                onClick={() => setSelectedIndex(index)}
                            >
                                <div className="flex justify-between items-center mb-3">
                                    <div className="flex gap-2 flex-wrap">
                                        {tags.map((tag, idx) => (
                                            <span key={idx} className="bg-gray-100 text-gray-700 text-xs px-2 py-1 rounded font-medium">{tag}</span>
                                        ))}
                                    </div>
                                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full flex items-center gap-1 shrink-0 ${
                                        item.status === 'pending_review' ? 'bg-yellow-100 text-green-800' :
                                        item.status === 'approved' ? 'bg-blue-100 text-blue-800' :
                                        'bg-red-100 text-red-700'
                                    }`}>
                                        {item.status === 'pending_review' && <><Clock size={12} />待审核</>}
                                        {item.status === 'approved' && <><CheckCircle size={12} />已通过</>}
                                        {item.status === 'rejected' && <><XCircle size={12} />已驳回</>}
                                    </span>
                                </div>
                                <div className="mt-3 text-sm text-gray-600 leading-relaxed bg-gray-50 p-3 rounded-lg">
                                    <h4 className="text-xs text-gray-400 uppercase m-0 mb-2 font-semibold">经验提取摘要</h4>
                                    {item.content}
                                </div>
                                
                                {activeTab === 'pending_review' && selectedIndex === index && (
                                    <div className="mt-4 flex gap-3 justify-end">
                                        <button 
                                            className="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium border border-gray-200 bg-white text-red-500 hover:bg-red-50 cursor-pointer transition-colors" 
                                            onClick={(e) => { e.stopPropagation(); handleAction(item.id, 'rejected'); }}
                                        >
                                            驳回 (N)
                                        </button>
                                        <button 
                                            className="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium border border-transparent bg-emerald-500 text-white hover:bg-emerald-600 cursor-pointer transition-colors" 
                                            onClick={(e) => { e.stopPropagation(); handleAction(item.id, 'approved'); }}
                                        >
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
                <div className="mt-8 pt-6 border-t border-gray-200/60 flex gap-4 text-xs text-gray-400 justify-center items-center">
                    <span>使用快捷键进行审核：</span>
                    <span className="flex items-center gap-1.5"><span className="bg-gray-100 text-gray-700 font-semibold px-1.5 py-0.5 rounded text-[10px]">J</span> 下一个</span>
                    <span className="flex items-center gap-1.5"><span className="bg-gray-100 text-gray-700 font-semibold px-1.5 py-0.5 rounded text-[10px]">K</span> 上一个</span>
                    <span className="flex items-center gap-1.5"><span className="bg-gray-100 text-gray-700 font-semibold px-1.5 py-0.5 rounded text-[10px]">Y</span> 通过</span>
                    <span className="flex items-center gap-1.5"><span className="bg-gray-100 text-gray-700 font-semibold px-1.5 py-0.5 rounded text-[10px]">N</span> 驳回</span>
                </div>
            )}
            </div>
        </div>
    );
}