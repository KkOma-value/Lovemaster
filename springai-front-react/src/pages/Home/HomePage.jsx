import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, MessageCircle, Sparkles, Shield, Zap } from 'lucide-react';

const HomePage = () => {
    const navigate = useNavigate();

    const features = [
        {
            icon: Heart,
            title: '情感分析',
            description: '智能解读聊天内容，帮你理解对方的真实心意'
        },
        {
            icon: Sparkles,
            title: '话术建议',
            description: '提供贴心的回复建议，让每次对话都恰到好处'
        },
        {
            icon: Shield,
            title: '隐私保护',
            description: '所有对话内容安全处理，你的秘密只属于你'
        },
        {
            icon: Zap,
            title: '即时响应',
            description: '快速生成建议，不让你在关键时刻等待'
        }
    ];

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(180deg, #FDF2F4 0%, #FAE8EB 100%)',
            padding: '48px 24px',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
        }}>
            <div style={{
                maxWidth: '1000px',
                margin: '0 auto'
            }}>
                {/* Bento Grid Layout */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(12, 1fr)',
                    gap: '20px',
                    marginBottom: '48px'
                }}>
                    {/* Main Hero Card - Soft Coral Pink Gradient */}
                    <div
                        onClick={() => navigate('/chat/loveapp')}
                        style={{
                            gridColumn: 'span 7',
                            gridRow: 'span 2',
                            background: 'linear-gradient(135deg, #F9A8B4 0%, #FBBED0 35%, #FDD5E0 65%, #FDE8EC 100%)',
                            borderRadius: '32px',
                            padding: '48px 40px',
                            minHeight: '380px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'flex-end',
                            cursor: 'pointer',
                            boxShadow: '0 20px 60px rgba(249, 168, 180, 0.3)',
                            transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
                            position: 'relative',
                            overflow: 'hidden'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-6px)';
                            e.currentTarget.style.boxShadow = '0 28px 80px rgba(249, 168, 180, 0.4)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 20px 60px rgba(249, 168, 180, 0.3)';
                        }}
                    >
                        <h1 style={{
                            fontSize: 'clamp(2rem, 4vw, 2.8rem)',
                            fontWeight: 700,
                            color: '#fff',
                            marginBottom: '14px',
                            textShadow: '0 2px 12px rgba(0,0,0,0.1)',
                            position: 'relative',
                            zIndex: 1
                        }}>
                            Hi, 欢迎来到 Love Master
                        </h1>
                        <p style={{
                            fontSize: '16px',
                            color: 'rgba(255,255,255,0.9)',
                            fontWeight: 500,
                            position: 'relative',
                            zIndex: 1
                        }}>
                            你的专属恋爱助手已准备好
                        </p>
                    </div>

                    {/* Top Right Card - Love Companion */}
                    <div
                        onClick={() => navigate('/chat/loveapp')}
                        style={{
                            gridColumn: 'span 5',
                            backgroundColor: '#FFFFFF',
                            borderRadius: '28px',
                            padding: '28px',
                            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '16px'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-4px)';
                            e.currentTarget.style.boxShadow = '0 12px 40px rgba(0, 0, 0, 0.1)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.05)';
                        }}
                    >
                        <div style={{
                            width: '52px',
                            height: '52px',
                            background: 'linear-gradient(135deg, #F9A8B4 0%, #FBBED0 100%)',
                            borderRadius: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 6px 20px rgba(249, 168, 180, 0.35)'
                        }}>
                            <Heart size={26} color="#fff" />
                        </div>
                        <div>
                            <h3 style={{
                                fontSize: '18px',
                                fontWeight: 700,
                                marginBottom: '6px',
                                color: '#1F2937'
                            }}>
                                恋爱陪伴
                            </h3>
                            <p style={{ fontSize: '14px', color: '#9CA3AF', fontWeight: 400 }}>
                                温柔陪你聊天
                            </p>
                        </div>
                    </div>

                    {/* Bottom Right Card - Love Coach */}
                    <div
                        onClick={() => navigate('/chat/coach')}
                        style={{
                            gridColumn: 'span 5',
                            backgroundColor: '#FFFFFF',
                            borderRadius: '28px',
                            padding: '28px',
                            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '16px'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-4px)';
                            e.currentTarget.style.boxShadow = '0 12px 40px rgba(0, 0, 0, 0.1)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.05)';
                        }}
                    >
                        <div style={{
                            width: '52px',
                            height: '52px',
                            background: 'linear-gradient(135deg, #F9A8B4 0%, #FBBED0 100%)',
                            borderRadius: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 6px 20px rgba(249, 168, 180, 0.35)'
                        }}>
                            <MessageCircle size={26} color="#fff" />
                        </div>
                        <div>
                            <h3 style={{
                                fontSize: '18px',
                                fontWeight: 700,
                                marginBottom: '6px',
                                color: '#1F2937'
                            }}>
                                恋爱教练
                            </h3>
                            <p style={{ fontSize: '14px', color: '#9CA3AF', fontWeight: 400 }}>
                                分析聊天记录
                            </p>
                        </div>
                    </div>
                </div>

                {/* Features Section */}
                <div style={{
                    backgroundColor: '#FFFFFF',
                    borderRadius: '32px',
                    padding: '48px',
                    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)'
                }}>
                    <h2 style={{
                        fontSize: '24px',
                        fontWeight: 700,
                        marginBottom: '40px',
                        textAlign: 'center',
                        color: '#1F2937'
                    }}>
                        为什么选择我们
                    </h2>

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(4, 1fr)',
                        gap: '24px'
                    }}>
                        {features.map((feature, index) => (
                            <div key={index} style={{
                                textAlign: 'center',
                                padding: '24px 16px'
                            }}>
                                <div style={{
                                    width: '56px',
                                    height: '56px',
                                    background: 'linear-gradient(135deg, #F9A8B4 0%, #FDD5E0 100%)',
                                    borderRadius: '18px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    margin: '0 auto 16px',
                                    boxShadow: '0 6px 20px rgba(249, 168, 180, 0.3)'
                                }}>
                                    <feature.icon size={28} color="#fff" />
                                </div>
                                <h4 style={{
                                    fontSize: '16px',
                                    fontWeight: 600,
                                    marginBottom: '8px',
                                    color: '#1F2937'
                                }}>
                                    {feature.title}
                                </h4>
                                <p style={{
                                    fontSize: '13px',
                                    color: '#9CA3AF',
                                    lineHeight: 1.5
                                }}>
                                    {feature.description}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Footer */}
                <footer style={{
                    textAlign: 'center',
                    marginTop: '48px',
                    paddingBottom: '24px'
                }}>
                    <p style={{
                        fontSize: '14px',
                        fontWeight: 500,
                        color: '#9CA3AF'
                    }}>
                        Love Master AI · 2026
                    </p>
                </footer>
            </div>
        </div>
    );
};

export default HomePage;
