package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * 一个模拟的天气查询工具。
 * 在实际生产项目中，您可以替换为真实的 HTTP 调用，例如访问高德、和风天气等第三方 API。
 */
@Component
public class WeatherTool {

    private static final Map<String, String> FAKE_WEATHER = Map.of(
            "北京", "晴, 25°C, 微风",
            "上海", "多云转小雨, 22°C",
            "杭州", "阴, 18°C, 东北风3级",
            "深圳", "雷阵雨, 28°C"
    );

    private final Random random = new Random();

    @Tool(description = "获取中国某个城市的实时天气信息")
    public String getWeather(@ToolParam(description = "城市中文名") String city) {
        if (city == null || city.isBlank()) {
            return "请提供一个有效的城市名称。";
        }
        String key = city.trim();
        if (FAKE_WEATHER.containsKey(key)) {
            return FAKE_WEATHER.get(key);
        }
        String[] conditions = {"晴", "多云", "小雨", "阴", "大风"};
        int temp = 15 + random.nextInt(20);
        return key + "当前天气：" + conditions[random.nextInt(conditions.length)]
                + "，" + temp + "°C。";
    }
}