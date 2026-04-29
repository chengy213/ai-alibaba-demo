package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 一个简单的科学计算器工具，内部使用 Java ScriptEngine 来计算数学表达式。
 */
@Component
public class CalculatorTool {

    @Tool(description = "计算数学表达式，输入为一个数学表达式字符串，例如 '3*4+2'。返回计算结果。")
    public String calculator(@ToolParam(description = "数学表达式") String expression) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            if (engine == null) {
                engine = new ScriptEngineManager().getEngineByName("js"); // Java 15+ fallback
            }
            if (engine == null) {
                return "当前环境不支持 JavaScript 引擎，请尝试简单表达式。";
            }
            Object result = engine.eval(expression);
            return "计算结果：" + result.toString();
        } catch (Exception e) {
            return "计算出错：" + e.getMessage();
        }
    }
}