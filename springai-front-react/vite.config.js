import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        // 配置 SSE 代理支持
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req) => {
            // 禁用代理响应的缓冲，避免 SSE 事件被缓冲
            if (req.url?.includes('/chat/sse') || req.url?.includes('/manus/chat')) {
              proxyRes.headers['cache-control'] = 'no-cache';
              proxyRes.headers['x-accel-buffering'] = 'no';
            }
          });
        }
      }
    }
  }
})
