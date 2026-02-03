<template>
  <div class="home-page">
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="app-header">
        <div class="logo-container">
          <div class="logo-icon">💕</div>
          <h2>恋爱大师</h2>
        </div>
        <div class="header-controls">
          <el-tooltip content="项目源码" placement="bottom" :hide-after="2000">
            <el-button type="primary" :icon="Link" circle @click="openRepo" class="repo-btn" />
          </el-tooltip>
        </div>
      </el-header>

      <!-- 主体内容 -->
      <el-main class="app-main">
        <div class="welcome-section">
          <div class="welcome-content">
            <!-- 装饰元素 -->
            <div class="floating-hearts">
              <span class="heart" style="--delay: 0s; --x: 20%;">💗</span>
              <span class="heart" style="--delay: 1s; --x: 80%;">💕</span>
              <span class="heart" style="--delay: 2s; --x: 50%;">💖</span>
            </div>

            <h1 class="welcome-title">
              <span class="title-emoji">💘</span>
              <span class="title-text">欢迎来到</span>
              <span class="title-highlight">恋爱大师</span>
            </h1>
            <p class="welcome-subtitle">✨ 帮你把喜欢说清楚，把关系经营好 ✨</p>
            
            <!-- 功能卡片 -->
            <div class="feature-cards">
              <div class="feature-card love-app" @click="goToLoveApp">
                <div class="card-glow"></div>
                <div class="card-icon">
                  <span>💬</span>
                </div>
                <div class="card-content">
                  <h3>恋爱陪伴 <span class="badge">Love App</span></h3>
                  <p>温柔陪你聊天，帮你梳理情绪，给出更自然的表达方式</p>
                  <ul class="feature-list">
                    <li>💬 话术建议</li>
                    <li>🫶 情绪支持</li>
                    <li>🔗 连续对话</li>
                  </ul>
                </div>
                <div class="card-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>

              <div class="feature-card manus" @click="goToManus">
                <div class="card-glow"></div>
                <div class="card-icon">
                  <span>🎯</span>
                </div>
                <div class="card-content">
                  <h3>恋爱教练 <span class="badge coach">Coach</span></h3>
                  <p>把对话发我，我帮你分析对方意图、给出更稳的回复策略</p>
                  <ul class="feature-list">
                    <li>🧠 语气与意图分析</li>
                    <li>✍️ 回复润色与改写</li>
                    <li>🗺️ 约会与相处建议</li>
                  </ul>
                </div>
                <div class="card-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>

            <!-- 恋爱亮点 -->
            <div class="tech-highlights">
              <div class="tech-item">
                <span class="tech-emoji">⚡</span>
                <span>快速给出建议</span>
              </div>
              <div class="tech-item">
                <span class="tech-emoji">💝</span>
                <span>更温和的表达</span>
              </div>
              <div class="tech-item">
                <span class="tech-emoji">🌟</span>
                <span>聊天不尴尬</span>
              </div>
              <div class="tech-item">
                <span class="tech-emoji">📈</span>
                <span>关系复盘成长</span>
              </div>
            </div>
          </div>
        </div>
      </el-main>

      <!-- 底部信息 -->
      <el-footer class="app-footer">
        <div class="footer-content">
          <p>💕 Copyright © {{ new Date().getFullYear() }} 恋爱大师</p>
          <p>专注恋爱沟通与关系成长</p>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { useRouter } from 'vue-router'
import { 
  ArrowRight, 
  Link 
} from '@element-plus/icons-vue'

const router = useRouter()
const isDarkTheme = inject('isDarkTheme')

const REPO_URL = 'https://github.com/KkOma-value/SpringAI_Learn'

const openRepo = () => {
  window.open(REPO_URL, '_blank', 'noopener,noreferrer')
}

// 导航到 Love App 页面
const goToLoveApp = () => {
  router.push('/loveapp')
}

// 导航到 Manus 页面
const goToManus = () => {
  router.push('/manus')
}
</script>

<style scoped>
.home-page {
  position: relative;
  z-index: 10;
  height: 100vh;
}

.main-container {
  height: 100vh;
}

.app-header {
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  color: var(--text-color);
  padding: 0 30px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 30px rgba(255, 20, 147, 0.2);
  border-bottom: 1px solid var(--border-color);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 32px;
  animation: heartbeat 1.5s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.logo-container h2 {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
  letter-spacing: 2px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.repo-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color)) !important;
  border: none !important;
}

.repo-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 0 20px var(--primary-color);
}

.app-main {
  padding: 40px 40px 20px;
  height: calc(100vh - 140px);
  overflow-y: auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.welcome-section {
  width: 100%;
  max-width: 1200px;
  text-align: center;
  position: relative;
}

.welcome-content {
  animation: fadeInUp 1s ease-out;
}

.floating-hearts {
  position: absolute;
  top: -80px;
  left: 0;
  right: 0;
  pointer-events: none;
}

.heart {
  position: absolute;
  font-size: 2rem;
  left: var(--x);
  animation: floatHeart 4s ease-in-out infinite;
  animation-delay: var(--delay);
  opacity: 0.7;
}

@keyframes floatHeart {
  0%, 100% { 
    transform: translateY(0) rotate(0deg);
    opacity: 0.7;
  }
  50% { 
    transform: translateY(-20px) rotate(10deg);
    opacity: 1;
  }
}

.welcome-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.3;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.title-emoji {
  font-size: 4rem;
  animation: heartbeat 1.5s ease-in-out infinite;
}

.title-text {
  color: var(--text-color);
}

.title-highlight {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color), #ffd700);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  background-size: 200% 200%;
  animation: gradientShift 3s ease infinite;
  filter: drop-shadow(0 0 30px var(--primary-color));
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.welcome-subtitle {
  font-size: 1.3rem;
  color: var(--text-color);
  opacity: 0.9;
  margin-bottom: 60px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 30px;
  margin-bottom: 60px;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.feature-card {
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 30px;
  box-shadow: 0 15px 40px var(--shadow-color);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.23, 1, 0.320, 1);
  display: flex;
  align-items: center;
  gap: 20px;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    transparent 30%,
    rgba(255, 105, 180, 0.1) 70%
  );
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}

.feature-card:hover .card-glow {
  opacity: 1;
}

.feature-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 
    0 25px 50px rgba(255, 20, 147, 0.3),
    0 0 60px rgba(255, 105, 180, 0.2);
  border-color: var(--primary-color);
}

.love-app {
  border-left: 4px solid var(--primary-color);
}

.manus {
  border-left: 4px solid var(--secondary-color);
}

.card-icon {
  font-size: 50px;
  flex-shrink: 0;
  filter: drop-shadow(0 0 15px var(--primary-color));
  transition: transform 0.3s;
}

.feature-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

.card-content {
  flex: 1;
}

.card-content h3 {
  font-size: 1.4rem;
  font-weight: 600;
  margin-bottom: 10px;
  color: var(--text-color);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.badge {
  font-size: 0.7rem;
  padding: 3px 10px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: 12px;
  color: white;
  font-weight: 500;
}

.badge.coach {
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
}

.card-content p {
  color: var(--text-color);
  opacity: 0.8;
  margin-bottom: 15px;
  line-height: 1.6;
  font-size: 0.95rem;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  color: var(--text-color);
  opacity: 0.7;
  font-size: 0.9rem;
  margin-bottom: 6px;
  line-height: 1.4;
}

.card-arrow {
  font-size: 24px;
  color: var(--text-color);
  opacity: 0.4;
  transition: all 0.4s;
}

.feature-card:hover .card-arrow {
  opacity: 1;
  transform: translateX(8px);
  color: var(--primary-color);
}

.tech-highlights {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.tech-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 18px 20px;
  background: var(--card-bg);
  backdrop-filter: blur(15px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 8px 25px var(--shadow-color);
  color: var(--text-color);
  font-weight: 500;
  transition: all 0.3s ease;
  font-size: 0.9rem;
}

.tech-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(255, 20, 147, 0.25);
  border-color: var(--primary-color);
  background: rgba(255, 105, 180, 0.1);
}

.tech-emoji {
  font-size: 1.3rem;
}

.app-footer {
  background: var(--footer-bg);
  backdrop-filter: blur(20px);
  text-align: center;
  color: var(--text-color);
  padding: 15px 0;
  height: 70px;
  border-top: 1px solid var(--border-color);
  box-shadow: 0 -4px 30px rgba(255, 20, 147, 0.1);
}

.footer-content {
  font-size: 14px;
  opacity: 0.8;
}

.footer-content p:first-child {
  margin-bottom: 5px;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .welcome-title {
    font-size: 2.5rem;
  }
  
  .title-emoji {
    font-size: 3rem;
  }
  
  .feature-cards {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .feature-card {
    flex-direction: column;
    text-align: center;
    padding: 25px;
  }
  
  .tech-highlights {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .app-main {
    padding: 30px 20px 15px;
  }
}

@media (max-width: 480px) {
  .welcome-title {
    font-size: 2rem;
    flex-direction: column;
    gap: 8px;
  }
  
  .welcome-subtitle {
    font-size: 1rem;
    margin-bottom: 40px;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .card-icon {
    font-size: 40px;
  }
  
  .tech-highlights {
    grid-template-columns: 1fr;
  }
}
</style>