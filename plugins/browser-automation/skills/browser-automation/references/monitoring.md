# 监控与调试

本文档提供浏览器自动化中的监控和调试工作流程，包括网络请求监控、性能分析和错误调试。

---

## 1. 网络请求监控

### 场景
监控页面发出的 API 请求。

### 完整流程

```bash
# 1. 打开页面
navigate_page(type="url", url="https://example.com")

# 2. 列出 XHR/Fetch 请求
list_network_requests(resourceTypes=["xhr", "fetch"])

# 3. 获取具体请求详情
get_network_request(reqid=0)

# 4. 保存请求/响应体
get_network_request(
    reqid=0,
    requestFilePath="/tmp/request_body.json",
    responseFilePath="/tmp/response_body.json"
)

# 5. 清空后重新获取（监控新请求）
navigate_page(type="reload", ignoreCache=true)
list_network_requests()
```

---

## 2. 性能分析

### 场景
分析页面性能。

### 完整流程

```bash
# 1. 开始性能追踪
performance_start_trace(reload=true, autoStop=true, filePath="/tmp/trace.json")

# 2. 等待追踪完成
# 页面会自动重新加载并追踪

# 3. 分析性能洞察
performance_analyze_insight(
    insightSetId="insight-set-id",
    insightName="LCPBreakdown"
)
```

---

## 3. 错误调试流程

### 场景
调试 JavaScript 错误。

### 完整流程

```bash
# 1. 执行操作
navigate_page(type="url", url="https://example.com")
take_snapshot()
click(uid="1_10")

# 2. 检查控制台错误
list_console_messages(types=["error"])

# 3. 获取错误详情
get_console_message(msgid=0)

# 4. 重新获取快照检查状态
take_snapshot()

# 5. 截图保存错误状态
take_screenshot(filePath="/tmp/error_state.png")
```

---

## 调试工作流程

### 步骤 1: 保存快照

```bash
# 保存快照用于分析
take_snapshot(filePath="/tmp/debug_snapshot.txt")
```

### 步骤 2: 检查控制台错误

```bash
# 列出所有错误
list_console_messages(types=["error"])

# 获取具体错误详情
get_console_message(msgid=0)
```

### 步骤 3: 截图当前状态

```bash
# 完整页面截图
take_screenshot(filePath="/tmp/debug_full.png", fullPage=true)

# 或视口截图
take_screenshot(filePath="/tmp/debug_viewport.png")
```

### 步骤 4: 检查网络请求

```bash
# 查看 API 请求
list_network_requests(resourceTypes=["xhr", "fetch"])

# 获取失败请求
get_network_request(reqid=0)
```

### 步骤 5: 使用 JavaScript 调试

```bash
# 检查元素状态
evaluate_script(function="""
() => {
    const elem = document.querySelector('.target');
    return {
        exists: !!elem,
        visible: elem ? elem.offsetParent !== null : false,
        text: elem ? elem.textContent : '',
        disabled: elem ? elem.disabled : null
    };
}
""")
```

---

## 常见调试场景

### 场景 1: 表单提交失败

```bash
# 调试步骤
take_snapshot()

# 1. 检查表单字段
fill_form(elements=[
    {"uid": "1_5", "value": "test"},
    {"uid": "1_6", "value": "test@example.com"}
])

# 2. 检查验证错误
list_console_messages(types=["error"])
take_screenshot(filePath="/tmp/form_before_submit.png")

# 3. 尝试提交
click(uid="1_10")

# 4. 等待并检查结果
wait_for(text="成功", timeout=3000)
take_screenshot(filePath="/tmp/form_after_submit.png")
```

### 场景 2: 页面加载缓慢

```bash
# 调试步骤
navigate_page(type="url", url="https://example.com")

# 1. 检查加载状态
evaluate_script(function="""
() => {
    return {
        readyState: document.readyState,
        domLoaded: document.readyState === 'complete' || document.readyState === 'interactive'
    };
}
""")

# 2. 等待特定元素
wait_for(text="主要内容", timeout=10000)

# 3. 检查网络请求
list_network_requests()

# 4. 截图
take_screenshot(filePath="/tmp/slow_page.png")
```

### 场景 3: 动态内容不更新

```bash
# 调试步骤
take_snapshot()
click(uid="1_20")  # 触发动态加载

# 1. 等待加载指示器
wait_for(text="加载中", timeout=3000)

# 2. 等待内容出现
wait_for(text="新内容", timeout=5000)

# 3. 重新快照
take_snapshot()

# 4. 检查内容
evaluate_script(function="""
() => {
    const content = document.querySelector('.dynamic-content');
    return {
        html: content ? content.innerHTML : 'not found',
        text: content ? content.textContent : 'not found'
    };
}
""")
```

---

## 性能问题诊断

### 检查页面性能

```bash
# 1. 开始性能追踪
performance_start_trace(reload=true, autoStop=true, filePath="/tmp/trace.json")

# 2. 分析结果
performance_analyze_insight(
    insightSetId="insight-set-id",
    insightName="DocumentLatency"
)
```

### 模拟慢速网络

```bash
# 1. 设置慢速网络
emulate(networkConditions="Slow 3G")

# 2. 测试页面加载
navigate_page(type="url", url="https://example.com")

# 3. 恢复正常网络
emulate(networkConditions="No emulation")
```

---

## 日志记录策略

### 结构化日志

```bash
# 创建调试日志
debug_log = []

def log_step(step, data):
    debug_log.append({
        "step": step,
        "data": data,
        "timestamp": datetime.now().isoformat()
    })

# 使用
log_step("navigate", {"url": "https://example.com"})
navigate_page(type="url", url="https://example.com")

log_step("snapshot", {"elements": "count"})
take_snapshot()

# 保存日志
import json
with open("/tmp/debug_log.json", "w") as f:
    json.dump(debug_log, f, indent=2)
```

### 截图时间戳

```bash
from datetime import datetime

def debug_screenshot(name):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = f"/tmp/debug_{name}_{timestamp}.png"
    take_screenshot(filePath=path)
    return path

# 使用
navigate_page(type="url", url="https://example.com")
debug_screenshot("after_navigate")

take_snapshot()
debug_screenshot("after_snapshot")
```

---

## 故障排除清单

### 初步检查

- [ ] 浏览器是否已连接？使用 `list_pages()` 检查
- [ ] 是否选择了正确的页面？使用 `select_page()`
- [ ] uid 是否有效？重新 `take_snapshot()`
- [ ] 元素是否可见？截图检查

### 深入检查

- [ ] 控制台是否有错误？`list_console_messages(types=["error"])`
- [ ] 网络请求是否正常？`list_network_requests()`
- [ ] 页面是否完全加载？`wait_for()` 或检查 `document.readyState`
- [ ] 元素是否在 iframe 中？需要切换上下文

### 高级检查

- [ ] 使用 `evaluate_script` 直接操作 DOM
- [ ] 使用性能追踪检查加载时间
- [ ] 检查 CSS 选择器是否正确
- [ ] 验证页面逻辑（JavaScript 执行）

---

## 常用调试代码片段

### 检查元素是否存在

```bash
evaluate_script(function="""
() => {
    const elem = document.querySelector('.target');
    return {
        exists: !!elem,
        visible: elem ? elem.getBoundingClientRect().height > 0 : false
    };
}
""")
```

### 获取所有可点击元素

```bash
evaluate_script(function="""
() => {
    const clickables = document.querySelectorAll('button, a, [onclick], [role="button"]');
    return Array.from(clickables).map((el, i) => ({
        index: i,
        tag: el.tagName,
        text: el.textContent.trim().substring(0, 50),
        id: el.id,
        class: el.className
    }));
}
""")
```

### 滚动到元素

```bash
evaluate_script(function="""
(selector) => {
    const elem = document.querySelector(selector);
    if (elem) {
        elem.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return true;
    }
    return false;
}
""", args=[{"selector": ".target"}])
```

---

## 最佳实践

1. **保存调试信息** - 总是保存快照和截图
2. **分步验证** - 每步操作后验证结果
3. **使用详细日志** - 记录所有操作和结果
4. **检查控制台** - 定期检查 JavaScript 错误
5. **超时设置合理** - 根据网络情况调整超时时间
