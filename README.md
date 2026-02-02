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

## 更新插件

当插件有新版本时，可以更新已安装的插件：

```
claude plugin update browser-automation@cc-plugins
claude plugin update cloudless-service@cc-plugins
```

或更新所有插件：

```
claude plugin update --all
```

或使用斜杠命令：

```
/plugin update browser-automation@cc-plugins
/plugin update cloudless-service@cc-plugins
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

## 许可证

MIT License

Copyright (c) 2025 梅思铭
