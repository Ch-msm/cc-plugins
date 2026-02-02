# 决策导航指南

本文档帮助 AI 根据用户需求快速找到正确的工作流程和工具。

---

## 快速决策表

根据用户需求类型，快速定位到对应的工作流程：

| 用户需求类型 | 使用的工作流程 | 关键工具 | 文件位置 |
|-------------|---------------|---------|---------|
| "帮我搜索/查找" | 搜索并提取结果 | navigate_page, fill, click, wait_for | basic-workflows.md → 1 |
| "填写表单/注册/提交" | 表单自动填写 | take_snapshot, fill_form | basic-workflows.md → 2 |
| "登录网站" | 登录后的数据操作 | fill, click, wait_for | auth-workflows.md → 1 |
| **"登录有验证码"** | **验证码处理 ⚠️** | **take_screenshot, AI 分析** | auth-workflows.md → 2 |
| "抓取数据/爬虫/提取信息" | 数据抓取完整流程 | evaluate_script | data-workflows.md → 2 |
| "批量验证/测试多个链接" | 批量数据验证 | 循环 + navigate_page | data-workflows.md → 1 |
| "页面有问题/报错/调试" | 错误调试流程 | list_console_messages | monitoring.md → 3 |
| "测试移动端/手机" | 模拟设备测试 | emulate | advanced-workflows.md → 1 |
| **"复杂页面/看不懂布局"** | **AI 视觉分析优先** | **take_screenshot, Read** | advanced-workflows.md → 2 |
| "监控API/网络请求" | 网络请求监控 | list_network_requests | monitoring.md → 1 |
| "性能测试/慢页面" | 性能分析 | performance_start_trace | monitoring.md → 2 |
| "多标签页/切换页面" | 多页面操作 | new_page, select_page | basic-workflows.md → 3 |

---

## 决策流程图

```
用户请求
    ↓
需要登录？
├─ 是 → 有验证码？
│       ├─ 是 → auth-workflows.md#2-验证码处理
│       └─ 否 → auth-workflows.md#1-登录后的数据操作
└─ 否 → 需要抓取数据？
        ├─ 是 → data-workflows.md
        │        ├─ 批量操作？→ data-workflows.md#1-批量数据验证
        │        └─ 单次抓取？→ data-workflows.md#2-数据抓取完整流程
        └─ 否 → 页面复杂？
                 ├─ 是 → advanced-workflows.md#2-AI视觉分析优先
                 └─ 否 → basic-workflows.md
```

---

## 关键决策点

### 决策 1: 是否需要 AI 视觉分析？

使用 AI 视觉分析的场景：
- ✅ 首次访问的复杂网站
- ✅ 用户说"看不懂"、"分析页面"
- ✅ 页面布局复杂或非标准
- ✅ 需要理解整体 UI/UX 设计
- ✅ 怀疑有视觉或布局问题

不需要 AI 分析的场景：
- ❌ 简单的表单填写
- ❌ 已知的稳定页面
- ❌ 纯数据提取任务
- ❌ 高频重复操作

### 决策 2: 是否需要处理验证码？

判断标准：
- 用户明确提到"验证码"
- 登录页面有图片验证码区域
- 提交表单后返回验证码错误

**重要**：验证码必须使用 AI 视觉分析识别，不能猜测！

### 决策 3: 操作失败后的处理

```
操作失败
    ↓
检查错误类型
    ├─ uid 错误 → 重新 take_snapshot
    ├─ 元素未找到 → wait_for 等待 或 检查选择器
    ├─ 超时 → 增加 timeout 或 检查网络
    ├─ 验证码 → 使用 AI 分析识别
    └─ 控制台错误 → list_console_messages 查看
```

---

## 工作流程组合示例

### 场景 1: 登录带验证码的系统并抓取数据

```bash
# === 阶段 1: 认证 (auth-workflows.md) ===
navigate_page(type="url", url="https://example.com/login")
take_screenshot(filePath="~/tmp/login.png")
# AI 识别验证码
take_snapshot()
fill_form(elements=[...])  # 包含验证码
click(uid="登录按钮")
wait_for(text="欢迎", timeout=5000)

# === 阶段 2: 导航到数据页面 ===
navigate_page(type="url", url="https://example.com/data")

# === 阶段 3: 数据抓取 (data-workflows.md) ===
take_snapshot()
evaluate_script(function="""
() => {
    // 提取数据逻辑
}
""")

# === 阶段 4: 验证 (monitoring.md) ===
list_console_messages(types=["error"])
take_screenshot(filePath="~/tmp/result.png")
```

### 场景 2: 移动端 AI 视觉分析

```bash
# === 阶段 1: 设置移动设备 (advanced-workflows.md#1) ===
emulate(viewport={"width": 375, "height": 667, "isMobile": true})

# === 阶段 2: AI 分析页面 (advanced-workflows.md#2) ===
navigate_page(type="url", url="https://example.com")
take_screenshot(filePath="~/tmp/mobile_analysis.png", fullPage=true)
# AI 分析移动端布局

# === 阶段 3: 执行操作 ===
take_snapshot()
click(uid="AI识别的按钮uid")
```

---

## 参数选择指南

### includeSnapshot 参数

| 场景 | 推荐值 | 原因 |
|------|--------|------|
| click 后立即操作 | `true` | 自动获取新快照，节省一次调用 |
| click 后不立即操作 | `false` | 按需获取，避免冗余 |
| 连续多次 fill | `false` | 页面不变化，不需要重新快照 |
| 不确定页面是否变化 | `true` | 安全起见，自动获取新状态 |

### timeout 参数

| 操作类型 | 推荐超时 | 说明 |
|---------|---------|------|
| wait_for 文本出现 | 5000ms | 大多数页面 3-5 秒加载完成 |
| wait_for 图片加载 | 10000ms | 图片可能需要更长时间 |
| 等待动态内容 | 8000ms | 考虑 AJAX 加载时间 |
| 等待页面跳转 | 5000ms | 页面跳转通常较快 |

---

## 常见错误速查

| 错误信息 | 原因 | 快速解决 |
|---------|------|---------|
| `Element with uid not found` | uid 失效 | `take_snapshot()` 重新获取 |
| `Timeout waiting for element` | 元素未加载 | 增加 `timeout` 或检查选择器 |
| `No element matching selector` | 选择器错误 | 检查快照中的 uid 是否正确 |
| "验证码错误" | 验证码识别错误 | 刷新页面重新用 AI 识别 |
| "页面没反应" | 可能需要等待 | 使用 `wait_for` 等待关键文本 |

---

## 反模式：避免这些做法

### ❌ 错误做法

```bash
# 错误 1: 直接使用猜测的 uid
click(uid="1_10")  # uid 可能不正确

# 错误 2: 页面变化后不重新快照
navigate_page(type="url", url="https://example.com")
click(uid="1_5")  # uid 已失效

# 错误 3: 不等待加载完成
navigate_page(type="url", url="https://example.com")
take_snapshot()  # 页面可能未加载完成

# 错误 4: 验证码手动猜测
fill(uid="1_8", value="1234")  # 验证码错误会导致失败

# 错误 5: 没有验证操作结果
click(uid="1_10")  # 点击后没有检查是否成功
```

### ✅ 正确做法

```bash
# 正确 1: 先快照再交互
take_snapshot()
click(uid="快照中的uid")

# 正确 2: 页面变化后重新快照
navigate_page(type="url", url="https://example.com")
take_snapshot()  # 获取新的 uid
click(uid="新的uid")

# 正确 3: 等待关键文本
navigate_page(type="url", url="https://example.com")
wait_for(text="页面加载完成", timeout=5000)
take_snapshot()

# 正确 4: 使用 AI 分析验证码
take_screenshot(filePath="~/tmp/login.png")
# AI 识别验证码后填写

# 正确 5: 验证操作结果
click(uid="1_10")
wait_for(text="操作成功", timeout=3000)  # 确认操作成功
```

---

## 快速开始检查清单

### 开始任何任务前，先回答：

- [ ] 浏览器是否已连接？`list_pages()`
- [ ] 是否选择了正确的页面？`select_page()`
- [ ] 用户需求属于哪一类？（查决策表）
- [ ] 是否需要登录？有验证码吗？
- [ ] 页面是否复杂？需要 AI 分析吗？

### 执行任务时，注意：

- [ ] 每次页面变化后重新 `take_snapshot()`
- [ ] 关键操作后使用 `wait_for()` 等待
- [ ] 定期检查 `list_console_messages(types=["error"])`
- [ ] 关键步骤截图保存
