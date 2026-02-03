# Design: Shared file-save directory across main app and MCP server

## Problem
当前实现把文件根目录绑定到进程工作目录：
- 主应用：`FileConstant.FILE_SAVE_DIR = ${user.dir}/tmp`
- MCP server：`Path.of(${user.dir}, "tmp", "download")`

当两个进程从不同目录启动时（例如主应用从 repo 根目录启动，MCP server 从 `mcp-servers/` 启动），会产生两个不同的 `tmp/download`。

## Design Goals
- 同一套配置能让主应用与 MCP server 写入/读取同一目录。
- 保持向后兼容：不配置时沿用 `${user.dir}/tmp`。
- 继续保留 PDF 写图的安全边界：禁止 `tmp/download` 目录穿越。

## Proposed Approach
- 引入共享配置项：`app.file-save-dir`（绝对路径或相对路径均可）。
  - 未配置：默认 `${user.dir}/tmp`。
  - `downloadDir = <file-save-dir>/download`
  - `pdfDir = <file-save-dir>/pdf`
- MCP server 与主应用均读取相同配置名。

## Observability
- 工具成功/失败时返回“resolved paths”：
  - `downloadDir` / `pdfPath`
  - 缺图时列出“expected at ...”

## Alternatives Considered
- 强制要求用户从 repo 根目录启动所有进程：简单但脆弱，且与部署环境不匹配。
- 在主应用侧读取 MCP server 的返回绝对路径并允许跨目录：会弱化目录边界与安全约束，不推荐。
