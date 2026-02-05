import React from 'react';
import { Plus, MessageSquare, Trash2, ChevronLeft, ChevronRight, Heart } from 'lucide-react';

const ChatSidebar = ({
    isOpen,
    onToggle,
    onNewChat,
    currentChatId,
    chatList = [],
    onSelectChat,
    onDeleteChat
}) => {
    const [hoveredId, setHoveredId] = React.useState(null);

    const handleDelete = (e, chatId) => {
        e.stopPropagation();
        if (confirm('确定要删除这个对话吗？')) {
            onDeleteChat?.(chatId);
        }
    };

    // Collapsed state
    if (!isOpen) {
        return (
            <div style={{
                position: 'fixed',
                left: 0,
                top: 0,
                bottom: 0,
                zIndex: 50,
                display: 'flex',
                alignItems: 'flex-start',
                paddingTop: '24px',
                paddingLeft: '16px'
            }}>
                <button
                    style={{
                        padding: '12px',
                        background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 100%)',
                        border: 'none',
                        borderRadius: '14px',
                        boxShadow: '0 8px 24px rgba(253, 164, 175, 0.35)',
                        cursor: 'pointer',
                        transition: 'all 0.3s ease',
                        color: '#fff'
                    }}
                    onClick={onToggle}
                    title="展开侧边栏"
                    onMouseEnter={(e) => {
                        e.currentTarget.style.transform = 'scale(1.05)';
                        e.currentTarget.style.boxShadow = '0 12px 32px rgba(253, 164, 175, 0.45)';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.transform = 'scale(1)';
                        e.currentTarget.style.boxShadow = '0 8px 24px rgba(253, 164, 175, 0.35)';
                    }}
                >
                    <ChevronRight size={20} />
                </button>
            </div>
        );
    }

    return (
        <aside style={{
            position: 'fixed',
            left: 0,
            top: 0,
            bottom: 0,
            width: '280px',
            background: 'linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%)',
            borderRight: '1px solid #FECDD3',
            zIndex: 50,
            display: 'flex',
            flexDirection: 'column',
            fontFamily: "'Inter', -apple-system, sans-serif"
        }}>
            {/* Header */}
            <div style={{
                height: '72px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0 20px',
                borderBottom: '1px solid #FECDD3',
                background: 'rgba(255, 241, 242, 0.5)'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Heart size={20} color="#F43F5E" fill="#FDA4AF" />
                    <span style={{
                        fontWeight: 700,
                        fontSize: '18px',
                        background: 'linear-gradient(135deg, #F43F5E 0%, #EC4899 100%)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text'
                    }}>
                        聊天记录
                    </span>
                </div>
                <button
                    style={{
                        padding: '8px',
                        backgroundColor: 'transparent',
                        border: 'none',
                        borderRadius: '10px',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease'
                    }}
                    onClick={onToggle}
                    title="收起侧边栏"
                    onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor = '#FFE4E6';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor = 'transparent';
                    }}
                >
                    <ChevronLeft size={20} color="#F43F5E" />
                </button>
            </div>

            <div style={{ padding: '16px', flex: 1, overflow: 'hidden' }}>
                {/* New Chat Button */}
                <button
                    onClick={onNewChat}
                    style={{
                        width: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '8px',
                        padding: '14px 20px',
                        background: 'linear-gradient(135deg, #FDA4AF 0%, #FB7185 100%)',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '16px',
                        fontWeight: 600,
                        fontSize: '15px',
                        cursor: 'pointer',
                        marginBottom: '20px',
                        boxShadow: '0 8px 24px rgba(253, 164, 175, 0.35)',
                        transition: 'all 0.3s ease'
                    }}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.boxShadow = '0 12px 32px rgba(253, 164, 175, 0.5)';
                        e.currentTarget.style.transform = 'translateY(-2px)';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.boxShadow = '0 8px 24px rgba(253, 164, 175, 0.35)';
                        e.currentTarget.style.transform = 'translateY(0)';
                    }}
                >
                    <Plus size={18} />
                    新对话
                </button>

                {/* Chat List */}
                <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                    overflowY: 'auto',
                    maxHeight: 'calc(100vh - 220px)'
                }}>
                    {chatList.map(chat => {
                        const isActive = currentChatId === chat.id;
                        const isHovered = hoveredId === chat.id;

                        return (
                            <div
                                key={chat.id}
                                style={{
                                    position: 'relative',
                                    padding: '14px 16px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '12px',
                                    cursor: 'pointer',
                                    borderRadius: '14px',
                                    backgroundColor: isActive ? '#FFE4E6' : (isHovered ? '#FFF5F6' : 'transparent'),
                                    border: isActive ? '1px solid #FECDD3' : '1px solid transparent',
                                    transition: 'all 0.2s ease'
                                }}
                                onClick={() => onSelectChat(chat.id)}
                                onMouseEnter={() => setHoveredId(chat.id)}
                                onMouseLeave={() => setHoveredId(null)}
                            >
                                <MessageSquare size={18} color={isActive ? '#F43F5E' : '#9CA3AF'} />
                                <span style={{
                                    flex: 1,
                                    fontSize: '14px',
                                    fontWeight: isActive ? 600 : 400,
                                    color: isActive ? '#BE123C' : '#374151',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap'
                                }}>
                                    {chat.title || 'New Conversation'}
                                </span>
                                {isHovered && (
                                    <button
                                        style={{
                                            padding: '6px',
                                            backgroundColor: 'transparent',
                                            border: 'none',
                                            cursor: 'pointer',
                                            color: '#F43F5E',
                                            borderRadius: '8px',
                                            transition: 'all 0.2s ease'
                                        }}
                                        onClick={(e) => handleDelete(e, chat.id)}
                                        title="删除对话"
                                        onMouseEnter={(e) => {
                                            e.currentTarget.style.backgroundColor = '#FEE2E2';
                                        }}
                                        onMouseLeave={(e) => {
                                            e.currentTarget.style.backgroundColor = 'transparent';
                                        }}
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Footer */}
            <div style={{
                marginTop: 'auto',
                padding: '20px',
                borderTop: '1px solid #FECDD3',
                background: 'linear-gradient(135deg, #FFF1F2 0%, #FFE4E6 100%)'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '18px' }}>💕</span>
                    <span style={{
                        fontSize: '14px',
                        fontWeight: 600,
                        background: 'linear-gradient(135deg, #F43F5E 0%, #EC4899 100%)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text'
                    }}>
                        Love Master
                    </span>
                </div>
            </div>
        </aside>
    );
};

export default ChatSidebar;
