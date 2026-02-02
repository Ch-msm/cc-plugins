# CC Plugins

Claude Code 插件商店 - 自定义插件集合。

## 关于

这是一个自定义的 Claude Code 插件市场，包含各种技能和插件。

## 安装

在 Claude Code 中运行以下命令添加此市场：

```
/plugin marketplace add https://github.com/Ch-msm/cc-plugins
```

## 插件列表

当前市场中的插件：

<!-- 在此处添加插件列表 -->

## 开发

### 添加新插件

1. 在 `skills/` 目录下创建新的技能文件夹
2. 在技能文件夹中创建 `SKILL.md` 文件
3. 更新 `.claude-plugin/marketplace.json` 中的插件列表

### 技能模板

```markdown
---
name: my-skill-name
description: 技能描述
---

# 技能名称

技能说明和使用指南。
```

## 许可证

MIT License
