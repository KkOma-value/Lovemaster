<template>
  <div class="chat-page">
    <div class="back-button-container">
      <el-button @click="goBack" :icon="ArrowLeft" type="success" plain size="small" class="back-btn">
        💕 返回首页
      </el-button>
    </div>
    <ManusChat ref="chatComponent" />
  </div>
</template>

<script setup>
import { inject, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import ManusChat from '../components/ManusChat.vue'

const router = useRouter()
const chatComponent = ref(null)

const goBack = () => {
  router.push('/')
}

// 暴露清空聊天方法
defineExpose({
  clearMessages: () => {
    if (chatComponent.value) {
      chatComponent.value.clearMessages()
    }
  }
})
</script>

<style scoped>
.chat-page {
  position: relative;
  z-index: 10;
  height: 100vh;
}

.back-button-container {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 20;
}

.back-btn {
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b) !important;
  border: none !important;
  color: white !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
  font-weight: 500;
}

.back-btn:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 25px rgba(255, 20, 147, 0.4);
}
</style>