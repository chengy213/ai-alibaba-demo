package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 备忘录工具，用于管理用户的简短备忘信息。
 * 数据仅保存在内存中，重启后丢失。
 */
@Component
public class MemoTool {

    // 使用一个简单的 List 存储备忘
    private final List<String> memos = new ArrayList<>();

    @Tool(description = "管理用户的备忘录。添加格式：'add: 内容'；列出所有备忘：'list'")
    public String memo(
            @ToolParam(description = "操作指令，例如 'add: 下午3点开会' 或 'list'") String command) {
        if (command == null || command.isBlank()) {
            return "格式错误，请使用 'add: 内容' 或 'list'";
        }
        String cmd = command.trim();
        if (cmd.startsWith("add:")) {
            String content = cmd.substring(4).trim();
            if (content.isEmpty()) {
                return "备忘内容不能为空";
            }
            memos.add(content);
            return "已添加备忘：「" + content + "」";
        }
        if ("list".equalsIgnoreCase(cmd)) {
            if (memos.isEmpty()) {
                return "暂无备忘";
            }
            StringBuilder sb = new StringBuilder("您的备忘：\n");
            for (int i = 0; i < memos.size(); i++) {
                sb.append(i + 1).append(". ").append(memos.get(i)).append("\n");
            }
            return sb.toString();
        }
        return "未知操作，请使用 'add: 内容' 添加备忘，或 'list' 查看。";
    }
}