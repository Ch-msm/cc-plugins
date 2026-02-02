# 数据操作工作流程

本文档提供浏览器自动化中的数据操作工作流程，包括批量验证和数据抓取。

---

## 1. 批量数据验证

### 场景
验证一组链接是否可访问。

### 完整流程

```bash
# 链接列表
links = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

# 遍历验证
for link in links:
    navigate_page(type="url", url=link)

    # 检查是否有错误
    list_console_messages(types=["error"])

    # 截图
    take_screenshot(filePath=f"/tmp/verify_{links.index(link)}.png")

    # 记录状态
    evaluate_script(function="""
    () => {
        return {
            url: window.location.href,
            title: document.title,
            status: document.readyState
        };
    }
    """)
```

---

## 2. 数据抓取完整流程

### 场景
从电商网站抓取商品信息。

### 完整流程

```bash
# 1. 打开商品列表页
navigate_page(type="url", url="https://shop.example.com/products")

# 2. 等待页面加载
wait_for(text="商品", timeout=5000)

# 3. 获取快照
take_snapshot()

# 4. 抓取商品数据
evaluate_script(function="""
() => {
    const products = Array.from(document.querySelectorAll('.product-item'));
    return products.map(p => ({
        name: p.querySelector('.name')?.textContent.trim(),
        price: p.querySelector('.price')?.textContent.trim(),
        image: p.querySelector('img')?.src,
        link: p.querySelector('a')?.href
    }));
}
""")

# 5. 翻页（如果需要）
click(uid="1_50")  # 下一页按钮
wait_for(text="商品", timeout=3000)

# 6. 截图保存结果
take_screenshot(filePath="/tmp/products_page1.png", fullPage=true)
```

---

## 最佳实践

1. **结构化数据**：使用 `evaluate_script` 返回结构化的 JSON 数据
2. **错误处理**：批量操作时记录每个项目的状态
3. **分页处理**：使用循环或递归处理多页数据
4. **数据验证**：抓取后验证数据完整性
