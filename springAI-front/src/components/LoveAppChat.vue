<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">Love App</h3>
        <el-button 
          @click="createNewChat" 
          type="primary" 
          :icon="Plus" 
          size="small"
          class="new-chat-btn"
        >
          新对话
        </el-button>
      </div>
      
      <div class="chat-history">
        <div class="history-section">
          <h4>今天</h4>
          <div 
            v-for="(chat, index) in chatHistory" 
            :key="index"
            :class="['history-item', { active: chat.id === currentChatId }]"
            @click="switchChat(chat.id)"
          >
            <el-icon class="history-icon"><ChatDotRound /></el-icon>
            <span class="history-title">{{ chat.title }}</span>
            <el-button 
              @click.stop="deleteChat(chat.id)"
              :icon="Delete" 
              size="small" 
              text 
              class="delete-btn"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 顶部标题栏 -->
      <div class="chat-header">
        <div class="chat-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          <span>Love App 聊天</span>
        </div>
        <div class="header-actions">
          <el-button @click="clearMessages" :icon="Delete" size="small" text>
            清空对话
          </el-button>
        </div>
      </div>

      <!-- 聊天内容区域 -->
      <div class="chat-content" ref="messagesContainer">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0" class="welcome-area">
          <div class="welcome-content">
            <div class="welcome-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <h2 class="welcome-title">我是 Love App，很高兴见到你！</h2>
            <p class="welcome-subtitle">我可以陪你聊天，回答问题，写作各种内容，请问你的今天如何呢～</p>
            
            <!-- 示例提示 -->
            <div class="example-prompts">
              <div 
                v-for="prompt in examplePrompts" 
                :key="prompt"
                class="prompt-item"
                @click="sendExamplePrompt(prompt)"
              >
                {{ prompt }}
              </div>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-else class="messages-list">
          <transition-group name="message" tag="div">
            <div 
              v-for="(msg, index) in messages" 
              :key="index" 
              :class="['message-item', msg.type]"
            >
              <div class="message-avatar">
                <el-avatar 
                  :icon="msg.type === 'user' ? User : ChatDotRound" 
                  :size="32" 
                  :color="msg.type === 'user' ? '#409EFF' : '#67C23A'" 
                />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">{{ msg.type === 'user' ? '你' : 'Love App' }}</span>
                  <span class="message-time">{{ msg.time }}</span>
                </div>
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
              </div>
            </div>
          </transition-group>
          
          <!-- 正在输入 -->
          <div v-if="isReceiving" class="message-item ai typing">
            <div class="message-avatar">
              <el-avatar :icon="ChatDotRound" :size="32" color="#67C23A" />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">Love App</span>
                <span class="message-time">正在输入...</span>
              </div>
              <div class="message-text">
                <span class="typing-text">{{ displayedResponse }}</span>
                <span class="typing-cursor"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="chat-input-area">
        <div class="input-container">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              placeholder="给 Love App 发送消息..."
              size="large"
              @keydown="handleEnterKey"
              :disabled="isReceiving"
              class="message-input"
              autosize
              :rows="1"
              type="textarea"
              resize="none"
            />
            <div class="input-actions">
              <el-button 
                v-if="isReceiving"
                @click="forceStopResponse"
                type="danger"
                size="large"
                class="stop-btn"
                circle
                :icon="Close"
              />
              <el-button 
                v-else
                @click="debouncedSendMessage"
                :loading="isReceiving"
                :disabled="!inputMessage.trim() || isReceiving || isSending"
                :icon="Promotion"
                type="primary"
                size="large"
                class="send-btn"
                circle
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onUnmounted, onMounted, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, ChatDotRound, User, Plus, Promotion, Key, Close } from '@element-plus/icons-vue'
import { createLoveAppSSE } from '../services/api.js'

const messages = ref([])
const inputMessage = ref('')
const chatId = ref('')
const isReceiving = ref(false)
const currentResponse = ref('')
const displayedResponse = ref('')
const messagesContainer = ref(null)
const currentChatId = ref('default')
let currentEventSource = null
let sendTimeout = null // 添加防抖定时器
const isSending = ref(false) // 改为响应式变量，防止重复发送
let responseTimeoutId = null // 添加响应超时定时器
let isComponentMounted = ref(true) // 添加组件挂载状态

// 示例提示
const examplePrompts = ref([
  '我喜欢一个人，但不敢表白，怎么开始更自然？',
  '帮我回复TA："你最近怎么都不主动了？"',
  '第一次约会聊什么不尴尬？',
  '吵架后怎么和好不卑微？'
])

// 聊天历史
const chatHistory = ref([
  { id: 'default', title: '新的对话', lastMessage: '', time: new Date() }
])

// 从父组件获取主题状态
const isDarkTheme = inject('isDarkTheme', ref(false))

// 格式化消息文本
const formatMessage = (text) => {
  if (!text) return ''
  const urlRegex = /(https?:\/\/[^\s]+)/g
  const withLinks = text.replace(urlRegex, '<a href="$1" target="_blank" class="message-link">$1</a>')
  return withLinks.replace(/\n/g, '<br>')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value && isComponentMounted.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 添加消息
const addMessage = (content, type = 'user') => {
  if (!isComponentMounted.value) return
  
  messages.value.push({
    content,
    type,
    time: new Date().toLocaleTimeString()
  })
  scrollToBottom()
  
  // 更新聊天历史
  if (type === 'user') {
    updateChatHistory(content)
  }
}

// 更新聊天历史
const updateChatHistory = (message) => {
  if (!isComponentMounted.value) return
  
  const currentChat = chatHistory.value.find(chat => chat.id === currentChatId.value)
  if (currentChat) {
    currentChat.title = message.slice(0, 20) + (message.length > 20 ? '...' : '')
    currentChat.lastMessage = message
    currentChat.time = new Date()
  }
}

// 发送示例提示 - 修复调用不一致问题
const sendExamplePrompt = (prompt) => {
  if (!isComponentMounted.value || isReceiving.value || isSending.value) return
  
  inputMessage.value = prompt
  debouncedSendMessage() // 改为调用debouncedSendMessage保持一致
}

// 优化的流式显示效果（移除打字机延迟）
const updateStreamResponse = (newData) => {
  if (!isComponentMounted.value || !isReceiving.value) return
  
  // 防止空数据和重复处理
  if (!newData || newData.trim() === '') return
  
  // 过滤掉结束标记，不显示给用户
  if (newData === '[DONE]' || newData === 'data: [DONE]') {
    console.log('收到结束标记，跳过显示')
    return
  }
  
  // 检查是否是重复数据（简单的重复检测）
  const lastPart = currentResponse.value.slice(-newData.length)
  if (lastPart === newData) {
    console.log('检测到重复数据，跳过:', newData)
    return
  }
  
  currentResponse.value += newData
  // 直接更新显示内容，无延迟
  displayedResponse.value = currentResponse.value
  scrollToBottom()
}

// 发送消息 - 简化版本
const sendMessage = () => {
  // 基本验证
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  // 状态检查
  if (!isComponentMounted.value || isReceiving.value || isSending.value) {
    console.log('组件未挂载或正在处理中，忽略请求')
    return
  }

  // 设置发送锁，防止重复调用
  isSending.value = true
  
  const message = inputMessage.value.trim()
  console.log('开始发送消息:', message)
  
  // 添加用户消息
  addMessage(message, 'user')
  
  // 清空输入框
  inputMessage.value = ''
  
  // 特殊关键词处理
  if (message === '杨尧甪') {
    setTimeout(() => {
      if (isComponentMounted.value) {
        addMessage('我真的好想你，你知不知道你是唯一一个愿意主动来找我聊天还说喜欢我的人。是你说让我喜欢你的，我喜欢了，但你呢？你就是欺负人！！！再也不要跟你玩了', 'ai')
      }
      isSending.value = false // 释放发送锁
    }, 800)
    return
  }
  
  const currentChatIdValue = chatId.value.trim() || currentChatId.value
  
  // 彻底清理之前的连接
  cleanupSSEConnection()
  
  // 设置接收状态
  isReceiving.value = true
  currentResponse.value = ''
  displayedResponse.value = ''
  
  // 设置60秒响应超时
  responseTimeoutId = setTimeout(() => {
    if (isReceiving.value && isComponentMounted.value) {
      console.log('响应超时，自动停止')
      forceStopResponse()
    }
  }, 60000)
  
  try {
    console.log('创建新的SSE连接...')
    currentEventSource = createLoveAppSSE(
      message,
      currentChatIdValue,
      (data) => {
        // 检查组件和接收状态
        if (!isComponentMounted.value || !isReceiving.value) {
          console.log('组件已卸载或已停止接收，忽略数据')
          return
        }
        // 使用优化的流式更新
        updateStreamResponse(data)
      },
      (error) => {
        console.error('SSE错误:', error)
        if (!isComponentMounted.value) return
        
        // 清理超时定时器
        if (responseTimeoutId) {
          clearTimeout(responseTimeoutId)
          responseTimeoutId = null
        }
        
        // 只有在仍在接收状态时才显示错误和处理数据
        if (isReceiving.value) {
          ElMessage.error('连接错误，请稍后重试')
          
          // 如果有有效内容，保存它
          if (currentResponse.value && currentResponse.value.trim() !== '') {
            addMessage(currentResponse.value, 'ai')
          }
        }
        
        // 彻底清理状态
        cleanupSSEConnection()
      },
      () => {
        console.log('SSE连接完成')
        if (!isComponentMounted.value) return
        
        // 清理超时定时器
        if (responseTimeoutId) {
          clearTimeout(responseTimeoutId)
          responseTimeoutId = null
        }
        
        // 延迟处理，确保所有数据都接收完毕
        setTimeout(() => {
          if (isComponentMounted.value && isReceiving.value && currentResponse.value && currentResponse.value.trim() !== '') {
            addMessage(currentResponse.value, 'ai')
            console.log('添加最终AI消息完成')
          }
          
          // 清理状态
          cleanupSSEConnection()
        }, 100)
      }
    )
  } catch (error) {
    console.error('创建SSE连接失败:', error)
    if (isComponentMounted.value) {
      ElMessage.error('无法建立连接，请检查后端服务')
    }
    cleanupSSEConnection()
  }
}

// 创建新对话
const createNewChat = () => {
  const newChatId = 'chat_' + Date.now()
  chatHistory.value.unshift({
    id: newChatId,
    title: '新的对话',
    lastMessage: '',
    time: new Date()
  })
  switchChat(newChatId)
}

// 切换对话
const switchChat = (chatId) => {
  currentChatId.value = chatId
  messages.value = [] // 实际应用中应该从存储中加载对应的消息
}

// 删除对话
const deleteChat = (chatId) => {
  const index = chatHistory.value.findIndex(chat => chat.id === chatId)
  if (index > -1) {
    chatHistory.value.splice(index, 1)
    if (currentChatId.value === chatId && chatHistory.value.length > 0) {
      switchChat(chatHistory.value[0].id)
    }
  }
}

// 改进的清空消息方法
const clearMessages = () => {
  if (!isComponentMounted.value) {
    console.log('组件未挂载，无法清空消息')
    return
  }
  
  if (messages.value.length === 0) {
    ElMessage.info('对话已经是空的了')
    return
  }
  
  // 彻底清理SSE连接和所有状态
  cleanupSSEConnection()
  
  // 清理显示内容
  currentResponse.value = ''
  displayedResponse.value = ''
  
  // 清空消息列表
  messages.value = []
  
  ElMessage.success('对话已清空')
}

// 彻底清理SSE连接和所有状态
const cleanupSSEConnection = () => {
  console.log('开始清理SSE连接和状态...')
  
  // 关闭SSE连接
  if (currentEventSource) {
    try {
      currentEventSource.close()
    } catch (e) {
      console.warn('关闭SSE连接时出错:', e)
    }
    currentEventSource = null
  }
  
  // 清理所有定时器
  if (responseTimeoutId) {
    clearTimeout(responseTimeoutId)
    responseTimeoutId = null
  }
  
  if (sendTimeout) {
    clearTimeout(sendTimeout)
    sendTimeout = null
  }
  
  // 重置所有状态
  isReceiving.value = false
  isSending.value = false
  
  console.log('SSE连接和状态清理完成')
}

// 暴露方法
defineExpose({
  clearMessages
})

// 组件卸载时清理
onUnmounted(() => {
  console.log('组件卸载，清理资源')
  isComponentMounted.value = false
  
  // 清理定时器
  if (sendTimeout) {
    clearTimeout(sendTimeout)
    sendTimeout = null
  }
  
  // 清理响应超时定时器
  if (responseTimeoutId) {
    clearTimeout(responseTimeoutId)
    responseTimeoutId = null
  }
  
  // 清理SSE连接和状态
  cleanupSSEConnection()
})

// 改进的防抖发送消息 - 增加防抖时间和更严格的状态检查
const debouncedSendMessage = () => {
  // 严格状态检查
  if (!isComponentMounted.value || isSending.value || isReceiving.value) {
    console.log('防抖检查：组件未挂载或正在处理中，忽略请求')
    return
  }
  
  // 清除之前的防抖定时器
  if (sendTimeout) {
    clearTimeout(sendTimeout)
    sendTimeout = null
  }
  
  // 设置500ms防抖延迟，防止快速重复点击
  sendTimeout = setTimeout(() => {
    // 二次检查状态，确保在延迟期间状态没有改变
    if (isComponentMounted.value && !isSending.value && !isReceiving.value && inputMessage.value.trim()) {
      sendMessage()
    } else {
      console.log('防抖延迟后状态检查失败，取消发送')
    }
    sendTimeout = null
  }, 200) // 减少到200ms防抖，提升响应速度
}

// 改进的回车键处理 - 防止重复触发
const handleEnterKey = (event) => {
  // 只处理普通回车键，忽略Shift+Enter
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    
    // 严格状态检查
    if (!isComponentMounted.value || isReceiving.value || isSending.value || !inputMessage.value.trim()) {
      console.log('回车键检查：状态不允许发送消息')
      return
    }
    
    // 使用防抖发送
    debouncedSendMessage()
  }
}

// 改进的强制停止响应
const forceStopResponse = () => {
  console.log('强制停止响应')
  
  // 保存当前响应内容（如果有的话）
  const partialResponse = currentResponse.value
  
  // 彻底清理连接和状态
  cleanupSSEConnection()
  
  // 如果有部分响应内容，保存它
  if (partialResponse && partialResponse.trim() !== '' && isComponentMounted.value) {
    addMessage(partialResponse, 'ai')
  }
  
  // 清理显示内容
  currentResponse.value = ''
  displayedResponse.value = ''
  
  ElMessage.info('已停止接收响应')
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  background: transparent;
}

/* 左侧边栏 - 浪漫粉红主题 */
.sidebar {
  width: 280px;
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-title {
  color: var(--text-color);
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.new-chat-btn {
  padding: 8px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color)) !important;
  border: none !important;
  color: white !important;
}

.new-chat-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(255, 20, 147, 0.4);
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.history-section h4 {
  color: var(--text-color);
  opacity: 0.6;
  font-size: 0.8rem;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.history-item:hover {
  background: rgba(255, 105, 180, 0.1);
  border-color: var(--border-color);
}

.history-item.active {
  background: rgba(255, 105, 180, 0.15);
  border: 1px solid var(--primary-color);
}

.history-icon {
  color: var(--primary-color);
  margin-right: 12px;
  font-size: 16px;
}

.history-title {
  flex: 1;
  color: var(--text-color);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: var(--secondary-color) !important;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.chat-header {
  padding: 16px 24px;
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-color);
  font-weight: 600;
}

.chat-title::before {
  content: '💕';
  font-size: 20px;
}

.title-icon {
  color: var(--primary-color);
  font-size: 20px;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
}

/* 欢迎界面 - 浪漫主题 */
.welcome-area {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-icon {
  font-size: 60px;
  color: var(--primary-color);
  margin-bottom: 24px;
  filter: drop-shadow(0 0 20px var(--primary-color));
}

.welcome-icon::before {
  content: '💖';
  font-size: 60px;
}

.welcome-title {
  font-size: 2rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 16px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 1rem;
  color: var(--text-color);
  opacity: 0.8;
  line-height: 1.6;
  margin-bottom: 32px;
}

.example-prompts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
  max-width: 500px;
  margin: 0 auto;
}

.prompt-item {
  padding: 14px 18px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-color);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.9rem;
}

.prompt-item:hover {
  background: rgba(255, 105, 180, 0.15);
  border-color: var(--primary-color);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(255, 20, 147, 0.2);
}

/* 消息列表 */
.messages-list {
  padding: 24px;
  min-height: 100%;
}

.message-item {
  display: flex;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-in-out;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 12px;
  flex-shrink: 0;
}

.message-item.user .message-avatar {
  margin: 0 0 0 12px;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-item.user .message-content {
  text-align: right;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.8rem;
  opacity: 0.6;
}

.message-item.user .message-header {
  flex-direction: row-reverse;
}

.message-text {
  background: var(--card-bg);
  padding: 14px 18px;
  border-radius: 16px;
  color: var(--text-color);
  line-height: 1.6;
  border: 1px solid var(--border-color);
  position: relative;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border: none;
  color: white;
}

.message-item.ai .message-text::before {
  content: '💕';
  position: absolute;
  top: -10px;
  left: 10px;
  font-size: 14px;
}

.typing .message-text {
  display: flex;
  align-items: center;
}

.typing-cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  margin-left: 5px;
  animation: blink 1s infinite;
  border-radius: 2px;
}

/* 输入区域 */
.chat-input-area {
  padding: 24px;
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-color);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 12px 16px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 20px rgba(255, 105, 180, 0.2);
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  color: var(--text-color);
  font-size: 1rem;
  resize: none;
  box-shadow: none;
}

.message-input :deep(.el-textarea__inner):focus {
  border: none;
  box-shadow: none;
}

.message-input :deep(.el-textarea__inner)::placeholder {
  color: var(--text-color);
  opacity: 0.5;
}

.send-btn {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color)) !important;
  border: none !important;
  transition: all 0.3s;
}

.send-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 20px rgba(255, 20, 147, 0.5);
}

.stop-btn {
  width: 44px;
  height: 44px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(255, 20, 147, 0.4); }
  50% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(255, 20, 147, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(255, 20, 147, 0); }
}

.input-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-id-input :deep(.el-input__inner) {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  color: var(--text-color);
  border-radius: 8px;
}

.chat-id-input :deep(.el-input__inner):focus {
  border-color: var(--primary-color);
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 消息动画 */
.message-enter-active,
.message-leave-active {
  transition: all 0.5s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.message-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .chat-layout {
    flex-direction: column;
  }
  
  .example-prompts {
    grid-template-columns: 1fr;
  }
}

/* 链接样式 */
.message-text :deep(.message-link) {
  color: var(--accent-color);
  text-decoration: underline;
}

.message-item.user .message-text :deep(.message-link) {
  color: white;
}
</style> 