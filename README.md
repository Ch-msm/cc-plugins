# CC Plugins

Claude Code 插件商店 - 自定义插件集合。

## 关于

这是一个自定义的 Claude Code 插件市场，包含各种技能和插件，由梅思铭维护。

## 安装

### 添加市场

在 Claude Code 中运行以下命令添加此市场：

```
/plugin marketplace add https://github.com/Ch-msm/cc-plugins
```

### 安装插件

添加市场后，可以安装单个插件：

```
claude plugin install browser-automation@cc-plugins
claude plugin install cloudless-service@cc-plugins
```

或使用斜杠命令：

```
/plugin install browser-automation@cc-plugins
/plugin install cloudless-service@cc-plugins
```

## 插件列表

### browser-automation

浏览器自动化测试和交互工具包。

**功能**：
- 网页自动化操作
- 表单填写
- UI 验证
- 截图捕获
- 日志记录
- 网络请求监控

**斜杠命令**：
```
/browser-automation 自动化需求描述
```

**MCP 服务器**：
- chrome-devtools：Chrome DevTools MCP 服务器，提供浏览器自动化能力

**使用方式**：
1. 安装插件后，chrome-devtools MCP 服务器会自动启动
2. 使用斜杠命令调用浏览器自动化功能
3. Claude 会根据任务需求自动选择合适的浏览器操作工具

---

### cloudless-service

Cloudless 微服务框架服务开发指南。

**功能**：
- 使用 @Service/@Method 注解创建服务
- 调用项目工具类（C.CONFIG、C.JSON、C.TIME、C.FILE、C.CACHE、C.MQ 等）
- MainDB 数据库操作
- 时序数据库操作
- 文件上传下载

**斜杠命令**：
```
/cloudless-service 开发需求描述
```

**使用方式**：
1. 使用斜杠命令请求 Cloudless 微服务开发指导
2. Claude 会根据你的需求提供相应的代码示例和最佳实践

---

## 开发

### 插件结构

```
plugins/
└── your-plugin/
    ├── .claude-plugin/
    │   └── plugin.json          # 插件元数据
    ├── commands/                 # 斜杠命令（Markdown 文件）
    ├── skills/                   # Agent Skills
    │   └── your-skill/
    │       └── SKILL.md
    ├── agents/                   # 子代理定义
    ├── hooks/                    # 事件钩子
    ├── .mcp.json                # MCP 服务器配置
    └── .lsp.json                # LSP 服务器配置
```

### 添加新插件

1. 在 `plugins/` 目录下创建新的插件文件夹
2. 创建 `.claude-plugin/plugin.json` 文件，定义插件元数据
3. 添加 `commands/`、`skills/` 或 `agents/` 目录
4. 更新 `.claude-plugin/marketplace.json` 中的插件列表
5. 提交并推送到 GitHub

### plugin.json 模板

```json
{
  "name": "your-plugin",
  "version": "1.0.0",
  "description": "插件描述",
  "author": {
    "name": "你的名字"
  }
}
```

### marketplace.json 配置

```json
{
  "plugins": [
    {
      "name": "your-plugin",
      "description": "插件描述",
      "source": "./plugins/your-plugin",
      "mcpServers": "./plugins/your-plugin/.mcp.json"
    }
  ]
}
```

### 斜杠命令模板

`commands/your-command.md`：
```markdown
---
description: 命令描述
argument-hint: 参数提示
---

使用 your-skill 技能完成相关任务。

用户需求：$ARGUMENTS

根据用户提供的参数执行相应的操作。
```

### MCP 服务器配置

`.mcp.json` 文件：
```json
{
  "your-server": {
    "command": "npx",
    "args": ["-y", "your-mcp-package@latest"]
  }
}
```

## 许可证

MIT License

Copyright (c) 2025 梅思铭
