package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 数学计算工具。
 * 模型可以通过调用这个工具完成用户提出的数学表达式计算。
 * 内部使用 javax.script.ScriptEngine 实现，如需更健壮的实现，可替换为 exp4j 等表达式解析库。
 */
@Component
public class CalculatorTool {

    @Tool(description = "计算一个数学表达式，例如 3*4+2，返回计算结果")
    public String calculator(
            @ToolParam(description = "数学表达式字符串，只能包含数字、运算符和小数点") String expression) {
        if (expression == null || expression.isBlank()) {
            return "表达式不能为空";
        }
        try {
            // Java 17 下仍可使用 nashorn，但建议添加 nashorn-core 依赖
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript");
            if (engine == null) {
                return "计算器不可用（缺少 JavaScript 引擎）";
            }
            Object result = engine.eval(expression);
            return "计算结果：" + result;
        } catch (Exception e) {
            return "计算错误：" + e.getMessage();
        }
    }
}