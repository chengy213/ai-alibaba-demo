---
name: spring-ai-alibaba
description: 开发 Spring AI Alibaba Agent/工具时的参考。何时触发：用户要新增/修改 @Tool 工具、改 ReactAgent 装配、排查 SSE 流式输出或对话历史、调 DashScope 模型调用、动 RedisSaver 持久化。先读 CLAUDE.md 的"已知坑"作为基底。
---
本仓库（ai-alibaba-demo）基于 Spring AI Alibaba 1.1.2.0 + Spring Boot 3.4.5，JDK 17。先读仓库根 `CLAUDE.md` 了解全局架构与已知坑，再按下表按需读取 references/，**不要一次全读**（渐进披露，省主上下文）。

## 入口决策树
| 用户要做的事 | 读哪个参考 |
|---|---|
| 新增/改 @Tool 工具方法 | `references/tool-pattern.md` |
| 改 ReactAgent 装配、加 Hook、改系统提示词 | `references/agent-wiring.md` |
| 排查流式输出 / 对话历史 / 会话记忆 | `references/streaming-and-history.md` |

每个 reference 文件聚焦"这个项目里怎么写 + 已踩过的坑"，配合源码理解。若用户问的点不在上表，直接读源码 + CLAUDE.md，不要硬套参考。
