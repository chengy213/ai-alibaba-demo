# @Tool 工具开发模式

本仓库工具位于 `src/main/java/com/example/ai/tool/`，每个 `@Component` 暴露一个或多个 `@Tool` 方法，由 `AgentConfig.toolCallbackProvider()` 通过 `MethodToolCallbackProvider` 统一扫描成模型可调用的工具列表。

## 标准写法

```java
@Component
public class XxxTool {

    @Tool(description = "一句话说清这个工具能干什么——模型据此决定是否调用")
    public String doXxx(
            @ToolParam(description = "参数含义与格式，如：日期 yyyy-MM-dd") String date) {
        if (date == null || date.isBlank()) return "日期不能为空";
        try {
            // ... 业务逻辑
            return "可读的中文结果";
        } catch (Exception e) {
            return "执行失败：" + e.getMessage();  // 不要抛异常
        }
    }
}
```

要点：
- **返回类型用 String**，错误也返回可读消息而非抛异常——模型会把返回值当作"观察"继续推理。
- **`@Tool` description 是模型的路由依据**，写清能力边界（如"仅支持未来 6 天"）。
- **`@ToolParam` description 说明参数含义与格式**，模型据此从自然语言抽取参数。
- 入参做 null/blank 兜底。

## 既有工具与坑

### WeatherTool.getWeather(city, date)
- 调 Open-Meteo 免费 API（无密钥）：先 geocoding 解析经纬度，再查未来 7 天预报。
- 拒绝过去日期（`daysAhead < 0`）和 >6 天的日期。
- 改动时注意 `findDateIndex` 按 `time` 数组匹配 `queryDate`。

### UserLocationTool.getUserLocation(city, date)
- **基本残废**：仅把 city/date 拼成 `city:xxx, date:xxx` 字符串，自身不查任何东西；本机无 IP 定位能力，且依赖模型先给 city/date。改造方向：要么接 IP 定位服务，要么直接删掉让模型把参数传给 getWeather。

### CalculatorTool.calculator(expression)
- **JDK 17 下大概率不可用**：Nashorn 从 JDK 15 起被移除，`getEngineByName("JavaScript")` 返回 null，工具返回"计算器不可用（缺少 JavaScript 引擎）"。
- 修复方案 A：pom.xml 加 `org.openjdk.nashorn:nashorn-core` 依赖。
- 修复方案 B（推荐）：换 exp4j 等表达式解析库，避免脚本引擎安全风险。

### MemoTool.memo(command)
- 内存 List 存储，重启即丢，也无 userId 隔离。如需持久化，可改用 Redis（参考 ChatController 的 Redis List 用法）。

## 新增工具的步骤
1. 在 `tool/` 下新建 `XxxTool.java`，标 `@Component` + `@Tool`。
2. 在 `AgentConfig` 构造器注入该 Bean，并加入 `toolCallbackProvider()` 的 `toolObjects(...)`。
3. 在系统提示词里补一句工具说明，引导模型调用。
4. 重启应用生效。
