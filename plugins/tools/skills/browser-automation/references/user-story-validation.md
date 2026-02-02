# 用户故事验证工作流程

本文档专门针对"根据需求文档中的用户故事来做验证"这一场景，提供完整的工作流程。

---

## 什么是用户故事验证？

用户故事（User Story）通常包含：
- **标题**：简短描述功能
- **场景**：具体的测试场景
- **前置条件**：测试开始前的状态
- **操作步骤**：需要执行的操作
- **预期结果**：操作后应该看到的结果

**目标**：使用浏览器自动化工具验证用户故事中的预期结果是否达成。

---

## 标准验证流程

### 阶段 1: 解析用户故事

```bash
# 输入：用户故事文本
"""
用户故事：用户登录

场景：正确输入用户名和密码
前置条件：用户已注册，在登录页面
操作步骤：
  1. 输入用户名 "admin"
  2. 输入密码 "password123"
  3. 点击"登录"按钮
预期结果：登录成功，跳转到首页，显示"欢迎, admin"
"""

# AI 解析后生成验证计划
# 目标 URL: 登录页面
# 输入字段: username="admin", password="password123"
# 操作: 点击登录按钮
# 验证点: URL 变化、显示 "欢迎, admin"
```

### 阶段 2: 执行验证

```bash
# === 步骤 1: 准备环境 ===
list_pages()                              # 检查浏览器状态
select_page(pageId=0)                     # 选择测试页面

# === 步骤 2: 到达前置条件 ===
navigate_page(type="url", url="登录页面URL")  # 打开登录页
wait_for(text="登录", timeout=5000)       # 确认页面加载完成

# === 步骤 3: 执行操作步骤 ===
take_snapshot()                          # 获取页面元素
fill_form(elements=[
    {"uid": "用户名字段uid", "value": "admin"},
    {"uid": "密码字段uid", "value": "password123"}
])
click(uid="登录按钮uid")

# === 步骤 4: 验证预期结果 ===
wait_for(text="欢迎, admin", timeout=5000)  # 等待预期文本

# === 步骤 5: 收集验证证据 ===
take_screenshot(filePath="~/tmp/验证_用户登录_通过.png")  # 截图
evaluate_script(function="""
() => {
    return {
        currentUrl: window.location.href,
        username: document.querySelector('.username')?.textContent,
        isLoggedIn: !!document.querySelector('.user-avatar')
    };
}
""")

# === 步骤 6: 检查错误 ===
list_console_messages(types=["error"])   # 检查是否有错误
```

### 阶段 3: 生成验证报告

```bash
# 验证结果格式：
"""
=== 用户故事验证报告 ===

用户故事：用户登录
场景：正确输入用户名和密码
验证时间：2024-02-02 10:30:00

验证结果：✅ 通过

验证点检查：
  ✅ 登录后跳转到首页 (currentUrl: https://example.com/home)
  ✅ 显示用户名 (username: admin)
  ✅ 用户头像显示 (isLoggedIn: true)

错误检查：
  ✅ 无控制台错误

证据：
  - 截图：~/tmp/验证_用户登录_通过.png
  - 状态数据：{"currentUrl": "...", "username": "admin", "isLoggedIn": true}
"""
```

---

## 用户故事模板映射

### 模板 1: 表单提交类用户故事

**用户故事结构**：
```
场景：[场景描述]
前置条件：[前置条件]
操作步骤：
  1. 填写 [字段1] = [值1]
  2. 填写 [字段2] = [值2]
  3. 点击 [按钮]
预期结果：[预期结果]
```

**验证脚本模板**：
```bash
# 前置条件
navigate_page(type="url", url="[页面URL]")
wait_for(text="[确认页面加载的文本]", timeout=5000)

# 操作步骤
take_snapshot()
fill_form(elements=[
    {"uid": "[字段1的uid]", "value": "[值1]"},
    {"uid": "[字段2的uid]", "value": "[值2]"}
])
click(uid="[按钮的uid]")

# 验证预期结果
wait_for(text="[预期结果中的关键文本]", timeout=5000)
take_screenshot(filePath="~/tmp/验证_[场景名称].png")
```

### 模板 2: 数据展示类用户故事

**用户故事结构**：
```
场景：[场景描述]
前置条件：[前置条件，如已登录]
操作步骤：
  1. 导航到 [页面]
  2. 等待数据加载
预期结果：显示 [数据内容]
```

**验证脚本模板**：
```bash
# 前置条件
navigate_page(type="url", url="[目标页面URL]")
# 如果需要登录，先执行登录流程...

# 操作步骤
wait_for(text="[确认数据加载的文本]", timeout=5000)
take_snapshot()

# 验证预期结果
evaluate_script(function="""
() => {
    const dataElement = document.querySelector('[数据选择器]');
    return {
        displayed: !!dataElement,
        content: dataElement ? dataElement.textContent : null
    };
}
""")
take_screenshot(filePath="~/tmp/验证_[场景名称].png")
```

### 模板 3: 交互操作类用户故事

**用户故事结构**：
```
场景：[场景描述]
前置条件：[前置条件]
操作步骤：
  1. 点击 [元素]
  2. 等待响应
预期结果：[状态变化]
```

**验证脚本模板**：
```bash
# 前置条件
navigate_page(type="url", url="[页面URL]")
wait_for(text="[确认页面加载的文本]", timeout=5000)
take_snapshot()

# 操作步骤
click(uid="[元素的uid]")
wait_for(text="[确认响应的文本]", timeout=5000)

# 验证预期结果
take_snapshot()  # 重新获取页面状态
# 验证状态变化...
take_screenshot(filePath="~/tmp/验证_[场景名称].png")
```

---

## 常见用户故事类型

| 用户故事类型 | 关键验证点 | 推荐工具 |
|-------------|-----------|---------|
| 登录/注册 | 表单提交、跳转、欢迎消息 | fill_form, click, wait_for |
| 数据查询 | 结果列表、数据正确性 | evaluate_script |
| 数据添加 | 表单填写、数据保存、列表更新 | fill_form, click, wait_for |
| 数据编辑 | 编辑按钮、表单预填、更新保存 | click, fill, wait_for |
| 数据删除 | 删除按钮、确认弹窗、列表移除 | click, handle_dialog, wait_for |
| 权限控制 | 未授权访问、错误提示 | navigate_page, wait_for |
| 表单验证 | 错误提示、必填字段 | fill_form, click, wait_for |

---

## 批量验证工作流程

当需要验证多个用户故事时：

```bash
# 用户故事列表
user_stories = [
    {"name": "用户登录", "file": "story_login.md"},
    {"name": "查看数据", "file": "story_view_data.md"},
    {"name": "添加数据", "file": "story_add_data.md"}
]

# 批量验证
for story in user_stories:
    print(f"=== 开始验证: {story['name']} ===")

    # 执行验证脚本
    execute_story_verification(story)

    # 生成报告
    generate_verification_report(story)

    # 清理状态（如需要）
    cleanup_test_state()

print("=== 所有用户故事验证完成 ===")
```

---

## 验证失败处理

### 失败类型 1: 操作失败

```bash
# 问题：点击后没有反应
click(uid="按钮uid")

# 诊断
take_snapshot()  # 检查页面状态
list_console_messages(types=["error"])  # 检查错误
take_screenshot(filePath="~/tmp/失败_按钮无响应.png")

# 可能原因：
# - uid 错误 → 重新 take_snapshot()
# - 元素被禁用 → 检查元素属性
# - 需要等待 → 使用 wait_for()
```

### 失败类型 2: 验证失败

```bash
# 问题：预期结果未出现
wait_for(text="欢迎, admin", timeout=5000)  # 超时

# 诊断
take_screenshot(filePath="~/tmp/失败_未找到预期文本.png")
evaluate_script(function="""
() => {
    return {
        currentUrl: window.location.href,
        pageText: document.body.textContent
    };
}
""")

# 可能原因：
# - 登录失败 → 检查用户名密码
# - 跳转失败 → 检查网络/URL
# - 页面变化 → 检查实际显示的文本
```

---

## 完整示例

### 用户故事：用户搜索商品

```
用户故事：商品搜索

场景：用户搜索存在的商品
前置条件：用户在首页
操作步骤：
  1. 在搜索框输入 "iPhone"
  2. 点击"搜索"按钮
预期结果：
  - 显示搜索结果列表
  - 结果中包含 "iPhone"
  - 显示商品价格
```

### 完整验证脚本

```bash
# === 验证：商品搜索 ===

# 1. 前置条件：到达首页
navigate_page(type="url", url="https://shop.example.com")
wait_for(text="首页", timeout=5000)
print("✅ 前置条件满足：在首页")

# 2. 操作步骤 1：输入搜索关键词
take_snapshot()
fill(uid="搜索框uid", value="iPhone")
print("✅ 操作步骤 1：输入搜索关键词")

# 3. 操作步骤 2：点击搜索按钮
click(uid="搜索按钮uid")
print("✅ 操作步骤 2：点击搜索按钮")

# 4. 验证预期结果
wait_for(text="iPhone", timeout=5000)

# 4.1 验证：显示搜索结果列表
results = evaluate_script(function="""
() => {
    const items = document.querySelectorAll('.product-item');
    return {
        count: items.length,
        hasResults: items.length > 0
    };
}
""")
print(f"✅ 验证点 1：显示搜索结果 (数量: {results['count']})")

# 4.2 验证：结果中包含 "iPhone"
page_text = evaluate_script(function="""
() => {
    return document.body.textContent.includes('iPhone');
}
""")
print(f"✅ 验证点 2：结果包含 'iPhone' ({page_text})")

# 4.3 验证：显示商品价格
prices = evaluate_script(function="""
() => {
    const prices = Array.from(document.querySelectorAll('.price'));
    return prices.map(p => p.textContent.trim());
}
""")
print(f"✅ 验证点 3：显示商品价格 (数量: {len(prices)})")

# 5. 收集验证证据
take_screenshot(filePath="~/tmp/验证_商品搜索_通过.png")

# 6. 生成验证报告
print("""
=== 验证报告 ===
用户故事：商品搜索
场景：用户搜索存在的商品
验证结果：✅ 通过

验证点检查：
  ✅ 显示搜索结果列表
  ✅ 结果中包含 "iPhone"
  ✅ 显示商品价格

证据：
  - 截图：~/tmp/验证_商品搜索_通过.png
""")
```

---

## 最佳实践

1. **解析用户故事时**：明确提取前置条件、操作步骤、预期结果
2. **编写验证脚本时**：添加清晰的注释，对应到用户故事的每一步
3. **验证时**：每个预期结果都应该有对应的验证代码
4. **失败时**：保存截图和状态信息，便于调试
5. **批量验证时**：每个用户故事独立执行，避免相互影响

---

## 快速参考：用户故事 → 工具映射

| 用户故事元素 | 对应工具/方法 |
|-------------|--------------|
| "打开XX页面" | `navigate_page(type="url", url="...")` |
| "在YY输入ZZ" | `fill(uid="...", value="...")` |
| "点击AA按钮" | `click(uid="...")` |
| "等待BB出现" | `wait_for(text="...", timeout=...)` |
| "显示CC文本" | `evaluate_script()` 检查文本 |
| "跳转到DD页面" | 检查 `window.location.href` |
| "显示EE元素" | `evaluate_script()` 检查元素存在 |
| "没有FF错误" | `list_console_messages(types=["error"])` |
