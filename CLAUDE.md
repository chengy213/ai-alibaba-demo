# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基于 Spring AI Alibaba 的天气/生活助手演示项目。前端聊天页接收自然语言，后台 `ReactAgent` 通过 ReAct（Reasoning + Acting）循环识别意图、调用工具、生成回答。核心场景：查天气、记备忘录、算数学表达式。

技术栈：Spring Boot 3.4.5 + Spring AI Alibaba 1.1.2.0 + Redisson 3.27.0 + Redis，JDK 17。

## 常用命令

```bash
mvn clean compile          # 编译
mvn spring-boot:run        # 启动应用（监听 8080 端口）
mvn clean package          # 打包
```

- 运行前提：本地需有 Redis（`localhost:6379`，见 `application.yml`），否则启动报错。
- 大模型 API Key 硬编码在 `application.yml` 的 `spring.ai.dashscope.api-key`（百炼 DashScope）。
- 依赖从 `spring-milestones` 仓库拉取（`pom.xml` 中已配置）。
- 项目无测试代码（`src/test` 为空）。

## 架构

### Agent 装配（核心）

`config/AgentConfig.java` 是总装点，构造 `ReactAgent` Bean（name=`life_assistant`）：

- `ChatModel`（DashScope 大模型）由 Spring AI Alibaba 自动配置提供，本类中的 `chatModel` Bean 已被注释掉。
- `ToolCallbackProvider` 通过 `MethodToolCallbackProvider` 扫描所有带 `@Tool` 注解的方法，把它们封装成模型可调用的工具列表。
- `RedisSaver`（基于 Redisson）持久化 Agent 会话状态，配合 `RunnableConfig.threadId` 实现跨请求上下文记忆。
- `ModelCallLimitHook` 限制单次对话最多 `app.agent.max-iterations`（默认 5）轮推理/行动循环，防止死循环。
- 系统提示词在运行时注入当天真实日期，引导模型把「今天/后天/下周一」等相对日期换算成 `yyyy-MM-dd`。

### 请求流

`controller/ChatController.java`：

- `POST /api/chat/stream?userId=xxx`：流式 SSE 接口。用 `RunnableConfig.threadId(userId)` 绑定会话，框架据此自动加载/保存 Agent 历史状态。
- `GET /api/history?userId=xxx`：返回最近 20 条聊天记录（存于 Redis List `chat:history:{userId}`）。
- 对话历史（前端可见的 user/assistant 消息）与 Agent 内部状态（`RedisSaver`）是**两套独立存储**，前者在 `ChatController` 手动写 Redis List，后者由框架自动管理。

### 工具（`tool/` 包）

每个 `@Component` 暴露一个或多个 `@Tool` 方法：

- `WeatherTool.getWeather(city, date)`：调 Open-Meteo 免费 API（无密钥）。先 geocoding 解析城市经纬度，再查未来 7 天预报；拒绝过去日期和 >6 天的日期。
- `UserLocationTool.getUserLocation(city, date)`：仅把 city/date 拼接成字符串，日期缺省时用服务器当天。**基本是残废功能**——本机无 IP 定位能力，且依赖模型先给 city/date，自身不查任何东西（见 `version2-20260509.txt` 备注）。
- `CalculatorTool.calculator(expression)`：用 `javax.script` 的 JavaScript 引擎求值。
- `MemoTool.memo(command)`：内存 List 存备忘，重启即丢。

## 已知坑 / 注意事项

- **CalculatorTool 在 JDK 17 下大概率不可用**：Nashorn 引擎从 JDK 15 起被移除，`getEngineByName("JavaScript")` 会返回 null，工具返回「计算器不可用」。要修需引入 `nashorn-core` 依赖或换 exp4j 等解析库。
- **DashScopeChatOptions 用 `maxToken(1000)` 而非 `withMaxTokens(...)`**（见 `AgentConfig` 中被注释的代码）。
- 流式输出只把 `OutputType.AGENT_MODEL_STREAMING` 的文本推给前端；`AGENT_MODEL_FINISHED`/`AGENT_TOOL_FINISHED` 事件仅打印日志。工具调用决策（Round 0）的 `text` 为空，会被 `.filter(text -> !text.isEmpty())` 丢弃——这是预期行为，用户只看到 Round 1 生成的最终回答。
- API Key 明文写在 `application.yml` 里，提交前注意。

## 参考文档

- `version1-20260429.txt`：v1 初始架构说明与组件关系。
- `version2-20260509.txt`：v2 改动日志（真实天气、Redis 持久化、UserLocationTool、推理轮次限制）。
- `readme-2rounds.txt`：Agent 两轮推理（ReAct）的日志解读与原理说明。
