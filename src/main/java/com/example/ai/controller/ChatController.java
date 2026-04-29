package com.example.ai.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ReactAgent agent;

    public ChatController(ReactAgent agent) {
        this.agent = agent;
    }

    /**
     * 同步聊天接口（保留，作为示例）
     * 请求体：{"message": "你好"}
     * 响应体：{"reply": "你好！有什么可以帮你的？"}
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) throws GraphRunnerException {
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return Map.of("reply", "请输入一些内容吧 😊");
        }
        AssistantMessage assistantMessage = agent.call(userMessage);
        return Map.of("reply", assistantMessage.getText());
    }

    /**
     * 流式聊天接口
     * 请求体：{"message": "北京天气"}
     * 响应：SSE 流，每块数据为纯文本（不是 JSON）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody Map<String, String> body) throws GraphRunnerException {
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("请输入一些内容吧 😊")
                    .build());
        }

        return agent.stream(userMessage)
                .filter(output -> output instanceof StreamingOutput)
                .map(output -> (StreamingOutput<?>) output)
                .doOnNext(so -> { // 1. 使用 doOnNext 观察所有流式输出状态
                    if (so.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                        // 模型正在流式输出文本
                        System.out.println("📝 模型输出: " + ((AssistantMessage) so.message()).getText());
                    } else if (so.getOutputType() == OutputType.AGENT_MODEL_FINISHED) {
                        // 本轮模型思考结束，可以在这里查看完整的 AssistantMessage
                        AssistantMessage msg = (AssistantMessage) so.message();
                        if (!msg.getToolCalls().isEmpty()) {
                            System.out.println("✅ 模型决定调用工具: " + msg.getToolCalls());
                        } else {
                            System.out.println("💭 模型本轮无工具调用意图。");
                        }
                    } else if (so.getOutputType() == OutputType.AGENT_TOOL_FINISHED) {
                        // 工具执行完毕，查看工具返回结果
                        ToolResponseMessage toolMsg = (ToolResponseMessage) so.message();
                        System.out.println("⚙️ 工具执行完毕: " + toolMsg.getResponses());
                    }
                })
                // 2. 仅将模型流式输出的文本推送给前端
                .filter(so -> so.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
                .map(so -> {
                    AssistantMessage msg = (AssistantMessage) so.message();
                    return msg.getText();
                })
                .filter(text -> !text.isEmpty())
                .map(text -> ServerSentEvent.<String>builder().data(text).build());
    }
}