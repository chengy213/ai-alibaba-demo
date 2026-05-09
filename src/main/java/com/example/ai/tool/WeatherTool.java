package com.example.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class WeatherTool {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    // Open-Meteo 免费 API，无需密钥
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast";

    public WeatherTool() {
        this.restClient = RestClient.create();
    }

    @Tool(description = "Get weather for a specific city and date (up to 7 days ahead). " +
            "City and date should come from getUserLocation output.")
    public String getWeather(
            @ToolParam(description = "City name") String city,
            @ToolParam(description = "Date in yyyy-MM-dd format") String date) {

        if (city == null || city.isBlank()) return "❌ City name is missing.";

        try {
            // 1. 解析城市经纬度
            String geoJson = restClient.get()
                    .uri(GEO_URL + "?name=" + city + "&count=1&language=zh")
                    .retrieve().body(String.class);
            JsonNode geoNode = mapper.readTree(geoJson).get("results");
            if (geoNode == null || geoNode.isEmpty()) return "❌ City not found: " + city;

            double lat = geoNode.get(0).get("latitude").asDouble();
            double lon = geoNode.get(0).get("longitude").asDouble();
            String cityName = geoNode.get(0).get("name").asText();

            // 2. 计算查询日期与今天的偏移天数
            LocalDate today = LocalDate.now();
            // 在 getWeather 方法中，获取 queryDate 后加入校验
            LocalDate queryDate;
            try {
                queryDate = LocalDate.parse(date);
            } catch (Exception e) {
                queryDate = today;
            }
            long daysAhead = ChronoUnit.DAYS.between(today, queryDate);
            if (daysAhead < 0) {
                return "⚠️ 无法查询过去的天气，请提供今天或未来的日期。";
            } else if (daysAhead > 6) {
                return "⚠️ 天气预报仅支持未来6天，您查询的日期超出范围。";
            }

            // 3. 调用天气预报接口，获取未来7天数据
            String weatherJson = restClient.get()
                    .uri(WEATHER_URL + "?latitude={lat}&longitude={lon}" +
                                    "&daily=temperature_2m_max,temperature_2m_min,weathercode,windspeed_10m_max,relative_humidity_2m_max" +
                                    "&timezone=auto&forecast_days=7",
                            lat, lon)
                    .retrieve().body(String.class);

            JsonNode daily = mapper.readTree(weatherJson).get("daily");
            String targetDate = queryDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            int idx = findDateIndex(daily.get("time"), targetDate);
            if (idx == -1) return "⚠️ Weather data not available for " + targetDate;

            double maxTemp = daily.get("temperature_2m_max").get(idx).asDouble();
            double minTemp = daily.get("temperature_2m_min").get(idx).asDouble();
            int weatherCode = daily.get("weathercode").get(idx).asInt();
            double windSpeed = daily.get("windspeed_10m_max").get(idx).asDouble();
            int humidity = daily.get("relative_humidity_2m_max").get(idx).asInt();

            return String.format("🌤 %s on %s: %s, high %.1f°C, low %.1f°C, wind %.1f km/h, humidity %d%%",
                    cityName, targetDate, weatherCodeToDesc(weatherCode), maxTemp, minTemp, windSpeed, humidity);
        } catch (Exception e) {
            return "Weather service error: " + e.getMessage();
        }
    }

    private int findDateIndex(JsonNode timeArray, String date) {
        for (int i = 0; i < timeArray.size(); i++) {
            if (timeArray.get(i).asText().equals(date)) return i;
        }
        return -1;
    }

    // 简单的天气码转换（WMO）
    private String weatherCodeToDesc(int code) {
        return switch (code) {
            case 0 -> "Clear";
            case 1,2,3 -> "Partly cloudy";
            case 45,48 -> "Fog";
            case 51,53,55 -> "Drizzle";
            case 61,63,65 -> "Rain";
            case 71,73,75 -> "Snow";
            case 80,81,82 -> "Rain showers";
            case 95,96,99 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
}