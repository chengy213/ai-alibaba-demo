---
description: 启动 Spring Boot 应用（前提：本地 Redis 已在 localhost:6379 运行）
allowed-tools: Bash
---
检查本地 Redis 是否在 6379 监听（如 `redis-cli ping` 应返回 PONG；若环境无 redis-cli，可用端口探测）。若未运行，提示用户先启动 Redis 再继续，不要强行启动应用。

确认 Redis 可达后，执行 `mvn spring-boot:run` 启动应用。应用监听 8080 端口，启动完成后告知用户访问 http://localhost:8080?userId=123 进行对话。

若启动报 Redis 连接错误（如 Unable to connect to localhost:6379），定位为 Redis 未启动，提示用户启动 Redis 后重试，不要去改 application.yml 的连接地址。
