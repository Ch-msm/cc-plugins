# 枚举与常量

## `MethodStatus`

```java
public enum MethodStatus {
    COMPLETE,
    DEVELOPING,
    NEED_TO_MODIFY
}
```

建议：

- 已稳定使用：`COMPLETE`
- 正在开发：`DEVELOPING`
- 需要返工：`NEED_TO_MODIFY`

## `MethodNature`

```java
public enum MethodNature {
    PUBLIC,
    PROTECTED,
    CONTROLLED
}
```

语义：

- `PUBLIC`：无需登录
- `PROTECTED`：需要登录，不校验接口授权
- `CONTROLLED`：需要登录并校验接口授权

## `ServerName`

常用服务名枚举：

```java
ServerName.USER_CENTER
ServerName.FILE_SERVER
ServerName.LOG_CENTER
ServerName.CONFIG_CENTER
ServerName.PUSH
```

用途：

- 服务间调用时表达已有平台服务
- 获取标准服务名与端口

示例：

```java
String url = "/" + ServerName.USER_CENTER + "/Authenticate/getLoginUser";
```

## `Module`

`common` 中确实有 `Module` 枚举，但它表示框架内置工具模块：

```java
Module.CONFIG
Module.JSON
Module.TIME
Module.TEXT
Module.CACHE
Module.IMAGE
Module.GEO
Module.MQ
```

注意：

- 不能写 `Module.USER`、`Module.ORDER`
- `@Method` 没有 `module` 属性
- `@Service.module` 是自由字符串，不是这个枚举

正确示例：

```java
@Service(value = "用户管理", author = "梅思铭", date = "2026-03-09", module = "系统管理")
public class UserService extends AbstractService {
}
```

## `common` 内置常量

框架本身暴露的常量较少，常用的有：

```java
Constant.Cache.DATA_UPDATE_KEYS_PREFIX
Constant.Subscribe.PUSH_SUBSCRIBE_MESSAGE
```

用途：

- 数据变更订阅 key 前缀
- 推送服务订阅频道

## 业务常量建议

业务项目应自己维护常量类，例如：

```java
public class BizConstant {
    private BizConstant() {
    }

    public static class Cache {
        public static final String USER_INFO = "user:info:";
    }

    public static class QueueKafka {
        public static final String USER_CHANGE = "user:change";
    }

    public static class Push {
        public static final String REPORT_READY = "report:ready";
    }
}
```

## 推荐做法

1. 接口状态使用 `MethodStatus`
2. 接口权限使用 `MethodNature`
3. 平台内置服务优先用 `ServerName`
4. 业务分组和业务常量自己定义，不要把 `Module` 当业务枚举用
