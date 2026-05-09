package com.example.ai.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天控制器，提供流式 SSE 接口。
 * URL: POST /api/chat/stream?userId=xxx
 * 通过 RunnableConfig 绑定 threadId，框架自动加载/保存历史上下文。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ReactAgent agent;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_HISTORY_SIZE = 20;
    private static final String HISTORY_KEY_PREFIX = "chat:history:";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(ReactAgent agent, RedisTemplate<String, Object> redisTemplate) {
        this.agent = agent;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/history")
    public List<Map<String, String>> getHistory(@RequestParam(defaultValue = "1") String userId) {
        String key = HISTORY_KEY_PREFIX + userId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return Collections.emptyList();
        }
        long start = Math.max(0, size - MAX_HISTORY_SIZE);
        List<Object> objects = redisTemplate.opsForList().range(key, start, size - 1);
        if (objects == null) return Collections.emptyList();

        return objects.stream()
                .map(obj -> {
                    try {
                        String json = (String) obj;
                        // 明确告诉编译器我们返回的是 Map<String, String>
                        Map<String, String> map = objectMapper.readValue(json, Map.class);
                        return map;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();  // 使用 Java 16+ 的不变列表收集器，避免类型推断问题
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody Map<String, String> body,
                                                    @RequestParam(defaultValue = "1") String userId) throws GraphRunnerException {
        String userMessage = body.getOrDefault("message", "").trim();
        if (userMessage.isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("请输入一些内容吧 😊")
                    .build());
        }

        addMessageToHistory(userId, "user", userMessage);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(userId)
                .build();

        StringBuilder fullResponse = new StringBuilder();

        return agent.stream(userMessage, config)
                .filter(output -> output instanceof StreamingOutput)
                .map(output -> (StreamingOutput<?>) output)
                .doOnNext(so -> {
                    if (so.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                        System.out.println("📝 模型输出: " + ((AssistantMessage) so.message()).getText());
                    } else if (so.getOutputType() == OutputType.AGENT_MODEL_FINISHED) {
                        AssistantMessage msg = (AssistantMessage) so.message();
                        if (!msg.getToolCalls().isEmpty()) {
                            System.out.println("✅ 模型决定调用工具: " + msg.getToolCalls());
                        } else {
                            System.out.println("💭 模型本轮无工具调用意图。");
                        }
                    } else if (so.getOutputType() == OutputType.AGENT_TOOL_FINISHED) {
                        ToolResponseMessage toolMsg = (ToolResponseMessage) so.message();
                        System.out.println("⚙️ 工具执行完毕: " + toolMsg.getResponses());
                    }
                })
                .filter(so -> so.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
                .map(so -> ((AssistantMessage) so.message()).getText())
                .filter(text -> !text.isEmpty())
                .doOnNext(fullResponse::append)
                .map(text -> ServerSentEvent.<String>builder().data(text).build())
                .doOnComplete(() -> {
                    String reply = fullResponse.toString();
                    if (!reply.isEmpty()) {
                        addMessageToHistory(userId, "assistant", reply);
                    }
                });
    }

    private void addMessageToHistory(String userId, String role, String content) {
        try {
            Map<String, String> msg = Map.of("role", role, "content", content);
            String json = objectMapper.writeValueAsString(msg);
            String key = HISTORY_KEY_PREFIX + userId;
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.opsForList().trim(key, -MAX_HISTORY_SIZE, -1);
        } catch (Exception e) {
            System.err.println("保存历史消息失败: " + e.getMessage());
        }
    }
}