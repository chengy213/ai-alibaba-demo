package com.example.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.ai.tool.CalculatorTool;
import com.example.ai.tool.MemoTool;
import com.example.ai.tool.WeatherTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    private final WeatherTool weatherTool;
    private final MemoTool memoTool;
    private final CalculatorTool calculatorTool;

    public AgentConfig(WeatherTool weatherTool,
                       MemoTool memoTool,
                       CalculatorTool calculatorTool) {
        this.weatherTool = weatherTool;
        this.memoTool = memoTool;
        this.calculatorTool = calculatorTool;
    }

    // 创建 ToolCallbackProvider，它会自动从标注了 @Tool 的方法生成 ToolCallback
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTool, memoTool, calculatorTool)
                .build();
    }

    @Bean
    public ReactAgent weatherAssistantAgent(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        String systemPrompt = """
            你是一个友好、乐于助人且具有一定幽默感的智能生活助手“小 A”，你具备以下能力：

            1. 「get_weather」：查询中国城市的实时天气。
            2. 「memo」：管理备忘录。
            3. 「calculator」：计算数学表达式。

            回答规则：
            - 如果调用了工具，请在最终回复中自然引用工具返回的结果，不要直接暴露原始 JSON。
            - 如果用户闲聊，你可以自由回答，但尽量引导他们利用你的工具能力。
            - 回答保持友好、简洁，必要时使用 Emoji。
            """;

        return ReactAgent.builder()
                .name("life_assistant")
                .model(chatModel)
                .tools(toolCallbackProvider.getToolCallbacks())  // ✅ 正确用法
                .systemPrompt(systemPrompt)
                .saver(new MemorySaver())
                .enableLogging(true)
                .build();
    }
}