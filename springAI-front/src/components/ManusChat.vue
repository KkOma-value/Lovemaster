<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">恋爱教练</h3>
        <el-button 
          @click="createNewChat" 
          type="success" 
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
            <el-icon class="history-icon"><ChatLineRound /></el-icon>
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
          <el-icon class="title-icon"><ChatLineRound /></el-icon>
          <span>恋爱教练模式</span>
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
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <h2 class="welcome-title">我是恋爱教练，很高兴见到你！</h2>
            <p class="welcome-subtitle">把你们的对话或困扰发我，我帮你分析并给出更自然的回复建议</p>
            
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
                  :icon="msg.type === 'user' ? User : ChatLineRound" 
                  :size="32" 
                  :color="msg.type === 'user' ? '#67C23A' : '#409EFF'" 
                />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">{{ msg.type === 'user' ? '你' : '恋爱教练' }}</span>
                  <span class="message-time">{{ msg.time }}</span>
                </div>
                <div class="message-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
              </div>
            </div>
          </transition-group>
          
          <!-- 思考中/正在输入 -->
          <div v-if="isReceiving" class="message-item ai typing">
            <div class="message-avatar">
              <el-avatar :icon="ChatLineRound" :size="32" color="#409EFF" />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">恋爱教练</span>
                <span class="message-time">{{ currentStatus || '正在思考...' }}</span>
              </div>
              <!-- 思考气泡动画 -->
              <div v-if="isThinking && !displayedResponse" class="thinking-bubble">
                <div class="thinking-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
                <span class="thinking-text">{{ currentStatus }}</span>
              </div>
              <!-- 实际内容 -->
              <div v-else class="message-text markdown-body">
                <span v-html="renderedDisplayedResponse"></span>
                <span v-if="isReceiving" class="typing-cursor"></span>
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
              placeholder="给恋爱教练发送消息..."
              size="large"
              @keyup.enter="handleEnterKey"
              :disabled="isReceiving"
              class="message-input"
              autosize
              :rows="1"
              type="textarea"
              resize="none"
            />
            <div class="input-actions">
              <el-button 
                @click="debouncedSendMessage"
                :loading="isReceiving"
                :disabled="!inputMessage.trim() || isReceiving || isSending"
                :icon="Promotion"
                type="success"
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
import { Delete, ChatLineRound, User, Plus, Promotion } from '@element-plus/icons-vue'
import { createManusSSE } from '../services/api.js'
import { marked } from 'marked'

const messages = ref([])
const inputMessage = ref('')
const isReceiving = ref(false)
const currentResponse = ref('')
const displayedResponse = ref('')
const messagesContainer = ref(null)
const currentChatId = ref('default')
let currentEventSource = null
let typingTimer = null
let typingIndex = 0
const typingSpeed = 20  // 加快打字速度
const isSending = ref(false)
let sendTimeout = null
let isComponentMounted = ref(true)

// 新增状态变量
const isThinking = ref(false)  // 是否正在思考
const currentStatus = ref('')  // 当前状态消息

// 配置 marked 选项
marked.setOptions({
  breaks: true,  // 支持换行
  gfm: true,     // 支持 GitHub Flavored Markdown
})

// Markdown 渲染函数
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    return marked(text)
  } catch (e) {
    console.warn('Markdown渲染失败:', e)
    return text.replace(/\n/g, '<br>')
  }
}

// 渲染显示的响应（用于打字机效果）
const renderedDisplayedResponse = computed(() => {
  return renderMarkdown(displayedResponse.value)
})

// 示例提示
const examplePrompts = ref([
  '帮我回复TA："我觉得你变了"',
  'TA已读不回，我要不要再发一条？怎么发更合适？',
  '我们最近总吵架，问题可能出在哪？',
  '第一次约会结束后，怎么跟进更自然？'
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

// 清理所有状态和连接
const cleanupManusConnection = () => {
  console.log('清理Manus连接和状态')
  
  // 关闭SSE连接
  if (currentEventSource) {
    try {
      currentEventSource.close()
    } catch (e) {
      console.warn('关闭Manus SSE连接时出错:', e)
    }
    currentEventSource = null
  }
  
  // 清理定时器
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }
  
  if (sendTimeout) {
    clearTimeout(sendTimeout)
    sendTimeout = null
  }
  
  // 重置状态
  isReceiving.value = false
  isSending.value = false
  isThinking.value = false
  currentStatus.value = ''
}

// 打字机效果 - 增量式，不重新开始
const startTypingEffect = () => {
  if (!isComponentMounted.value) return
  
  // 如果已经在运行，不重新创建
  if (typingTimer) return
  
  typingTimer = setInterval(() => {
    if (!isComponentMounted.value) {
      clearInterval(typingTimer)
      typingTimer = null
      return
    }
    
    // 检查是否还有内容需要显示
    if (typingIndex < currentResponse.value.length) {
      displayedResponse.value += currentResponse.value.charAt(typingIndex)
      typingIndex++
      scrollToBottom()
    } else if (!isReceiving.value) {
      // 如果已经完成接收且显示完毕，停止定时器
      clearInterval(typingTimer)
      typingTimer = null
    }
  }, typingSpeed)
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

// 发送示例提示
const sendExamplePrompt = (prompt) => {
  if (!isComponentMounted.value || isReceiving.value || isSending.value) return
  
  inputMessage.value = prompt
  debouncedSendMessage()
}

// 防抖发送消息
const debouncedSendMessage = () => {
  // 状态检查
  if (!isComponentMounted.value || isSending.value || isReceiving.value) {
    console.log('Manus防抖检查：组件未挂载或正在处理中，忽略请求')
    return
  }
  
  // 清除之前的防抖定时器
  if (sendTimeout) {
    clearTimeout(sendTimeout)
    sendTimeout = null
  }
  
  // 设置200ms防抖延迟
  sendTimeout = setTimeout(() => {
    // 二次检查状态
    if (isComponentMounted.value && !isSending.value && !isReceiving.value && inputMessage.value.trim()) {
      sendMessage()
    } else {
      console.log('Manus防抖延迟后状态检查失败，取消发送')
    }
    sendTimeout = null
  }, 200)
}

// 处理回车键
const handleEnterKey = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    
    // 状态检查
    if (!isComponentMounted.value || isReceiving.value || isSending.value || !inputMessage.value.trim()) {
      console.log('Manus回车键检查：状态不允许发送消息')
      return
    }
    
    // 使用防抖发送
    debouncedSendMessage()
  }
}

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  // 状态检查
  if (!isComponentMounted.value || isReceiving.value || isSending.value) {
    console.log('Manus发送检查：组件未挂载或正在处理中，忽略请求')
    return
  }

  // 设置发送锁
  isSending.value = true

  const message = inputMessage.value.trim()
  console.log('开始发送Manus消息:', message)
  
  addMessage(message, 'user')
  inputMessage.value = ''
  
  // 特殊关键词处理
  if (message === '杨尧甸') {
    setTimeout(() => {
      if (isComponentMounted.value) {
        addMessage('我真的好想你，你知不知道你是唯一一个愿意主动来找我聊天还说喜欢我的人。是你说让我喜欢你的，我喜欢了，但你呢？你就是欺负人！！！再也不要跟你玩了', 'ai')
      }
      isSending.value = false
    }, 800)
    return
  }
  
  // 清理之前的连接
  cleanupManusConnection()
  
  isReceiving.value = true
  isThinking.value = true
  currentStatus.value = '正在思考中...'
  currentResponse.value = ''
  displayedResponse.value = ''
  typingIndex = 0
  
  try {
    currentEventSource = createManusSSE(
      message,
      (data) => {
        // 检查组件和接收状态
        if (!isComponentMounted.value || !isReceiving.value) {
          console.log('Manus组件已卸载或已停止接收，忽略数据')
          return
        }
        
        // 防止空数据
        if (!data || data.trim() === '') return
        
        // 过滤掉结束标记
        if (data === '[DONE]' || data === 'data: [DONE]') {
          console.log('收到Manus结束标记，跳过显示')
          return
        }
        
        // 尝试解析JSON格式
        try {
          const parsed = JSON.parse(data)
          console.log('解析SSE数据:', parsed)
          
          switch (parsed.type) {
            case 'thinking':
              isThinking.value = true
              currentStatus.value = parsed.content || '正在思考中...'
              break
              
            case 'status':
              currentStatus.value = parsed.content || '处理中...'
              break
              
            case 'content':
              // 收到内容时，停止思考状态
              isThinking.value = false
              currentStatus.value = '正在输入...'
              
              // 追加内容（不重置）
              if (parsed.content) {
                currentResponse.value += parsed.content
                startTypingEffect()
              }
              break
              
            case 'done':
              console.log('收到完成标记')
              break
              
            case 'error':
              ElMessage.error(parsed.content || '发生错误')
              break
              
            default:
              console.log('未知消息类型:', parsed.type)
          }
        } catch (parseError) {
          // 如果不是JSON，尝试用旧的方式处理（兼容性）
          console.log('非JSON数据，使用旧方式处理:', data)
          
          // 过滤掉工具调用的技术信息
          if (data.includes('工具 ') && data.includes('完成了它的任务')) {
            return
          }
          if (data.startsWith('Step ')) {
            // 提取Step后面的内容
            const colonIndex = data.indexOf(': ')
            if (colonIndex !== -1) {
              data = data.substring(colonIndex + 2)
            }
          }
          if (data === '思考完成 - 无需行动' || data === '没有工具调用') {
            return
          }
          if (data.startsWith('未检测到工具调用')) {
            return
          }
          
          // 有效内容，追加到响应
          isThinking.value = false
          currentStatus.value = '正在输入...'
          currentResponse.value += data
          startTypingEffect()
        }
      },
      (error) => {
        console.error('Manus SSE错误:', error)
        if (!isComponentMounted.value) return
        
        ElMessage.error('连接错误，请检查后端服务是否正常')
        
        if (currentResponse.value && currentResponse.value.trim() !== '') {
          addMessage(currentResponse.value, 'ai')
        }
        
        cleanupManusConnection()
      },
      () => {
        console.log('Manus SSE连接完成')
        if (!isComponentMounted.value) return
        
        // 等待打字机效果完成
        const waitForTyping = () => {
          if (typingIndex >= currentResponse.value.length) {
            if (isComponentMounted.value && currentResponse.value && currentResponse.value.trim() !== '') {
              addMessage(currentResponse.value, 'ai')
            }
            cleanupManusConnection()
          } else {
            setTimeout(waitForTyping, 100)
          }
        }
        
        setTimeout(waitForTyping, 300)
      }
    )
  } catch (error) {
    console.error('创建Manus SSE连接失败:', error)
    if (isComponentMounted.value) {
      ElMessage.error('无法建立连接，请检查后端服务')
    }
    cleanupManusConnection()
  }
}

// 创建新对话
const createNewChat = () => {
  if (!isComponentMounted.value) return
  
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
  if (!isComponentMounted.value) return
  
  // 清理当前连接
  cleanupManusConnection()
  
  currentChatId.value = chatId
  messages.value = [] // 实际应用中应该从存储中加载对应的消息
}

// 删除对话
const deleteChat = (chatId) => {
  if (!isComponentMounted.value) return
  
  const index = chatHistory.value.findIndex(chat => chat.id === chatId)
  if (index > -1) {
    chatHistory.value.splice(index, 1)
    if (currentChatId.value === chatId && chatHistory.value.length > 0) {
      switchChat(chatHistory.value[0].id)
    }
  }
}

// 清空消息
const clearMessages = () => {
  if (!isComponentMounted.value) {
    console.log('Manus组件未挂载，无法清空消息')
    return
  }
  
  if (messages.value.length === 0) {
    ElMessage.info('对话已经是空的了')
    return
  }
  
  // 清理连接和状态
  cleanupManusConnection()
  
  // 清理显示内容
  currentResponse.value = ''
  displayedResponse.value = ''
  
  // 清空消息列表
  messages.value = []
  
  ElMessage.success('对话已清空')
}

// 暴露方法
defineExpose({
  clearMessages
})

// 组件卸载时清理
onUnmounted(() => {
  console.log('Manus组件卸载，清理资源')
  isComponentMounted.value = false
  
  // 清理连接和状态
  cleanupManusConnection()
})
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
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.new-chat-btn {
  padding: 8px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b) !important;
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
  background: rgba(255, 20, 147, 0.15);
  border: 1px solid var(--secondary-color);
}

.history-icon {
  color: var(--secondary-color);
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
  content: '🎯';
  font-size: 20px;
}

.title-icon {
  color: var(--secondary-color);
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
  color: var(--secondary-color);
  margin-bottom: 24px;
  filter: drop-shadow(0 0 20px var(--secondary-color));
}

.welcome-icon::before {
  content: '💘';
  font-size: 60px;
}

.welcome-title {
  font-size: 2rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 16px;
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
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
  background: rgba(255, 20, 147, 0.15);
  border-color: var(--secondary-color);
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
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
  border: none;
  color: white;
}

.message-item.ai .message-text::before {
  content: '🎯';
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
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
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
  border-color: var(--secondary-color);
  box-shadow: 0 0 20px rgba(255, 20, 147, 0.2);
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
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b) !important;
  border: none !important;
  transition: all 0.3s;
}

.send-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 20px rgba(255, 20, 147, 0.5);
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

/* 思考气泡样式 */
.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  animation: pulse 2s ease-in-out infinite;
}

.thinking-dots {
  display: flex;
  gap: 4px;
}

.thinking-dots .dot {
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
  border-radius: 50%;
  animation: wave 1.4s ease-in-out infinite;
}

.thinking-dots .dot:nth-child(1) {
  animation-delay: 0s;
}

.thinking-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}

.thinking-text {
  color: var(--text-color);
  opacity: 0.8;
  font-size: 0.9rem;
}

@keyframes wave {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(255, 20, 147, 0.2);
  }
  50% {
    box-shadow: 0 0 15px 5px rgba(255, 20, 147, 0.1);
  }
}

/* Markdown 渲染样式 */
.markdown-body {
  line-height: 1.7;
  word-wrap: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  color: var(--text-color);
}

.markdown-body :deep(h1) { font-size: 1.5em; }
.markdown-body :deep(h2) { font-size: 1.3em; }
.markdown-body :deep(h3) { font-size: 1.15em; }
.markdown-body :deep(h4) { font-size: 1em; }

.markdown-body :deep(p) {
  margin: 0.5em 0;
}

.markdown-body :deep(strong) {
  font-weight: 600;
  color: var(--secondary-color);
}

.markdown-body :deep(em) {
  font-style: italic;
  opacity: 0.9;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-body :deep(li) {
  margin: 0.3em 0;
}

.markdown-body :deep(blockquote) {
  margin: 0.8em 0;
  padding: 12px 16px;
  border-left: 4px solid var(--secondary-color);
  background: rgba(255, 20, 147, 0.08);
  border-radius: 0 8px 8px 0;
  font-style: italic;
}

.markdown-body :deep(code) {
  background: rgba(255, 20, 147, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  background: rgba(0, 0, 0, 0.2);
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.8em 0;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(a) {
  color: var(--accent-color);
  text-decoration: underline;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 1em 0;
}

/* 用户消息内的 Markdown 样式覆盖 */
.message-item.user .markdown-body :deep(strong) {
  color: white;
}

.message-item.user .markdown-body :deep(blockquote) {
  background: rgba(255, 255, 255, 0.15);
  border-left-color: white;
}

.message-item.user .markdown-body :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.message-item.user .markdown-body :deep(a) {
  color: white;
}

/* 增强的打字光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background: linear-gradient(135deg, var(--secondary-color), #ff6b6b);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s infinite;
}
</style> 