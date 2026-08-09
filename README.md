# 🤖 SekaiForm · 表单与 AI 对话系统

> **Spring Boot + Thymeleaf + MyBatis 表单系统 · 意图识别 · 工具调用 · Live2D 角色**
> Form system with intent recognition, tool calling, and Live2D characters

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005F0F)](https://www.thymeleaf.org/)
[![MyBatis](https://img.shields.io/badge/MyBatis-3-lightgrey)](https://mybatis.org/)
[![Live2D](https://img.shields.io/badge/Live2D-pixi--live2d-ff69b4)](https://www.live2d.com/)
[![AI](https://img.shields.io/badge/意图识别-工具调用-purple)](#)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A form & AI dialogue system built with **Spring Boot + Thymeleaf + MyBatis**: collect user form data, recognize intents, call tools, and render **Live2D characters** in the browser via pixi-live2d.

基于 **Spring Boot + Thymeleaf + MyBatis** 的表单与 AI 对话系统：收集用户表单数据、意图识别与工具调用，支持 Live2D 角色展示。

---

## ✨ Features / 核心功能

- 用户表单填写与提交 User form filling & submission
- 意图识别（intent）与多轮对话 Intent recognition & multi-turn dialogue
- 城市选择 / 日志记录 / 模型选择 City selection / logging / model selection
- **Live2D（pixi-live2d）角色展示** Live2D character display
- **工具调用（tool choice）** Tool calling
- 对话状态检查与异步处理 Dialogue state checks & async handling

## 📐 Project Structure / 项目结构

```text
sekai-form-ai
├── SekaiForm/              # Spring Boot 主项目 (com.sekai.sekai_form, control/service/dataobject 分层)
│   ├── src/                # 后端 + 前端 (Thymeleaf + static)
│   ├── models/2d/          # Live2D 模型 (hiyori / haru / aidang / biaoqiang)
│   └── pom.xml
└── README.md
```

- `SekaiForm/models/2d/`：Live2D 官方免费示例模型（Live2D Free Sample Models），可直接使用

## ▶️ Quick Start / 快速开始

```powershell
cd SekaiForm
# 配置环境变量（如 DASHSCOPE_API_KEY，用于 AI 对话）
mvn spring-boot:run
```

- 数据库：`sekai_friend`，业务表统一 `sekai_form_` 前缀
- 业务数据表由 `src/main/resources/schema.sql` 初始化

## 📝 Notes / 说明

- AI 对话需要配置大模型 API Key（通过环境变量 `DASHSCOPE_API_KEY` 等注入）
- Live2D 模型为 Live2D Inc. 官方免费示例，遵循其 Free Material License

## 📄 License

[MIT](LICENSE) © 2026 [sekai-lyr](https://github.com/sekai-lyr)

---

**⭐ If this project helped you, star it! 如果这个项目对你有帮助，欢迎 Star！**
