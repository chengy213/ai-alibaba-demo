package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一个简易的在内存中维护的备忘录工具。
 * 支持添加和查询操作，工具方法根据第一个参数决定执行哪种操作。
 */
@Component
public class MemoTool {

    private final List<String> memos = new ArrayList<>();

    @Tool(description = "管理个人备忘录。" +
            "若用户希望记住某件事请发送 'add: 事情描述'；" +
            "若用户希望查看已有备忘录请发送 'list'。")
    public String memo(@ToolParam(description = "操作命令，add: 开头为添加，list 为查询") String command) {
        if (command == null || command.isBlank()) {
            return "请提供有效操作（add: 或 list）。";
        }
        String cmd = command.trim();
        if (cmd.startsWith("add:")) {
            String content = cmd.substring(4).trim();
            if (content.isEmpty()) return "备忘录内容不能为空。";
            memos.add(content);
            return "✅ 已添加备忘录：「" + content + "」。";
        } else {
            if (memos.isEmpty()) return "📋 还没有任何备忘录记录。";
            return "📋 您的备忘录：\n" + memos.stream()
                    .map(m -> "- " + m)
                    .collect(Collectors.joining("\n"));
        }
    }
}