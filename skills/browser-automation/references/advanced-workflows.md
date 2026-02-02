# 高级功能工作流程

本文档提供浏览器自动化中的高级功能工作流程，包括设备模拟和 AI 视觉分析。

---

## 1. 模拟设备测试

### 场景
测试移动端适配。

### 完整流程

```bash
# 1. 设置移动设备视口
emulate(viewport={
    "width": 375,
    "height": 667,
    "isMobile": true,
    "hasTouch": true,
    "deviceScaleFactor": 2
})

# 2. 打开页面
navigate_page(type="url", url="https://example.com")

# 3. 截图验证
take_screenshot(filePath="/tmp/mobile_view.png", fullPage=true)

# 4. 测试触摸操作
take_snapshot()
click(uid="1_10")

# 5. 恢复桌面视图
emulate(viewport={"width": 1920, "height": 1080})
navigate_page(type="reload")
```

### 模拟预设

```bash
# 移动设备
emulate(viewport={"width": 375, "height": 667, "isMobile": true, "hasTouch": true})

# 桌面分辨率
emulate(viewport={"width": 1920, "height": 1080})

# 网络限速
emulate(networkConditions="Slow 3G")

# 深色模式
emulate(colorScheme="dark")

# 地理位置模拟
emulate(geolocation={"latitude": 39.9, "longitude": 116.4})
```

---

## 2. AI 视觉分析优先的页面理解

### 场景
在处理复杂或陌生的页面时，优先使用 AI 视觉分析理解页面系统，然后再进行精确交互。

### 为什么优先使用 AI 分析？

| 传统流程 | AI 优先流程 |
|---------|------------|
| take_snapshot 获取文本 | take_screenshot 获取视觉 |
| 只能看到文本和 DOM 结构 | AI 理解布局、设计、交互模式 |
| 需要尝试才能发现问题 | 提前识别视觉和 UX 问题 |
| 适合简单页面 | 适合复杂/陌生页面 |

### 完整流程

```bash
# === 阶段 1: 截图进行 AI 视觉分析 ===
navigate_page(type="url", url="https://example.com")
wait_for(text="关键文本", timeout=5000)

# 截取完整页面
take_screenshot(filePath="/tmp/page_for_analysis.png", fullPage=true)

# AI 分析截图（使用 Read 工具）
# Read("/tmp/page_for_analysis.png")
# AI 将返回：
# - 页面结构分析（布局、分区）
# - UI 设计分析（颜色、字体、间距）
# - 交互元素识别（按钮、表单、导航）
# - 潜在问题（遮挡、重叠、可访问性）

# === 阶段 2: 基于 AI 分析制定策略 ===
# 根据分析结果决定交互方式：
# - 识别目标元素的位置和类型
# - 选择最佳交互路径
# - 预判可能的视觉变化

# === 阶段 3: 精确交互 ===
# 获取快照和 uid
take_snapshot()

# 执行交互
click(uid="识别到的按钮uid")
fill(uid="识别到的输入框uid", value="数据")

# === 阶段 4: 验证结果 ===
# 再次截图让 AI 验证结果
take_screenshot(filePath="/tmp/after_action.png")
# Read("/tmp/after_action.png")
# AI 确认操作效果
```

### 实际示例：分析并操作复杂页面

```bash
# 步骤 1: 打开页面并截图
navigate_page(type="url", url="https://complex-site.com")
take_screenshot(filePath="/tmp/complex_page.png", fullPage=true)

# 步骤 2: AI 分析截图
# 输出分析结果：
# "这是一个电商产品页面：
#  - 顶部：导航栏（首页、分类、购物车、登录）
#  - 中部：产品图片轮播（左侧）、产品信息（右侧）
#  - 右侧面板：价格、库存、购买按钮、数量选择器
#  - 底部：产品详情、相关推荐
#  - 建议：先检查库存，再点击购买"

# 步骤 3: 基于分析执行操作
take_snapshot()

# 检查库存状态
click(uid="库存指示器uid")

# 选择数量
fill(uid="数量输入框uid", value="2")

# 点击购买
click(uid="购买按钮uid")

# 步骤 4: 验证结果
take_screenshot(filePath="/tmp/after_purchase.png")
# AI 确认购物车更新
```

### AI 分析能力

| 分析维度 | AI 能提供的信息 |
|---------|---------------|
| **页面结构** | 布局分区、层级关系、元素分组 |
| **UI 设计** | 配色方案、字体使用、间距规范 |
| **交互模式** | 导航流程、表单布局、按钮样式 |
| **视觉问题** | 元素遮挡、文字重叠、对比度不足 |
| **可访问性** | 颜色对比、标签缺失、键盘导航 |

### 适用场景

✅ **推荐使用 AI 分析的场景**：
- 首次访问的复杂网站
- 需要理解整体 UI/UX 设计
- 怀疑有视觉或布局问题
- 需要验证响应式设计
- 生成页面分析报告

❌ **不需要 AI 分析的场景**：
- 简单的表单填写
- 已知的稳定页面
- 纯数据提取任务
- 高频重复操作

### 与传统流程对比

```bash
# 传统流程（适合简单页面）
navigate_page() → take_snapshot() → click(uid) → verify

# AI 优先流程（适合复杂页面）
navigate_page() → take_screenshot() → AI分析() →
制定策略() → take_snapshot() → click(uid) →
take_screenshot() → AI验证()
```

---

## 高级功能组合

### 场景：移动端 AI 视觉分析

```bash
# 1. 设置移动设备
emulate(viewport={"width": 375, "height": 667, "isMobile": true})

# 2. 打开页面
navigate_page(type="url", url="https://example.com")

# 3. AI 分析移动端布局
take_screenshot(filePath="/tmp/mobile_analysis.png", fullPage=true)
# Read("/tmp/mobile_analysis.png")

# 4. 基于分析执行操作
take_snapshot()
click(uid="AI识别的移动端按钮uid")
```

### 场景：慢速网络下的 AI 分析

```bash
# 1. 模拟慢速网络
emulate(networkConditions="Slow 3G")

# 2. 打开页面并等待
navigate_page(type="url", url="https://example.com")
wait_for(text="关键文本", timeout=15000)

# 3. AI 分析加载状态
take_screenshot(filePath="/tmp/slow_network.png")
# Read("/tmp/slow_network.png")

# 4. 恢复正常网络
emulate(networkConditions="No emulation")
```

---

## 最佳实践

1. **场景适配** - 根据测试目标选择合适的模拟参数
2. **组合使用** - 多个高级功能可以组合使用
3. **AI 优先** - 复杂页面优先使用 AI 视觉分析
4. **验证结果** - 操作后再次截图让 AI 验证效果
5. **参数重置** - 测试完成后恢复默认设置
