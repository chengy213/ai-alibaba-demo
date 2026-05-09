package com.example.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot 启动类。
 * 通过 WebMvcConfigurer 将访问根路径 “/” 的请求转发到 index.html，
 * 前端在页面内通过 URL query string 获取 userId。
 */
@SpringBootApplication
public class AiAlibabaDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAlibabaDemoApplication.class, args);
    }

    /**
     * 将所有 “/” 请求直接转发到静态资源 /index.html，
     * 浏览器地址栏依然显示 localhost:8080?userId=xxx
     */
    @Bean
    public WebMvcConfigurer forwardRootToIndex() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                // forward 模式下，浏览器地址不会改变，前端 JS 可以读取 query string
                registry.addViewController("/").setViewName("forward:/index.html");
            }
        };
    }
}