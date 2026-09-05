---
description: 基于 git 提交生成中文发版日志，写入 versionN-YYYYMMDD.txt
argument-hint: "[起始 commit/标签，可空]"
allowed-tools: Bash, Read, Write
---
按以下步骤生成发版日志：

1. 读取仓库根目录已有的 `version*.txt` 与 `readme-2rounds.txt`，学习其写作风格：中文、带 emoji 小节标题、按组件/功能列出改动、用 `[新增]`/`[修改]`/`[未变]`/`[删除]` 标注、附简短备注。

2. 执行 `git log $ARGUMENTS..HEAD --pretty=format:"%h %s"` 获取范围内的提交摘要。若 `$ARGUMENTS` 为空，用 `git log -- version2-20260509.txt` 找到该文件创建时的提交作为起点。

3. 结合 `git diff <起点>..HEAD --stat` 看改了哪些文件，按组件归类（config / controller / tool / resources / pom.xml）。

4. 按 version1/version2 的风格生成中文发版日志：目录结构变化、主要功能改动、已知坑/备注。

5. 文件名用 `version<N+1>-<日期>.txt`：N 为现有最大版本号 +1，日期取最新提交日期（`git log -1 --format=%cd --date=short`）。写入仓库根目录。
