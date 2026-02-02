---
description: 浏览器自动化测试和交互工具包
---

# Browser Automation

浏览器自动化工具，基于 Chrome DevTools MCP。

## 基本工作流程

`navigate_page → take_snapshot → 交互操作 → 验证结果`

### 核心原则

uid 来自快照，每次页面变化后必须重新 `take_snapshot`

## 支持的场景

- 用户故事验证
- 搜索和数据提取
- 表单自动填写
- 登录认证
- 数据抓取
- 错误调试
- 设备模拟
- AI 视觉分析
