---
name: daily-report
description: Use when generating a project daily report — end of day, before standup, or when asked to summarize today's work. Triggers: 日报, 每日日报, daily report, standup, 站会, 今日总结, 收尾, end-of-day summary.
---

# 每日日报 (Daily Report)

## Overview
生成固定格式的工程日报，便于团队成员同步当日进展。git 能自动采集的部分（今日完成、进展指标）直接填入；人工部分（阻塞与风险、明日计划）留 `[待填]` 占位符，由报告人补充。

## When to Use
- 一天结束、站会前、或被要求"总结今天工作"时
- 触发词：日报 / 每日日报 / daily report / standup / 站会 / 今日总结 / 收尾
- **不适用**：周报（范围不同）、单个 PR 的发版说明（用 `/release-notes`）

## 数据采集
运行以下命令采集今日数据（本机时区今日零点为界）：

```bash
# 1. 今日提交列表（仅当前 git 用户的提交）
git log --since=today --author="$(git config user.name)" --pretty=format:"- %h %s"

# 2. 今日聚合统计：改动文件数 / 增删行数（提交次数用命令 1 的输出行数）
git log --since=today --author="$(git config user.name)" --pretty=tformat: --numstat \
  | awk '{add+=$1; del+=$2; files[$3]++} END {printf "改动 %d 文件，+%d -%d 行\n", length(files), add, del}'

# 3.（可选）项目背景：读 CLAUDE.md 与 version*.txt
```
- 全团队日报（不限作者）：去掉 `--author` 参数。
- 若 `awk` 不可用，退回 `git log --since=today --shortstat` 手工汇总。

## 报告格式（必须严格遵循此模板）

```markdown
# 日报 · {YYYY-MM-DD} · {git user.name}

## 今日完成
{今日提交列表，每条一行：- (auto) <hash> <subject>}
{无提交时：- (无提交) 今日无可记录的提交}

## 进展指标
- 提交 {N} 次 / 改动 {files} 文件 / +{added} -{deleted} 行
{可选 1-2 句：涉及哪些模块/功能}

## 阻塞与风险
- [待填]

## 明日计划
- [待填]
```

## 工作流
1. 运行数据采集命令，拿到今日提交列表与统计。
2. 套用上面的模板，用真实数据填"今日完成"与"进展指标"。
3. "阻塞与风险""明日计划"留 `[待填]`——**不要凭空编造**。
4. 默认输出到对话（便于复制到群/文档）；用户要求保存时，写入 `docs/daily/{YYYY-MM-DD}-{user}.md`。
5. 今日无提交也生成报告，"今日完成"写"(无提交)"，并提示用户手动补充。

## Common Mistakes
- ❌ 编造阻塞/明日计划 → 必须留 `[待填]` 由人填。
- ❌ 改动模板标题或结构 → 格式是团队约定，保持固定。
- ❌ 把内部工具调用日志当进展 → 只记录 user-visible 的提交与改动。
- ❌ 统计时混入生成文件/锁文件 → 用上面 numstat 聚合的净增即可。
