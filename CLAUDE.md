# CLAUDE.md

此文件为 Claude Code 在此存储库中工作时的操作指南。

## 项目概述

这是一个自定义的 Claude Code 插件市场（CC Plugins），包含两个插件：
- **tools**：浏览器自动化测试和交互工具包
- **cloudless**：Cloudless 微服务框架服务开发指南

项目采用 Claude Code 插件架构，使用 JSON 配置文件和 Markdown 文档定义插件功能。

## 项目结构

```
cc-plugins/
├── .claude-plugin/
│   └── marketplace.json     # 插件市场定义
├── plugins/
│   ├── tools/               # 工具集合插件
│   │   ├── .claude-plugin/
│   │   │   └── plugin.json  # 插件元数据
│   │   ├── .mcp.json        # MCP 服务器配置
│   │   ├── commands/
│   │   │   └── browser-automation.md
│   │   └── skills/
│   │       └── browser-automation/
│   │           ├── config.json
│   │           ├── SKILL.md
│   │           ├── scripts/
│   │           └── references/
│   ├── cloudless/           # Cloudless 微服务插件
│   │   ├── .claude-plugin/
│   │   │   └── plugin.json
│   │   ├── commands/
│   │   │   └── cloudless-service.md
│   │   └── skills/
│   │       └── cloudless-service/
│   │           ├── config.json
│   │           ├── SKILL.md
│   │           ├── references/
│   │           └── scripts/
└── README.md                # 项目使用说明
```

## 核心概念

### 插件定义
每个插件在 `plugins/<plugin-name>/.claude-plugin/plugin.json` 中定义，包含：
- name：插件名称
- description：描述
- version：版本号
- author：作者信息
- mcpServers：MCP 服务器路径（可选）

### 技能（Skills）
技能定义在 `plugins/<plugin-name>/skills/<skill-name>/` 目录下，包含：
- `config.json`：技能配置
- `SKILL.md`：技能详细文档
- `scripts/`：脚本文件（如有）
- `references/`：参考文档

### 命令（Commands）
命令定义在 `plugins/<plugin-name>/commands/` 目录下，使用 Markdown 文件定义斜杠命令。

### MCP 服务器
MCP 服务器配置在 `.mcp.json` 中，定义外部 MCP 服务的启动命令和参数。

## 常用开发任务

### 添加新插件
1. 在 `plugins/` 目录下创建新插件目录
2. 创建 `.claude-plugin/plugin.json`
3. 根据需要添加 `commands/` 和 `skills/` 子目录
4. 在 `marketplace.json` 的 `plugins` 数组中添加新插件条目

### 修改插件
- 插件配置：编辑 `plugins/<plugin-name>/.claude-plugin/plugin.json`
- 命令定义：编辑 `plugins/<plugin-name>/commands/*.md`
- 技能配置：编辑 `plugins/<plugin-name>/skills/<skill-name>/config.json`
- 技能文档：编辑 `plugins/<plugin-name>/skills/<skill-name>/SKILL.md`

### 更新市场
编辑 `.claude-plugin/marketplace.json`，修改 `metadata.version` 和插件列表。

## 文件规范

- 所有文档使用 **中文**
- JSON 配置使用 2 空格缩进
- 作者名称使用 **梅思铭**
- 遵循现有插件结构和命名约定

## 插件使用方式

用户安装插件后：
1. MCP 服务器会自动启动（如果配置了 mcpServers）
2. 使用斜杠命令调用功能（如 `/browser-automation 需求描述`）
3. Claude 会根据任务自动选择合适的技能和工具

## 重要参考
- [Claude Code 插件文档](https://docs.anthropic.com/en/docs/claude-code/plugin-system)
- README.md 中的用户使用说明
- 各插件的 SKILL.md 详细文档

## 注意事项
- 不添加 third-party 依赖
- 不编写测试文件（除非明确要求）
- 保持最小改动原则
- 所有代码注释和文档使用中文
