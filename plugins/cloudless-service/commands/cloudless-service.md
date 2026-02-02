---
description: Cloudless 微服务框架服务开发指南
---

# Cloudless Service 开发指南

## 服务开发核心步骤

1. 创建服务类继承 `AbstractService`，使用 `@Service` 注解
2. 声明 `MainDB` 实例进行数据库操作
3. 重写 `init()` 方法初始化数据库表
4. 使用 `@Method` 注解注册方法
5. 通过 `C` 类访问工具类

## 支持的功能

- CRUD 操作
- 服务间调用
- 缓存消息（C.CACHE、C.MQ）
- 文件时间处理（C.FILE、C.TIME）
- 配置管理（C.CONFIG）
- 数据库操作（MainDB）
