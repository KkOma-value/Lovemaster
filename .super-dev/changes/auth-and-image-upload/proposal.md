# Proposal: 用户认证系统 + 图片上传功能

## 变更 ID
`auth-and-image-upload`

## 状态
`approved` — 用户已确认三份核心文档

## 概述
为 Lovemaster 项目添加用户注册/登录功能（JWT 双令牌）、基于 user_id 的数据隔离、以及图片上传功能（前端 browser-image-compression + 后端 MultipartFile）。

## 确认决策
1. **认证方式**: JWT (Access Token 30min + Refresh Token 7天)
2. **数据库**: 新增 users, refresh_tokens, user_images 三张表
3. **MVP 范围**: 仅 Email/密码认证，不含 OAuth、邮箱验证、密码重置
4. **图片存储**: 本地文件系统（预留后续云存储扩展）
5. **SSE 认证**: URL 参数传递 Token

## 参考文档
- `output/auth-upload-research.md`
- `output/auth-upload-prd.md`
- `output/auth-upload-architecture.md`
- `output/auth-upload-uiux.md`
