import React from 'react';
import styles from './HomePage.module.css';
import { useNavigate } from 'react-router-dom';
import { Card } from '../../components/ui/Card';
import { MessageCircleHeart, Target, Heart, Sparkles, Shield, Zap } from 'lucide-react';

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
            description: '所有对话内容本地处理，你的秘密只属于你'
        },
        {
            icon: Zap,
            title: '即时响应',
            description: '快速生成建议，不让你在关键时刻等待'
        }
    ];

    return (
        <div className={styles.container}>
            {/* Hero Section */}
            <header className={styles.hero}>
                <div className={styles.heroContent}>
                    <h1 className={styles.title}>
                        欢迎来到
                        <span className={styles.highlight}>恋爱大师</span>
                    </h1>
                    <p className={styles.subtitle}>
                        帮你把喜欢说清楚，把关系经营好
                    </p>
                </div>
            </header>

            {/* Main Cards */}
            <section className={styles.mainCards}>
                <Card
                    hoverable
                    className={styles.card}
                    onClick={() => navigate('/chat/loveapp')}
                >
                    <div className={styles.cardGlow} />
                    <div className={styles.iconWrapper}>
                        <MessageCircleHeart size={36} className={styles.iconPink} />
                    </div>
                    <h3 className={styles.cardTitle}>恋爱陪伴</h3>
                    <p className={styles.cardDesc}>
                        温柔陪你聊天，帮你梳理情绪，提供话术建议，就像你最好的朋友一样陪你聊天。
                    </p>
                    <div className={styles.cardAction}>
                        开始聊天
                        <span className={styles.arrow}>→</span>
                    </div>
                </Card>

                <Card
                    hoverable
                    className={styles.card}
                    onClick={() => navigate('/chat/coach')}
                >
                    <div className={styles.cardGlow} />
                    <div className={styles.iconWrapper}>
                        <Target size={36} className={styles.iconCoral} />
                    </div>
                    <h3 className={styles.cardTitle}>恋爱教练</h3>
                    <p className={styles.cardDesc}>
                        上传聊天截图，我帮你分析对方意图，制定稳赢的回复策略和约会计划。
                    </p>
                    <div className={styles.cardAction}>
                        开始分析
                        <span className={styles.arrow}>→</span>
                    </div>
                </Card>
            </section>

            {/* Features Section */}
            <section className={styles.features}>
                <h2 className={styles.featuresTitle}>为什么选择我们</h2>
                <div className={styles.featuresGrid}>
                    {features.map((feature, index) => (
                        <div key={index} className={styles.featureItem}>
                            <div className={styles.featureIcon}>
                                <feature.icon size={24} />
                            </div>
                            <h4 className={styles.featureTitle}>{feature.title}</h4>
                            <p className={styles.featureDesc}>{feature.description}</p>
                        </div>
                    ))}
                </div>
            </section>

            {/* Footer */}
            <footer className={styles.footer}>
                <p>用心陪伴每一段感情 💕</p>
            </footer>
        </div>
    );
};

export default HomePage;
