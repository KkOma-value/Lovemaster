/**
 * API 服务层 - 统一管理 SSE 连接
 * 
 * 关键设计：
 * - 使用原生 EventSource 处理 SSE
 * - 收到 [DONE] 标记或连接完成时立即关闭，防止自动重连导致重复请求
 * - 发生错误时立即关闭，不触发 EventSource 默认的自动重连
 */

// 后端 API 基础路径，通过 Vite 代理转发到 http://localhost:8088/api
const API_BASE = '/api'

/**
 * 创建 LoveApp SSE 连接
 * @param {string} message - 用户消息
 * @param {string} chatId - 会话ID（可选）
 * @param {function} onData - 数据回调 (data: string) => void
 * @param {function} onError - 错误回调 (error: Event) => void
 * @param {function} onComplete - 完成回调 () => void
 * @returns {EventSource} - 返回 EventSource 实例，调用方可用于手动关闭
 */
export function createLoveAppSSE(message, chatId, onData, onError, onComplete) {
  const params = new URLSearchParams()
  params.append('message', message)
  if (chatId) {
    params.append('chatId', chatId)
  }

  const url = `${API_BASE}/ai/love_app/chat/sse?${params.toString()}`
  console.log('[LoveApp SSE] 创建连接:', url)

  const eventSource = new EventSource(url)
  let isClosed = false

  // 安全关闭函数，防止重复关闭
  const safeClose = () => {
    if (!isClosed) {
      isClosed = true
      eventSource.close()
      console.log('[LoveApp SSE] 连接已关闭')
    }
  }

  // 监听消息事件
  eventSource.onmessage = (event) => {
    const data = event.data
    console.log('[LoveApp SSE] 收到数据:', data)

    // 检测结束标记
    if (data === '[DONE]') {
      console.log('[LoveApp SSE] 收到结束标记，关闭连接')
      safeClose()
      if (onComplete) onComplete()
      return
    }

    // 正常数据回调
    if (onData) onData(data)
  }

  // 监听错误事件 - 立即关闭，防止自动重连
  eventSource.onerror = (error) => {
    console.error('[LoveApp SSE] 连接错误:', error)
    
    // 如果连接已经关闭（正常结束），不触发错误回调
    if (eventSource.readyState === EventSource.CLOSED && !isClosed) {
      // 正常关闭，触发完成回调
      safeClose()
      if (onComplete) onComplete()
      return
    }

    // 异常错误，立即关闭防止重连
    safeClose()
    if (onError) onError(error)
  }

  // 监听打开事件
  eventSource.onopen = () => {
    console.log('[LoveApp SSE] 连接已建立')
  }

  return eventSource
}

/**
 * 创建 Manus SSE 连接
 * @param {string} message - 用户消息
 * @param {function} onData - 数据回调 (data: string) => void
 * @param {function} onError - 错误回调 (error: Event) => void
 * @param {function} onComplete - 完成回调 () => void
 * @returns {EventSource} - 返回 EventSource 实例，调用方可用于手动关闭
 */
export function createManusSSE(message, onData, onError, onComplete) {
  const params = new URLSearchParams()
  params.append('message', message)

  const url = `${API_BASE}/ai/manus/chat?${params.toString()}`
  console.log('[Manus SSE] 创建连接:', url)

  const eventSource = new EventSource(url)
  let isClosed = false

  // 安全关闭函数
  const safeClose = () => {
    if (!isClosed) {
      isClosed = true
      eventSource.close()
      console.log('[Manus SSE] 连接已关闭')
    }
  }

  // 监听消息事件
  eventSource.onmessage = (event) => {
    const data = event.data
    console.log('[Manus SSE] 收到数据:', data)

    // Manus 使用 SseEmitter，检测可能的结束标记
    if (data === '[DONE]' || data === '') {
      console.log('[Manus SSE] 收到结束标记，关闭连接')
      safeClose()
      if (onComplete) onComplete()
      return
    }

    // 正常数据回调
    if (onData) onData(data)
  }

  // 监听错误事件 - 立即关闭，防止自动重连
  eventSource.onerror = (error) => {
    console.error('[Manus SSE] 连接错误:', error)

    // 如果是正常关闭（服务端完成推送后关闭连接）
    if (eventSource.readyState === EventSource.CLOSED && !isClosed) {
      safeClose()
      if (onComplete) onComplete()
      return
    }

    // 异常错误，立即关闭防止重连
    safeClose()
    if (onError) onError(error)
  }

  // 监听打开事件
  eventSource.onopen = () => {
    console.log('[Manus SSE] 连接已建立')
  }

  return eventSource
}
