# 基础操作工作流程

本文档提供浏览器自动化的基础操作工作流程，包括搜索、表单填写和多页面操作。

---

## 1. 搜索并提取结果

### 场景
在搜索引擎中搜索关键词并提取结果链接。

### 完整流程

```bash
# 1. 打开搜索页面
navigate_page(type="url", url="https://www.baidu.com")

# 2. 获取快照，找到搜索框
take_snapshot()

# 3. 填写搜索关键词
fill(uid="1_23", value="Python 教程")

# 4. 点击搜索按钮
click(uid="1_24")

# 5. 等待结果加载
wait_for(text="Python", timeout=5000)

# 6. 重新获取快照
take_snapshot()

# 7. 提取搜索结果
evaluate_script(function="""
() => {
    const results = Array.from(document.querySelectorAll('.result'));
    return results.map(r => ({
        title: r.querySelector('h3')?.textContent || '',
        link: r.querySelector('a')?.href || '',
        snippet: r.querySelector('.c-abstract')?.textContent || ''
    }));
}
""")
```

---

## 2. 表单自动填写

### 场景
自动填写注册表单或登录表单。

### 完整流程

```bash
# 1. 打开表单页面
navigate_page(type="url", url="https://example.com/register")

# 2. 获取快照
take_snapshot()

# 3. 批量填写表单
fill_form(elements=[
    {"uid": "1_5", "value": "张三"},
    {"uid": "1_6", "value": "zhangsan@example.com"},
    {"uid": "1_7", "value": "13800138000"},
    {"uid": "1_8", "value": "Password123"}
])

# 4. 同意条款（复选框）
click(uid="1_9")

# 5. 提交表单
click(uid="1_10")

# 6. 等待跳转或成功提示
wait_for(text="注册成功", timeout=5000)

# 7. 截图验证
take_screenshot(filePath="/tmp/register_success.png")
```

### 使用辅助脚本

```bash
# 分析表单并生成填写命令
python3 ~/.claude/skills/browser-automation/scripts/form_filler.py \
    --snapshot /tmp/snapshot.txt \
    --data '{"username": "admin", "password": "123456", "email": "admin@example.com"}' \
    --output /tmp/fill_plan.json
```

---

## 3. 多页面操作

### 场景
在多个标签页之间切换操作。

### 完整流程

```bash
# 1. 打开第一个页面
navigate_page(type="url", url="https://example.com/page1")

# 2. 在后台打开第二个页面
new_page(url="https://example.com/page2", background=true)

# 3. 列出所有页面
list_pages()
# 返回: [{ "pageId": 0, "url": "..." }, { "pageId": 1, "url": "..." }]

# 4. 在第一个页面操作
select_page(pageId=0)
take_snapshot()
click(uid="1_10")

# 5. 切换到第二个页面
select_page(pageId=1)
take_snapshot()
fill(uid="1_5", value="数据")

# 6. 关闭第一个页面
close_page(pageId=0)
```

---

## 工作流程模板

### 通用模板

```bash
# === 阶段 1: 准备 ===
list_pages()
select_page(pageId=0)

# === 阶段 2: 导航 ===
navigate_page(type="url", url="目标网址")
wait_for(text="预期文本", timeout=5000)

# === 阶段 3: 交互 ===
take_snapshot()
# 执行交互操作...
click(uid="xxx")
fill(uid="yyy", value="zzz")

# === 阶段 4: 验证 ===
wait_for(text="验证文本", timeout=3000)
take_screenshot(filePath="/tmp/result.png")

# === 阶段 5: 检查 ===
list_console_messages(types=["error"])
```

---

## 最佳实践

1. **总是使用 wait_for**：关键操作后等待验证条件
2. **定期重新快照**：页面变化后重新获取 uid
3. **检查错误**：定期检查控制台错误
4. **保存截图**：关键步骤保存截图便于调试
5. **使用批量操作**：多个字段使用 fill_form 而非多次 fill
