package com.example.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class UserLocationTool {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Tool(description = "Extract city and date (yyyy-MM-dd) from user query. " +
            "If the date is not provided, the current date will be used.")
    public String getUserLocation(
            @ToolParam(description = "City name") String city,
            @ToolParam(description = "Date in yyyy-MM-dd. Leave empty if user didn't mention a specific date.") String date) {

        // 仅当模型没提供日期时，使用服务器今天（作为兜底）
        if (date == null || date.isBlank()) {
            date = LocalDate.now().format(DATE_FMT);
            System.out.println(">>> 模型未提供日期，默认使用今天：" + date);
        }
        return String.format("city:%s, date:%s", city, date);
    }
}