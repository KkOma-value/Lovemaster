<template>
  <div id="app" :class="{ 'dark-theme': isDarkTheme }">
    <!-- Three.js 3D背景 -->
    <ThreeBackground />
    
    <!-- 渐变遮罩层 -->
    <div class="gradient-overlay"></div>
    
    <router-view />
  </div>
</template>

<script setup>
import { ref, provide, onMounted } from 'vue'
import ThreeBackground from './components/ThreeBackground.vue'

const isDarkTheme = ref(true) // 默认使用暗色主题

// 提供给子组件使用的主题状态
provide('isDarkTheme', isDarkTheme)
provide('toggleTheme', () => {
  isDarkTheme.value = !isDarkTheme.value
})
</script>

<style>
:root {
  --primary-color: #ff69b4;     /* 粉红 */
  --secondary-color: #ff1493;   /* 玫红 */
  --accent-color: #ffb6c1;      /* 浅粉 */
  --background-color: #1a0a10;  /* 深玫红黑 */
  --text-color: #fff0f5;        /* 淡粉白 */
  --card-bg: rgba(45, 20, 30, 0.9);
  --border-color: rgba(255, 105, 180, 0.2);
  --shadow-color: rgba(255, 20, 147, 0.2);
  --message-user-bg: #ff69b4;
  --message-ai-bg: rgba(45, 20, 30, 0.9);
  --message-ai-border: rgba(255, 105, 180, 0.3);
  --header-bg: rgba(30, 10, 20, 0.95);
  --footer-bg: rgba(30, 10, 20, 0.95);
}

.dark-theme {
  --primary-color: #ff69b4;
  --secondary-color: #ff1493;
  --accent-color: #ffb6c1;
  --background-color: #0d0508;
  --text-color: #fff0f5;
  --card-bg: rgba(35, 15, 25, 0.9);
  --border-color: rgba(255, 105, 180, 0.15);
  --shadow-color: rgba(255, 20, 147, 0.3);
  --message-user-bg: #ff1493;
  --message-ai-bg: rgba(35, 15, 25, 0.9);
  --message-ai-border: rgba(255, 105, 180, 0.25);
  --header-bg: rgba(20, 8, 15, 0.95);
  --footer-bg: rgba(20, 8, 15, 0.95);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  transition: background-color 0.3s, color 0.3s, border-color 0.3s;
}

html, body {
  height: 100%;
  overflow: hidden;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  color: var(--text-color);
  background: linear-gradient(135deg, #1a0a10 0%, #2d0a1a 50%, #0d0508 100%);
  height: 100vh;
  overflow: hidden;
  position: relative;
}

.dark-theme #app {
  background: linear-gradient(135deg, #0d0508 0%, #1a0a10 50%, #0d0508 100%);
}

.gradient-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(
    ellipse at 50% 50%,
    transparent 0%,
    rgba(255, 20, 147, 0.05) 50%,
    rgba(0, 0, 0, 0.3) 100%
  );
  pointer-events: none;
  z-index: 1;
}

/* 全局链接样式 */
a {
  color: var(--primary-color);
  text-decoration: none;
  transition: color 0.3s, text-shadow 0.3s;
}

a:hover {
  color: var(--secondary-color);
  text-shadow: 0 0 10px var(--primary-color);
}

/* 全局滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--primary-color), var(--secondary-color));
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--secondary-color);
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 选中文本样式 */
::selection {
  background: var(--primary-color);
  color: white;
}
</style>