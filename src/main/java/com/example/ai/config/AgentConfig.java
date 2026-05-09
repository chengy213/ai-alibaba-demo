package com.example.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.redisson.api.RedissonClient;
import com.example.ai.tool.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Agent 总装配置类。
 * <p>
 * 负责创建并装配：
 * <ul>
 *   <li>ChatModel（DashScope 大模型）</li>
 *   <li>ToolCallbackProvider（工具回调）</li>
 *   <li>RedisSaver（Redis 状态持久化）</li>
 *   <li>ModelCallLimitHook（推理轮次限制）</li>
 *   <li>ReactAgent（最终可用的智能体）</li>
 * </ul>
 */
@Configuration
public class AgentConfig {

    private final WeatherTool weatherTool;
    private final MemoTool memoTool;
    private final CalculatorTool calculatorTool;
    private final UserLocationTool userLocationTool;

    public AgentConfig(WeatherTool weatherTool,
                       MemoTool memoTool,
                       CalculatorTool calculatorTool,
                       UserLocationTool userLocationTool) {
        this.weatherTool = weatherTool;
        this.memoTool = memoTool;
        this.calculatorTool = calculatorTool;
        this.userLocationTool = userLocationTool;
    }

    /**
     * 创建 DashScopeChatModel，限制单次最大输出 1000 tokens。
     * <p>
     * 注意：DashScopeChatOptions 中使用 {@code maxToken(1000)} 而非
     * {@code withMaxTokens(1000)}。
     */
//    @Bean
//    public ChatModel chatModel(DashScopeApi dashScopeApi) {
//        return DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .defaultOptions(DashScopeChatOptions.builder()
//                        .maxToken(1000)          // ✅ 正确方法
//                        .build())
//                .build();
//    }

    /**
     * 扫描所有标有 @Tool 注解的方法，将它们封装为 ToolCallback 列表。
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTool, memoTool, calculatorTool, userLocationTool)
                .build();
    }

    /**
     * RedisSaver：将 Agent 状态持久化到 Redis。
     * <p>
     * 使用 {@code new RedisSaver(redissonClient)} 单参构造，
     * 内部自动使用 JacksonStateSerializer 进行序列化。
     * 默认 TTL 为 -1（永不过期），如需设置过期时间可继承并重写。
     */
    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        // 1. 创建一个状态序列化器，用于将 Agent 状态转换为可存储的格式
        // OverAllState::new 告诉序列化器如何创建一个新的空状态对象
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        // 2. 使用 Builder 模式构建 RedisSaver 实例
        return RedisSaver.builder()
                .redisson(redissonClient)       // 注入 Redisson 客户端
                .stateSerializer(serializer)    // 设置状态序列化器
                .build();
    }

    /**
     * 模型调用次数限制 Hook，防止 Agent 进入无限制的推理循环。
     */
    @Bean
    public ModelCallLimitHook modelCallLimitHook(
            @Value("${app.agent.max-iterations}") int maxIterations) {
        return ModelCallLimitHook.builder()
                .runLimit(maxIterations)
                .build();
    }

    /**
     * 组装最终的 ReactAgent Bean。
     * <p>
     * 通过 {@code .saver(redisSaver)} 启用 Redis 持久化，
     * 通过 {@code .hooks(List.of(limitHook))} 启用轮次限制。
     */
    @Bean
    public ReactAgent weatherAssistantAgent(ChatModel chatModel,
                                            ToolCallbackProvider toolCallbackProvider,
                                            RedisSaver redisSaver,
                                            ModelCallLimitHook limitHook) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 强化日期计算规则的提示词
        String systemPrompt = String.format("""
                You are a smart life assistant named "Little A".
                IMPORTANT: Today is %s. Always use this as the reference date.
                When the user asks about weather with relative dates like "today", "tomorrow", "后天 (the day after tomorrow)", "next Monday" etc., 
                you MUST calculate the absolute date in yyyy-MM-dd format based on today (%s) before calling getUserLocation.
                For example: if today is 2026-05-09, then "后天" is 2026-05-11.
                Never guess a date; always calculate it relative to today.
                You can:
                1. getUserLocation: Provide city and the calculated date.
                2. getWeather: Get real weather for that city and date.
                3. calculator: Math calculations.
                4. memo: Manage memos.
                Be friendly and concise.
                """, today, today);

        return ReactAgent.builder()
                .name("life_assistant")
                .model(chatModel)
                .tools(toolCallbackProvider.getToolCallbacks())
                .systemPrompt(systemPrompt)
                .saver(redisSaver)
                .hooks(List.of(limitHook))
                .build();
    }
}