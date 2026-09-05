# 流式输出与对话历史

核心文件：`src/main/java/com/example/ai/controller/ChatController.java`。

## 两套独立存储（勿混淆）

| 存储 | 内容 | 谁写 | Key |
|---|---|---|---|
| Redis List `chat:history:{userId}` | 前端可见的 user/assistant 消息 | `ChatController` 手动 rightPush + trim | `chat:history:xxx` |
| RedisSaver | Agent 内部状态（含工具调用历史） | 框架据 `RunnableConfig.threadId` 自动加载/保存 | 由 RedisSaver 管理 |

**两者不互通**：前端历史是手动写的纯文本消息；Agent 状态是框架管理的完整 OverAllState。改历史相关逻辑时先确认动的是哪一套。

## SSE 流式接口

`POST /api/chat/stream?userId=xxx`，返回 `text/event-stream`。

```java
agent.stream(userMessage, config)
    .filter(output -> output instanceof StreamingOutput)
    .map(output -> (StreamingOutput<?>) output)
    .doOnNext(so -> { /* 打印 AGENT_MODEL_FINISHED / AGENT_TOOL_FINISHED 日志 */ })
    .filter(so -> so.getOutputType() == OutputType.AGENT_MODEL_STREAMING)  // 只推模型流式文本
    .map(so -> ((AssistantMessage) so.message()).getText())
    .filter(text -> !text.isEmpty())   // 丢弃空 text（Round 0 工具调用决策）
    .doOnNext(fullResponse::append)
    .map(text -> ServerSentEvent.<String>builder().data(text).build())
    .doOnComplete(() -> addMessageToHistory(userId, "assistant", fullResponse.toString()));
```

## ReAct 两轮推理（为何前端看不到工具调用）

- **Round 0**：模型决定调用工具，`AssistantMessage.text` 为空、`toolCalls` 非空 → 被 `.filter(text -> !text.isEmpty())` 丢弃。**这是预期行为**，用户不需要看到工具调用指令。
- **Round 1**：模型基于工具返回结果生成自然语言回答，逐字流式输出 → 前端只看到这段。
- `AGENT_MODEL_FINISHED` / `AGENT_TOOL_FINISHED` 事件只打日志，不推前端。

详见 `readme-2rounds.txt` 的日志解读。**不要把 Round 0 空 text 当成 bug 修复。**

## 会话绑定
- `RunnableConfig.builder().threadId(userId).build()` 绑定会话 ID。
- 框架据此 threadId 自动加载历史 Agent 状态（工具调用历史），并在轮次结束后自动保存。
- 前端历史则通过 `addMessageToHistory` 手动写 Redis List。

## 历史接口
- `GET /api/history?userId=xxx`：返回最近 `MAX_HISTORY_SIZE`（=20）条。
- `addMessageToHistory` 用 `rightPush` + `trim(key, -MAX_HISTORY_SIZE, -1)` 保持滑窗。
- 前端 `index.html` 在 `DOMContentLoaded` 时自动拉取并渲染历史。
