package com.example.ai.config;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.example.ai.hook.MessageTrimmingHook;
import com.example.ai.tool.*;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Agent 总装配置类。
 * <p>
 * 负责创建并装配：
 * <ul>
 *   <li>ChatModel（DashScope 大模型，由自动配置提供）</li>
 *   <li>ToolCallbackProvider（全局工具回调）</li>
 *   <li>SkillRegistry + SkillsAgentHook（技能渐进式披露）</li>
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
     * 全局工具回调：包括 Calculator、UserLocation（始终可用）。
     * 注意：Weather 和 Memo 工具将通过 Skills 渐进式披露，不在此注册。
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculatorTool, userLocationTool)
                .build();
    }

    /**
     * 技能注册表：从 classpath:skills 目录加载技能。
     */
    @Bean
    public SkillRegistry skillRegistry() {
        return ClasspathSkillRegistry.builder()
                .classpathPath("skills")
                .build();
    }

    /**
     * 技能 Hook：注册 read_skill 工具、注入技能列表到系统提示，
     * 并通过 groupedTools 实现渐进式工具披露。
     */
    @Bean
    public SkillsAgentHook skillsAgentHook(SkillRegistry skillRegistry,
                                           ToolCallback weatherToolCallback,
                                           ToolCallback memoToolCallback) {
        // 将 WeatherTool 绑定到 travel-planner 技能
        // 将 MemoTool 绑定到 health-reminder 技能
        // 只有模型调用 read_skill 激活对应技能后，这些工具才可用

        Map<String, List<ToolCallback>> groupedTools = Map.of(
                // (SKILL.md) 中的 name 字段：travel-planner
                "travel-planner", List.of(weatherToolCallback),
                // (SKILL.md) 中的 name 字段：health-reminder
                "health-reminder", List.of(memoToolCallback)
        );

        return SkillsAgentHook.builder()
                .skillRegistry(skillRegistry)
                .groupedTools(groupedTools)
                .build();
    }

    /**
     * WeatherTool 的工具回调（供 SkillsAgentHook 的 groupedTools 使用）。
     */
    @Bean
    public ToolCallback weatherToolCallback() {
        return ToolCallbacks.from(weatherTool)[0];
    }

    @Bean
    public ToolCallback memoToolCallback() {
        return ToolCallbacks.from(memoTool)[0];
    }

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
        return RedisSaver.builder()
                .redisson(redissonClient)
                .stateSerializer(serializer)
                .build();
    }

    @Bean
    public ModelCallLimitHook modelCallLimitHook(
            @Value("${app.agent.max-iterations}") int maxIterations) {
        return ModelCallLimitHook.builder()
                .runLimit(maxIterations)
                .build();
    }

    @Bean
    public ReactAgent weatherAssistantAgent(ChatModel chatModel,
                                            ToolCallbackProvider toolCallbackProvider,
                                            SkillsAgentHook skillsAgentHook,
                                            RedisSaver redisSaver,
                                            ModelCallLimitHook limitHook,
                                            MessageTrimmingHook trimmingHook) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String systemPrompt = String.format("""
                                You are a smart life assistant named "Little A". Today's date is %s.
                                IMPORTANT RULES:
                                - To use weather-related tools, you MUST first call read_skill("travel-planner") .
                                - To use memo tools, you MUST first call read_skill("health-reminder") .
                                - Tools getWeather, memo are NOT available until you activate the skill.
                            
                                Always-available tools:
                                - calculator : Math calculations
                                - getUserLocation : Extract city and date from user queries
                            
                                Be friendly, concise, and helpful.
                                """, today);

        return ReactAgent.builder()
                .name("life_assistant")
                .model(chatModel)
                .tools(toolCallbackProvider.getToolCallbacks())
                .systemPrompt(systemPrompt)
                .hooks(List.of(skillsAgentHook, limitHook, trimmingHook))
                .saver(redisSaver)
                .build();
    }
}