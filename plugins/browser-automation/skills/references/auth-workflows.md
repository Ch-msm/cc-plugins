# 认证场景工作流程

本文档提供浏览器自动化中的认证场景工作流程，包括登录操作和验证码处理。

---

## 1. 登录后的数据操作

### 场景
登录网站后执行操作。

### 完整流程

```bash
# 1. 打开登录页
navigate_page(type="url", url="https://example.com/login")

# 2. 获取快照并填写登录表单
take_snapshot()
fill(uid="1_5", value="username")
fill(uid="1_6", value="password")
click(uid="1_7")

# 3. 等待登录成功（检查 URL 变化或特定元素）
wait_for(text="欢迎", timeout=5000)

# 4. 验证登录状态
evaluate_script(function="""
() => {
    return {
        isLoggedIn: !!document.querySelector('.user-avatar'),
        username: document.querySelector('.username')?.textContent
    };
}
""")

# 5. 执行登录后的操作
navigate_page(type="url", url="https://example.com/dashboard")
# ... 继续操作
```

---

## 2. 验证码处理 ⚠️

### 场景
登录或表单提交时遇到图片验证码，需要 AI 识别验证码内容。

### 重要规则
**当登录页面或其他表单遇到验证码时，必须使用 AI 分析来识别验证码内容。**

验证码图片无法通过常规的 DOM 操作读取，只能通过视觉分析识别。

### 标准工作流程

```bash
# 步骤 1: 打开登录页面
navigate_page(type="url", url="https://example.com/login")

# 步骤 2: 截取页面（包含验证码）
take_screenshot(filePath="~/tmp/login_page.png")

# 步骤 3: 使用 AI 分析验证码
# 使用 Read 工具读取截图，AI 会自动识别验证码内容
# Read("~/tmp/login_page.png")
# AI 返回: "验证码是 6748"

# 步骤 4: 获取页面快照
take_snapshot()

# 步骤 5: 填写登录表单（包含验证码）
fill_form(elements=[
    {"uid": "1_4", "value": "username"},
    {"uid": "1_6", "value": "password"},
    {"uid": "1_8", "value": "6748"}  # AI 识别的验证码
])

# 步骤 6: 提交表单
click(uid="1_10")

# 步骤 7: 验证登录结果
wait_for(text="欢迎", timeout=5000)
```

### 注意事项

| 注意事项 | 说明 |
|---------|------|
| ✅ 必须使用 AI 分析 | 验证码图片无法通过常规方式读取 |
| ✅ 截图要完整 | 确保验证码图片在截图范围内 |
| ✅ 验证码有效期 | 识别后尽快填写，通常 2-5 分钟过期 |
| ❌ 不要猜测 | 错误的验证码会导致登录失败或账户锁定 |
| ❌ 不要刷新 | 刷新后验证码会更新，需要重新识别 |

### 验证码识别失败处理

```bash
# 如果验证码错误，刷新页面重新获取
navigate_page(type="reload", ignoreCache=true)

# 重新截图和识别
take_screenshot(filePath="~/tmp/login_page_v2.png")
# Read("~/tmp/login_page_v2.png")

# 重新填写表单
take_snapshot()
fill_form(elements=[...])
```

### 实际示例

```bash
# 示例：登录带验证码的系统
navigate_page(type="url", url="https://example.com/login")
take_screenshot(filePath="~/tmp/login.png", fullPage=true)

# AI 分析截图，返回：验证码是 "3375"
# Read("~/tmp/login.png")

take_snapshot()
fill_form(elements=[
    {"uid": "1_4", "value": "your_username"},
    {"uid": "1_6", "value": "your_password"},
    {"uid": "1_8", "value": "3375"}
])
click(uid="1_10")
```

---

## 最佳实践

1. **验证登录状态**：登录后通过多个指标验证（URL、元素、Cookie）
2. **处理会话超时**：检测登录状态失效时重新登录
3. **安全存储凭据**：不要在代码中硬编码密码
4. **验证码及时处理**：识别后立即填写，避免过期
