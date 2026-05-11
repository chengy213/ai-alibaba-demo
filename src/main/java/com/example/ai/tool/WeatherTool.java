package com.example.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class WeatherTool {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
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
        // ... 现有实现保持不变 ...
        if (city == null || city.isBlank()) return "❌ City name is missing.";
        try {
            String geoJson = restClient.get()
                    .uri(GEO_URL + "?name=" + city + "&count=1&language=zh")
                    .retrieve().body(String.class);
            JsonNode geoNode = mapper.readTree(geoJson).get("results");
            if (geoNode == null || geoNode.isEmpty()) return "❌ City not found: " + city;

            double lat = geoNode.get(0).get("latitude").asDouble();
            double lon = geoNode.get(0).get("longitude").asDouble();
            String cityName = geoNode.get(0).get("name").asText();

            LocalDate today = LocalDate.now();
            LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : today;
            long daysAhead = ChronoUnit.DAYS.between(today, queryDate);
            if (daysAhead < 0 || daysAhead > 6) {
                return "⚠️ Sorry, weather forecast is only available for today and the next 6 days.";
            }

            String weatherJson = restClient.get()
                    .uri(WEATHER_URL + "?latitude={lat}&longitude={lon}" +
                            "&daily=temperature_2m_max,temperature_2m_min,weathercode,windspeed_10m_max,relative_humidity_2m_max" +
                            "&timezone=auto&forecast_days=7", lat, lon)
                    .retrieve().body(String.class);

            JsonNode daily = mapper.readTree(weatherJson).get("daily");
            String targetDate = queryDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
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

    public ToolCallback weatherToolCallback() {
        return ToolCallbacks.from(this)[0];
    }

    private int findDateIndex(JsonNode timeArray, String date) {
        for (int i = 0; i < timeArray.size(); i++) {
            if (timeArray.get(i).asText().equals(date)) return i;
        }
        return -1;
    }

    private String weatherCodeToDesc(int code) {
        return switch (code) {
            case 0 -> "Clear";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
}