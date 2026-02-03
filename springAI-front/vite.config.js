import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      // 将 /api 请求代理到后端 http://localhost:8088/api
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
        // 不需要 rewrite，因为后端 context-path 已经是 /api
      }
    }
  }
}) 