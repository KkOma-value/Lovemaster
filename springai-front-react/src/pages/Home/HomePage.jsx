import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Heart, MessageCircle, Sparkles, Shield, Zap } from 'lucide-react';
import heroLoversUrl from '../../assets/illustrations/hero-lovers.svg';
import leafDecoUrl from '../../assets/illustrations/leaf-deco.svg';
import VideoCanvasBackground from '../../components/VideoCanvasBackground';
import { useChatRuntime } from '../../contexts/ChatRuntimeContext';
import pillStyles from '../../components/Chat/BackgroundRunsPill.module.css';
import styles from './HomePage.module.css';

const features = [
    {
        icon: Heart,
        title: '情感分析',
        description: '智能解读聊天内容，帮您理解对方的真实心意',
    },
    {
        icon: Sparkles,
        title: '话术建议',
        description: '提供贴心的回复建议，让每次对话都恰到好处',
    },
    {
        icon: Shield,
        title: '隐私保护',
        description: '所有对话内容安全处理，你的秘密只属于你',
    },
    {
        icon: Zap,
        title: '即时响应',
        description: '快速生成建议，不让你在关键时刻等待',
    },
];

const HomePage = () => {
    const navigate = useNavigate();
    const PageMotion = motion.div;
    const { activeRuns } = useChatRuntime();
    const activeRunCount = activeRuns.length;

    // Track card index for staggered entrance delay - 0.2s between each card
    let cardIndex = 0;
    const nextDelay = () => `${(cardIndex++) * 0.2}s`;

    return (
        <>
            <div
                className={styles.posterBg}
                aria-hidden="true"
            />
            <VideoCanvasBackground />

            {/* Global background runs indicator */}
            <AnimatePresence>
                {activeRunCount > 0 && (
                    <motion.button
                        className={pillStyles.pill}
                        onClick={() => {
                            const run = activeRuns[0];
                            navigate(`/chat/${run?.chatType === 'coach' ? 'coach' : 'loveapp'}`);
                        }}
                        initial={{ opacity: 0, scale: 0.8, y: -4 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.8, y: -4 }}
                        transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
                        style={{
                            position: 'fixed',
                            top: '16px',
                            right: '16px',
                            zIndex: 100
                        }}
                        aria-live="polite"
                        aria-label={`${activeRunCount} 个回复正在后台生成`}
                    >
                        <span className={pillStyles.dot} />
                        <span className={pillStyles.text}>
                            正在后台生成 {activeRunCount} 个回复
                        </span>
                    </motion.button>
                )}
            </AnimatePresence>

            <PageMotion
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{ duration: 0.24 }}
                className={styles.page}
            >
                <div className={styles.inner}>

                {/* ── Bento Grid ──────────────────────────────── */}
                <div className={styles.bentoGrid}>

                    {/* Hero card */}
                    <div
                        className={`${styles.heroCard} ${styles.staggerIn}`}
                        style={{ animationDelay: nextDelay() }}
                        onClick={() => navigate('/chat/loveapp')}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => e.key === 'Enter' && navigate('/chat/loveapp')}
                        aria-label="进入 Love Master 恋爱助手"
                    >
                        {/* Leaf decoration — top left */}
                        <img
                            src={leafDecoUrl}
                            alt=""
                            aria-hidden="true"
                            className={styles.leafDeco}
                        />

                        {/* Couple illustration — right side */}
                        <img
                            src={heroLoversUrl}
                            alt=""
                            aria-hidden="true"
                            className={styles.heroIllustration}
                        />

                        {/* Text content — sits above illustrations */}
                        <div className={styles.heroText}>
                            <h1 className={styles.heroTitle}>Love Master</h1>
                            <p className={styles.heroSubtitle}>你的专属恋爱助手</p>
                        </div>
                    </div>

                    {/* Feature card — 恋爱陪伴 */}
                    <div
                        className={`${styles.featureCard} ${styles.staggerIn}`}
                        style={{ animationDelay: nextDelay() }}
                        onClick={() => navigate('/chat/loveapp')}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => e.key === 'Enter' && navigate('/chat/loveapp')}
                        aria-label="恋爱陪伴"
                    >
                        <div className={styles.featureCardIcon} aria-hidden="true">
                            <Heart size={26} />
                        </div>
                        <div>
                            <h3 className={styles.featureCardTitle}>恋爱陪伴</h3>
                            <p className={styles.featureCardSubtitle}>温柔陪你聊天，随时倾听你的心声</p>
                        </div>
                    </div>

                    {/* Feature card — 恋爱教练 */}
                    <div
                        className={`${styles.featureCard} ${styles.staggerIn}`}
                        style={{ animationDelay: nextDelay() }}
                        onClick={() => navigate('/chat/coach')}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => e.key === 'Enter' && navigate('/chat/coach')}
                        aria-label="恋爱教练"
                    >
                        <div className={styles.featureCardIcon} aria-hidden="true">
                            <MessageCircle size={26} />
                        </div>
                        <div>
                            <h3 className={styles.featureCardTitle}>恋爱教练</h3>
                            <p className={styles.featureCardSubtitle}>分析聊天记录，给你最专业的建议</p>
                        </div>
                    </div>

                </div>

                {/* ── Features Section ─────────────────────────── */}
                <section
                    className={`${styles.featuresSection} ${styles.staggerIn}`}
                    style={{ animationDelay: nextDelay() }}
                    aria-label="产品特点"
                >
                    <h2 className={styles.featuresSectionTitle}>这次不是只陪你聊天</h2>

                    <div className={styles.featuresGrid}>
                        {features.map((feature, i) => {
                            const FeatureIcon = feature.icon;
                            return (
                                <div
                                    key={feature.title}
                                    className={`${styles.featureItem} ${styles.staggerIn}`}
                                    style={{ animationDelay: `${(cardIndex + i) * 0.2}s` }}
                                >
                                    <div className={styles.featureItemIcon} aria-hidden="true">
                                        <FeatureIcon size={28} />
                                    </div>
                                    <h4 className={styles.featureItemTitle}>{feature.title}</h4>
                                    <p className={styles.featureItemDesc}>{feature.description}</p>
                                </div>
                            );
                        })}
                    </div>
                </section>

                {/* ── Footer ───────────────────────────────────── */}
                <footer className={styles.footer}>
                    <p className={styles.footerText}>Love Master AI · 2026</p>
                </footer>

            </div>
        </PageMotion>
        </>
    );
};

export default HomePage;
