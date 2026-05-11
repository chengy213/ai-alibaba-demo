package com.example.ai.tool;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoTool {

    private final List<String> memos = new ArrayList<>();

    @Tool(description = "Manage user memos. Use 'add: content' to add, 'list' to view.")
    public String memo(
            @ToolParam(description = "Command string, e.g. 'add: meeting at 3pm' or 'list'") String command) {
        if (command == null || command.isBlank()) {
            return "Invalid format. Use 'add: content' or 'list'.";
        }
        String cmd = command.trim();
        if (cmd.startsWith("add:")) {
            String content = cmd.substring(4).trim();
            if (content.isEmpty()) return "Memo content cannot be empty.";
            memos.add(content);
            return "Memo added: [" + content + "]";
        }
        if ("list".equalsIgnoreCase(cmd)) {
            if (memos.isEmpty()) return "No memos yet.";
            StringBuilder sb = new StringBuilder("Your memos:\n");
            for (int i = 0; i < memos.size(); i++) {
                sb.append(i + 1).append(". ").append(memos.get(i)).append("\n");
            }
            return sb.toString();
        }
        return "Unknown command. Use 'add: content' or 'list'.";
    }

    public ToolCallback memoToolCallback() {
        return ToolCallbacks.from(this)[0];
    }
}