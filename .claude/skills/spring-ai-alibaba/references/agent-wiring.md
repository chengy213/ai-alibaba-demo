# ReactAgent 装配

核心装配点：`src/main/java/com/example/ai/config/AgentConfig.java`，构造 name=`life_assistant` 的 `ReactAgent` Bean。

## 组件关系

```
ChatModel (DashScope, 自动配置)
   │
   ├── ToolCallbackProvider (MethodToolCallbackProvider 扫描所有 @Tool)
   ├── RedisSaver (基于 Redisson, 持久化 Agent 会话状态)
   ├── ModelCallLimitHook (限制推理轮次, 防死循环)
   │
   ▼
ReactAgent.builder()
   .name("life_assistant")
   .model(chatModel)
   .tools(toolCallbackProvider.getToolCallbacks())
   .systemPrompt(systemPrompt)   // 运行时注入当天日期
   .saver(redisSaver)
   .hooks(List.of(limitHook))
   .build()
```

## 关键点与坑

### ChatModel
- 由 Spring AI Alibaba 自动配置提供（`spring.ai.dashscope.api-key`）。
- `AgentConfig` 中手写的 `chatModel` Bean **已被注释掉**——如需自定义（如改 max-tokens），取消注释并改用 `DashScopeChatOptions.builder().maxToken(1000)`。
- ⚠️ 用 `maxToken(1000)`，**不是** `withMaxTokens(1000)`。
- `application.yml` 里也可设 `spring.ai.dashscope.chat.options.max-tokens`。

### ToolCallbackProvider
- `MethodToolCallbackProvider.builder().toolObjects(...)` 列出所有工具 Bean。
- **新增工具后必须把 Bean 加进 `toolObjects(...)`，否则模型调不到。**

### RedisSaver
- `RedisSaver.builder().redisson(redissonClient).stateSerializer(serializer).build()`。
- 序列化器用 `SpringAIJacksonStateSerializer(OverAllState::new)`。
- 配合 `RunnableConfig.threadId(userId)` 实现跨请求上下文记忆——框架据 threadId 自动加载/保存状态。
- 默认 TTL=-1（永不过期）；如需过期需继承重写。

### ModelCallLimitHook
- `ModelCallLimitHook.builder().runLimit(app.agent.max-iterations).build()`，默认 5 轮。
- 限制单次对话最多 N 轮推理/行动循环，防死循环浪费 token。

### 系统提示词
- 运行时用 `LocalDate.now()` 注入当天日期，引导模型把"今天/后天/下周一"换算成 `yyyy-MM-dd`。
- 改提示词时保留日期注入那一段，否则相对日期计算会出错。

## 配置项（application.yml 的 app.*）
- `app.agent.max-iterations`: 最大推理轮次（默认 5）。
- `app.conversation.max-messages`: 历史滑窗（默认 20）。
- `app.conversation.ttl`: Redis 会话过期（默认 3d）。
