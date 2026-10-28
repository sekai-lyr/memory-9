<div align="center">

# 📝 SekaiForm · 表单收集与 AI 对话系统

> ⭐ Sekai 系列实战项目 09 / 14 · 单项目独立仓库

![Java](https://img.shields.io/badge/Java-red?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![MyBatis](https://img.shields.io/badge/MyBatis-blue?style=flat-square) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-blue?style=flat-square) ![MySQL](https://img.shields.io/badge/MySQL-blue?style=flat-square)

[![Stars](https://img.shields.io/github/stars/sekai-lyr/memory-9?style=flat-square&logo=github)](https://github.com/sekai-lyr/memory-9/stargazers) [![合集](https://img.shields.io/badge/📚_全系列合集-sekai--product-8250DF?style=flat-square)](https://github.com/sekai-lyr/sekai-product)

</div>

---

一个基于 **Spring Boot + Thymeleaf + MyBatis** 的表单与 AI 对话系统，用于收集用户表单数据、意图识别与工具调用。

## 功能

- 用户表单填写与提交
- 意图识别（intent）与多轮对话
- 城市选择 / 日志记录 / 模型选择
- Live2D（pixi-live2d）角色展示
- 工具调用（tool choice）
- 对话状态检查与异步处理

## 项目结构

- `SekaiForm/`：Spring Boot 主项目（`com.sekai.sekai_form`，control / service / dataobject 分层）
- 根目录下包含多个 Python 辅助脚本：
  - `add_*.py`：向数据库或系统添加城市、意图、日志、模型、Live2D、工具选择等数据
  - `check_*.py`：检查状态、函数、共存、最终一致性等
  - `chk_*.py`：对话/代码/异步校验脚本
  - `chat_method.txt`：对话方式说明

## 运行

```powershell
cd SekaiForm
mvn spring-boot:run
```

启动前需要提供一个 OpenAI-compatible 模型配置。可以使用通用变量：

```powershell
$env:AI_API_URL = "https://your-provider.example/v1/chat/completions"
$env:AI_API_KEY = "your-api-key"
$env:AI_MODEL = "your-model"
```

DeepSeek 可使用：`$env:DEEPSEEK_API_KEY = "your-deepseek-key"`，应用默认使用 `https://api.deepseek.com/v1/chat/completions` 和 `deepseek-chat`。

如果使用 memory-14 同样的 DashScope 配置，也可以只设置 `$env:DASHSCOPE_API_KEY`；应用会自动使用 DashScope 兼容接口和 `qwen-plus`。已有数据库配置不会被启动脚本覆盖，只有旧版本错误地把 DashScope Key 配到 DeepSeek 地址的自动初始化记录会被迁移。

## 说明

本项目的业务数据表统一使用 `sekai_form_` 前缀，数据库为 `sekai_friend`。

---

## 🗺️ Sekai 项目全景导航

本项目是 **Sekai 系列作品集** 的一员，全部 14 个实战项目：

| 项目 | 简介 |
|------|------|
| [`memory-1`](https://github.com/sekai-lyr/memory-1) | 📊 二次元数据中心 |
| [`memory-2`](https://github.com/sekai-lyr/memory-2) | 🛒 二次元电商商城 |
| [`memory-3`](https://github.com/sekai-lyr/memory-3) | 📔 二次元回忆录 |
| [`memory-4`](https://github.com/sekai-lyr/memory-4) | 🎋 角色抽签与今日运势 |
| [`memory-5`](https://github.com/sekai-lyr/memory-5) | 🎵 歌词同步音乐播放器 |
| [`memory-6`](https://github.com/sekai-lyr/memory-6) | 🎮 原创角色合成小游戏 |
| [`memory-7`](https://github.com/sekai-lyr/memory-7) | 🖼️ 二次元壁纸图库系统 |
| [`memory-8`](https://github.com/sekai-lyr/memory-8) | 📖 英语单词学习系统 |
| [`memory-9`](https://github.com/sekai-lyr/memory-9) | 📝 表单收集与 AI 对话系统 |
| [`memory-10`](https://github.com/sekai-lyr/memory-10) | 📺 动漫视频导航站 |
| [`memory-11`](https://github.com/sekai-lyr/memory-11) | ⚔️ 游戏王规则卡牌对战 |
| [`memory-12`](https://github.com/sekai-lyr/memory-12) | 🎓 游戏化学习平台 |
| [`memory-13`](https://github.com/sekai-lyr/memory-13) | 🎭 Ren'Py 视觉小说 |
| [`memory-14`](https://github.com/sekai-lyr/memory-14) | 🤖 识番推荐与 RAG 助手平台 |

📦 **一站式浏览全部项目**：[sekai-lyr/sekai-product](https://github.com/sekai-lyr/sekai-product)（monorepo 合集）

<div align="center">

**如果这个项目对你有帮助或启发，欢迎点一个 ⭐ Star 支持一下！**

*Made with ❤️ by [sekai-lyr](https://github.com/sekai-lyr)*

</div>
