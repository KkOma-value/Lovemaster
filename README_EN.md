<div align="center">

# 💕 Love Master

<a href="#">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&duration=3000&pause=1000&color=ED8B00&center=true&vCenter=true&width=600&lines=Your+AI+Relationship+Assistant;Love+Mode:+Always+Here+to+Listen;Coach+Mode:+Expert+Advice+%26+Strategies;Built+with+Spring+AI+%2B+React" alt="Typing SVG" />
</a>

**A full-stack AI dating companion and coaching application built with Spring AI + React**

🌐 **[简体中文](README.md)** | 🌍 **[English](README_EN.md)**
<br/>
[Quick Start](#-quick-start) · [Features](#-features) · [Architecture](#-architecture)

---

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-6DB33F?style=flat-square)](https://spring.io/projects/spring-ai)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL+PgVector-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Supabase](https://img.shields.io/badge/Supabase-Storage-3FCF8E?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

</div>

<br/>

<p align="center">
  <img src="bf2842354775bab871453344de9e4c4e.png" alt="Love Master Homepage" width="900" />
</p>

<br/>

## ✨ Features

<table>
<tr>
<td width="50%">

### 💬 Dating Companion · Love Mode

A gentle chat companion that listens without judgment.

- Text and screenshot input support
- Smart screenshot understanding: OCR extraction + question rewriting
- Automatic context enrichment for precise understanding

</td>
<td width="50%">

### 🧠 Dating Coach · Coach Mode

AI Agent architecture — thinks before acting, gives expert advice.

- On-demand tool calling: email, search, web scraping, PDF generation, **10+ tools**
- MCP Server extension: standalone module with dynamic tool registration
- Analyzes chat history for tailored, tactful response suggestions

</td>
</tr>
<tr>
<td width="50%">

### 🎯 Kiko AI · Probability Analysis

Dating success probability assessment — know where you stand.

- Auto-triggered by intents like "success rate" or "do I have a chance"
- Structured probability cards: score + positive/risk signals
- Next-step action recommendations

</td>
<td width="50%">

### 📚 Knowledge & Memory

The more it knows, the better the advice.

- RAG retrieval augmentation (PostgreSQL + PgVector / Dify / local Wiki)
- User feedback-driven automatic knowledge ingestion, zero manual approval
- Session persistence + background state recovery
- Google OAuth + JWT auth / Supabase cloud storage

</td>
</tr>
</table>

<br/>

## 🏗 Architecture

<details open>
<summary><b>System Architecture Diagram</b></summary>

<br/>

```mermaid
graph TB
    subgraph Frontend ["🖥 Frontend React + Vite :5173"]
        UI[Page Components]
    end

    subgraph Backend ["⚙️ Backend Spring Boot :8088"]
        Intake[MultimodalIntake<br/>Screenshot / OCR / Question Rewrite]
        RAG[RagKnowledgeService<br/>Dify + PgVector + Wiki]
        Brain[BrainAgentService<br/>Decide whether to call tools]
        Tools[ToolsAgentService<br/>Local tools + MCP]
        Love[LoveChatOrchestrator]
        Kiko[ProbabilityAnalysisService]
        Coach[CoachChatOrchestrator]
    end

    subgraph MCP ["🔌 MCP Server :8127"]
        MCPTools[MCP Tool Set]
    end

    subgraph Storage ["☁️ External Services"]
        DB[(PostgreSQL + PgVector)]
        Dify[Dify Dataset API]
        Supa[Supabase Storage]
        Wiki[Local Wiki Knowledge Base]
    end

    UI -->|SSE| Intake
    Intake --> RAG
    RAG --> Brain
    Brain -->|Needs tools| Tools
    Brain -->|Direct answer| Coach
    Tools --> MCPTools
    Love --> Intake
    Kiko --> Intake
    RAG --> DB
    RAG --> Dify
    RAG --> Wiki
    Tools --> Supa
```

</details>

### Chat Pipeline Overview

| Mode | Processing Flow | Characteristics |
|:---:|:---|:---|
| **Love** | `Input → Screenshot Understanding → RAG Recall → Companion Response` | Pure chat, direct advice |
| **Coach** | `Input → Screenshot Understanding → RAG → Brain Decision → [Direct Answer \| Tool Call] → Combined Response` | Agent architecture, think then act |
| **Kiko** | `Input → Intent Recognition → ProbabilityAnalysisService → Probability Card` | Structured probability analysis |

<br/>

## 🚀 Quick Start

### Requirements

| Dependency | Minimum Version |
|:---|:---|
| Java | 21+ |
| Maven | 3.6+ |
| PostgreSQL | 12+ |
| Node.js | 18+ |

### 1️⃣ Configure

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# Edit application-local.yml with your database and API key credentials
```

> 💡 Full configuration guide: [docs/QUICKSTART.md](docs/QUICKSTART.md)

### 2️⃣ Start Backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
# API: http://localhost:8088
```

### 3️⃣ Start Frontend

```bash
cd springai-front-react
npm install && npm run dev
# UI: http://localhost:5173
```

### 4️⃣ Start MCP Server (Optional)

```bash
cd mcp-servers
mvn spring-boot:run -Dspring-boot.run.profiles=local
# MCP: http://localhost:8127
```

> 📖 Detailed configuration (NVIDIA NIM / Dify / Supabase / Google OAuth): see [docs/QUICKSTART.md](docs/QUICKSTART.md)

<br/>

## 📁 Project Structure

<details>
<summary><b>Click to expand full directory</b></summary>

```
Lovemaster/
├── src/                           # Spring Boot backend
│   └── main/java/.../
│       ├── controller/            # REST API + SSE
│       ├── ai/                    # AI core modules
│       │   ├── intake/            # Multimodal input processing
│       │   ├── service/           # Brain / Tools / RAG services
│       │   └── orchestrator/      # Love / Coach orchestrators
│       ├── app/                   # LoveApp core
│       ├── auth/                  # Auth + image storage
│       ├── tools/                 # Tool registration & implementations
│       └── ChatMemory/            # Session persistence
├── springai-front-react/          # React frontend
│   └── src/
│       ├── components/            # Chat / Sidebar / UI components
│       ├── pages/                 # Home / Chat / Auth
│       └── hooks/                 # Custom hooks
├── mcp-servers/                   # MCP Server module
├── docs/                          # Documentation
├── knowledge/                     # Local Wiki knowledge base
└── scripts/                       # Automation scripts
```

</details>

<br/>

## 🛠 Common Commands

<details>
<summary><b>Backend</b></summary>

```bash
mvn test                              # Run tests
mvn -DskipTests=true package          # Build JAR
mvn spring-boot:run -Dspring-boot.run.profiles=local  # Start app
```

</details>

<details>
<summary><b>Frontend</b></summary>

```bash
cd springai-front-react
npm run dev                           # Dev mode
npm run lint                          # Lint check
npm run build                         # Production build
```

</details>

<details>
<summary><b>MCP Server</b></summary>

```bash
cd mcp-servers
mvn test                              # Run tests
mvn spring-boot:run -Dspring-boot.run.profiles=local  # Start MCP
```

</details>

<br/>

## 🧩 Development Guide

| Scenario | How To |
|:---|:---|
| **Add a new tool** | Create a class in `tools/`, annotate with `@Tool`, register in `ToolRegistration` |
| **Modify chat flow** | Edit files under `ai/orchestrator/` and `ai/service/` |
| **Update knowledge base** | `bash scripts/wiki-update.sh` or `bash scripts/setup-wiki-autoupdate.sh` |

> 📖 Full development docs: [docs/WORKFLOW_GUIDE_EN.md](docs/WORKFLOW_GUIDE_EN.md)

<br/>

## 🤝 Contributing

Contributions of all kinds are welcome! Whether it's filing a bug, suggesting a feature, or submitting a PR — we appreciate it all.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<br/>

## 📄 License

This project is licensed under [MIT](LICENSE).

<br/>

<div align="center">

---

**If you find this helpful, please give us a ⭐ Star!**

Made with ❤️ by Love Master Team

</div>
