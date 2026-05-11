package com.example.ai.hook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息裁剪 Hook，基于 MessagesModelHook 实现。
 * 在每次模型调用前，检查消息列表长度，
 * 若超过 maxMessages 则裁剪掉最早的消息，
 * 只保留最近的 maxMessages 条。
 */
@Component
public class MessageTrimmingHook extends MessagesModelHook {

    private final int maxMessages;

    public MessageTrimmingHook(@Value("${app.conversation.max-messages:20}") int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public String getName() {
        return "message_trimming_hook";
    }

    /**
     * 指定 Hook 的触发位置为 BEFORE_MODEL，
     * 即每次模型调用之前执行。
     */
    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    /**
     * 在模型调用前执行消息裁剪。
     *
     * @param previousMessages 当前消息列表（包含历史消息）
     * @param config           运行配置
     * @return AgentCommand，包含裁剪后的消息列表和 REPLACE 策略
     */
    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages == null || previousMessages.isEmpty()) {
            System.out.println("📌 [MessageTrimmingHook] 消息列表为空，跳过裁剪。");
            return super.beforeModel(previousMessages, config);
        }

        int currentSize = previousMessages.size();
        System.out.println("📌 [MessageTrimmingHook] 当前消息数: " + currentSize + "，最大允许: " + maxMessages);

        if (currentSize <= maxMessages) {
            System.out.println("📌 [MessageTrimmingHook] 消息数未超过限制，无需裁剪。");
            return super.beforeModel(previousMessages, config);
        }

        // 裁剪：只保留最近的 maxMessages 条消息
        // 注意：subList 返回的是视图，需要 new ArrayList 包装以防止并发修改问题
        List<Message> trimmedMessages = new ArrayList<>(
                previousMessages.subList(currentSize - maxMessages, currentSize)
        );

        System.out.println("📌 [MessageTrimmingHook] 消息已裁剪，保留最近 " + trimmedMessages.size() + " 条。");

        // 返回裁剪后的消息列表，使用 REPLACE 策略更新 Agent 状态
        return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
    }
}