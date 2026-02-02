# 枚举与常量完整指南

## 目录

- [MethodStatus - 方法状态](#methodstatus---方法状态)
- [MethodNature - 方法性质](#methodnature---方法性质)
- [ServerName - 服务名称](#servername---服务名称)
- [Module - 模块定义](#module---模块定义)
- [Constant - 系统常量](#constant---系统常量)
- [自定义常量](#自定义常量)

---

## MethodStatus - 方法状态

### 枚举值说明

```java
public enum MethodStatus {
    COMPLETE,        // 已完成（生产可用）
    DEVELOPING,      // 开发中（不可用）
    NEED_TO_MODIFY   // 需要修改（需要改进）
}
```

### 使用示例

```java
// 已完成的方法
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
public User getUser(String id) {
    return DB.use().eq(User::getId, id).get();
}

// 开发中的方法
@Method(value = "批量导入", status = MethodStatus.DEVELOPING)
public void batchImport(List<User> list) {
    throw new AppRuntimeException("功能开发中，敬请期待");
}

// 需要修改的方法
@Method(value = "数据统计", status = MethodStatus.NEED_TO_MODIFY)
public Statistics getStatistics() {
    // 功能可用但需要优化
    // TODO: 需要优化查询性能
    return DB.use().query();
}
```

### 状态选择指南

| 状态 | 使用场景 | 接口可用性 |
|------|---------|-----------|
| `COMPLETE` | 功能已完成，测试通过 | ✅ 可用 |
| `DEVELOPING` | 正在开发，未完成 | ❌ 不可用 |
| `NEED_TO_MODIFY` | 功能可用但需要重构 | ⚠️ 可用但待优化 |

### 最佳实践

```java
// ✅ 推荐：明确标识开发状态
@Method(value = "用户注册", status = MethodStatus.COMPLETE)
public void register(User user) {
    // 完整的实现
}

// ✅ 推荐：开发中先抛出异常
@Method(value = "AI分析", status = MethodStatus.DEVELOPING)
public AIResult analyze(String data) {
    throw new AppRuntimeException("AI分析功能开发中");
}

// ✅ 推荐：需要修改时添加TODO
@Method(value = "导出报表", status = MethodStatus.NEED_TO_MODIFY)
public String exportReport() {
    // TODO: 需要优化大数据量导出性能
    return File.EXECL.export("报表", items, list);
}
```

---

## MethodNature - 方法性质

### 枚举值说明

```java
public enum MethodNature {
    PUBLIC,      // 公开接口（所有人可访问）
    PROTECTED,   // 受保护接口（仅服务内部调用）
    CONTROLLED   // 受控接口（需要权限验证）
}
```

### 使用示例

```java
// 公开接口（默认）
@Method(value = "用户登录", nature = MethodNature.PUBLIC)
public LoginResult login(String username, String password) {
    // 所有人都可以访问
}

// 受保护接口
@Method(value = "数据初始化", nature = MethodNature.PROTECTED)
public void initData() {
    // 只能通过服务间调用或本地调用
    // 外部HTTP请求无法访问
}

// 受控接口
@Method(value = "删除用户", nature = MethodNature.CONTROLLED)
public void deleteUser(String userId) {
    // 框架会自动验证权限
}
```

### 访问权限说明

| 性质 | 访问控制 | 使用场景 |
|------|---------|----------|
| `PUBLIC` | 无限制 | 登录、注册、查询公开数据 |
| `PROTECTED` | 仅内部 | 定时任务、数据同步、内部方法 |
| `CONTROLLED` | 需要权限 | 删除、修改、导出敏感数据 |

### 最佳实践

```java
// ✅ 推荐：公开接口
@Method(value = "用户注册", nature = MethodNature.PUBLIC)
public void register(User user) { }

@Method(value = "查询公告", nature = MethodNature.PUBLIC)
public List<Notice> getNotices() { }

// ✅ 推荐：受控接口
@Method(value = "删除用户", nature = MethodNature.CONTROLLED)
public void deleteUser(String userId) { }

@Method(value = "导出数据", nature = MethodNature.CONTROLLED)
public String exportData() { }

// ✅ 推荐：受保护接口
@Method(value = "数据同步", nature = MethodNature.PROTECTED)
@Subscribe("data.sync")
public void syncData(String json) { }

@Method(value = "清理缓存", nature = MethodNature.PROTECTED)
public void cleanCache() { }
```

---

## ServerName - 服务名称

### 枚举值说明

```java
public enum ServerName {
    USER_CENTER("user-center", "用户中心", 9002),
    FILE_SERVER("file-server", "文件服务", 9007),
    LOG_CENTER("log-center", "日志中心", 9008),
    CONFIG_CENTER("config-center", "配置中心", 9009),
    ASSETS("assets", "资产中心", 9010),
    PUSH("push", "推送服务", 9011),
    IM("im", "IM服务", 9021),
    TASK_SCHEDULING("task-scheduling", "任务调度中心", 9022),
    // ... 更多服务
}
```

### 使用示例

```java
// 获取服务名称
String serviceName = ServerName.USER_CENTER.toString();  // "user-center"

// 获取服务中文名
String alias = ServerName.USER_CENTER.getAlisa();  // "用户中心"

// 获取服务端口
int port = ServerName.USER_CENTER.getPort();  // 9002

// 在URL中使用
String url = "http://" + ServerName.USER_CENTER + "/User/getUser";
// 结果: "http://user-center/User/getUser"
```

### 服务间调用

```java
// 创建服务依赖类
public class UserCenter {
    public static User getUser(String userId) {
        var json = ClientStub.send(
            "/" + ServerName.USER_CENTER + "/User/getUser",
            new RequestBody(),
            C.JSON.toJson(userId)
        );
        return C.JSON.fromJson(json, User.class);
    }

    public static LoginUser getLoginUser(String token) {
        var json = ClientStub.send(
            "/" + ServerName.USER_CENTER + "/Authenticate/getLoginUser",
            new RequestBody(),
            C.JSON.toJson(Map.of("token", token))
        );
        return C.JSON.fromJson(json, LoginUser.class);
    }
}

// 使用
User user = UserCenter.getUser(userId);
LoginUser user = UserCenter.getLoginUser(token);
```

### 推送频道命名

```java
public class Constant {
    public static final class Push {
        // 使用服务名作为频道前缀
        public static final String IM_MESSAGE = ServerName.IM.getName() + ":message";
        public static final String IM_CHAT = ServerName.IM.getName() + ":chat";

        public static final String USER_ONLINE = ServerName.USER_CENTER.getName() + ":online";
        public static final String DATA_CHANGE = "data:change";
    }
}

// 使用
C.PUSH.push(Constant.Push.IM_MESSAGE, message, userIds);
```

---

## Module - 模块定义

### 模块分类

```java
public enum Module {
    // 系统模块
    SYSTEM("系统"),

    // 业务模块
    USER("用户管理"),
    ORDER("订单管理"),
    PRODUCT("产品管理"),
    ASSET("资产管理"),

    // 数据模块
    DATA("数据管理"),
    REPORT("报表统计"),
    ANALYSIS("数据分析"),

    // 工具模块
    FILE("文件管理"),
    MESSAGE("消息管理"),
    WORKFLOW("工作流");

    private final String name;
}
```

### 使用示例

```java
// 在服务注解中引用模块
@Service(
    value = "用户管理",
    author = "梅思铭",
    date = "2025-01-08",
    module = Module.USER
)
public class UserService extends AbstractService {
    // ...
}

// 分组显示方法
@Method(value = "添加用户", module = Module.USER)
public void addUser(User user) { }

@Method(value = "删除订单", module = Module.ORDER)
public void deleteOrder(String orderId) { }
```

---

## Constant - 系统常量

### 缓存常量

```java
public class Constant {
    public static class Cache {
        // 数据更新key前缀
        public static final String DATA_UPDATE_KEYS_PREFIX = "subscribe:data_update:";

        // Token缓存
        public static final String TOKEN_PREFIX = "TOKEN:";

        // 用户信息缓存
        public static final String USER_INFO_PREFIX = "user:info:";

        // 配置缓存
        public static final String CONFIG_PREFIX = "config:";
    }
}

// 使用
String cacheKey = Constant.Cache.USER_INFO_PREFIX + userId;
C.CACHE.set(cacheKey, user, 1800);
```

### 订阅常量

```java
public class Constant {
    public static class Subscribe {
        // 推送订阅消息
        public static final String PUSH_SUBSCRIBE_MESSAGE = "push:subscribe:message";

        // 数据表变更
        public static final String DATA_TABLE = "data-table";

        // 设备状态
        public static final String DEVICE_STATUS = "device-status";
    }
}

// 使用
@Subscribe(Constant.Subscribe.DATA_TABLE)
public void handleTableChange(String dataId) {
    // 处理数据表变更
}
```

### 队列常量

```java
public class Constant {
    public static final class QueueKafka {
        // 用户更新
        public static final String USER_UPDATE = "user-update";

        // 数据变更
        public static final String DATA_CHANGE = "data-change";

        // 设备状态
        public static final String DEVICE_STATUS = "device-status";

        // 审批提交
        public static final String APPROVAL_SUBMIT = "approval-submit";

        // 报表生成
        public static final String REPORT_GENERATED = "report-generated";
    }
}

// 使用
C.MQ.publish(Constant.QueueKafka.USER_UPDATE, C.JSON.toJson(user));

@Subscribe(Constant.QueueKafka.USER_UPDATE)
public void handleUserUpdate(String json) {
    User user = C.JSON.fromJson(json, User.class);
}
```

### 推送常量

```java
public class Constant {
    public static final class Push {
        // IM消息
        public static final String IM_MESSAGE = "im:message";
        public static final String IM_CHAT = "im:chat";

        // 在线状态
        public static final String ONLINE_STATUS = "user:online:status";

        // 报表下载
        public static final String REPORT_AUTO_DOWNLOAD = "report:auto:download";

        // 数据变更
        public static final String DATA_CHANGE = "data:change";

        // 告警
        public static final String ALARM = "alarm:notification";

        // 配置变更
        public static final String CONFIG_CHANGE = "config:change";
    }
}

// 使用
C.PUSH.push(Constant.Push.IM_MESSAGE, message, userIds);
C.PUSH.push(Constant.Push.ONLINE_STATUS, status, userId);
```

---

## 自定义常量

### 创建常量类

```java
/**
 * 业务常量
 */
public class BizConstant {

    private BizConstant() {}

    /**
     * 用户状态
     */
    public static final class UserStatus {
        public static final int ACTIVE = 1;      // 正常
        public static final int DISABLED = 0;    // 禁用
        public static final int LOCKED = 2;      // 锁定
    }

    /**
     * 订单状态
     */
    public static final class OrderStatus {
        public static final int PENDING = 0;     // 待支付
        public static final int PAID = 1;        // 已支付
        public static final int SHIPPED = 2;     // 已发货
        public static final int COMPLETED = 3;   // 已完成
        public static final int CANCELLED = 4;   // 已取消
    }

    /**
     * 文件类型
     */
    public static final class FileType {
        public static final String IMAGE = "image";
        public static final String DOCUMENT = "document";
        public static final String VIDEO = "video";
        public static final String AUDIO = "audio";
    }

    /**
     * 缓存过期时间
     */
    public static final class CacheExpire {
        public static final long MINUTE_5 = 300L;
        public static final long MINUTE_30 = 1800L;
        public static final long HOUR_1 = 3600L;
        public static final long HOUR_24 = 86400L;
        public static final long WEEK_1 = 604800L;
    }
}
```

### 使用自定义常量

```java
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "添加用户", status = MethodStatus.COMPLETE)
    public void addUser(User user) {
        // 设置默认状态
        user.setStatus(BizConstant.UserStatus.ACTIVE);

        // 插入数据库
        DB.use().insert(user);

        // 缓存用户信息（30分钟）
        String cacheKey = "user:info:" + user.getId();
        C.CACHE.set(cacheKey, user, BizConstant.CacheExpire.MINUTE_30);
    }

    @Method(value = "禁用用户", status = MethodStatus.COMPLETE)
    public void disableUser(String userId) {
        DB.use()
            .eq(User::getId, userId)
            .update(User::getStatus, BizConstant.UserStatus.DISABLED);

        // 清除缓存
        C.CACHE.del("user:info:" + userId);
    }
}
```

### 创建枚举常量

```java
/**
 * 用户状态枚举
 */
public enum UserStatus {
    DISABLED(0, "禁用"),
    ACTIVE(1, "正常"),
    LOCKED(2, "锁定");

    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    // 根据值获取枚举
    public static UserStatus fromValue(int value) {
        for (UserStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
```

### 使用枚举常量

```java
@Service(value = "订单服务", author = "梅思铭", date = "2025-01-08")
public class OrderService extends AbstractService {

    @Method(value = "创建订单", status = MethodStatus.COMPLETE)
    public void createOrder(Order order) {
        // 设置订单状态
        order.setStatus(OrderStatus.PENDING.getValue());
        order.setStatusDesc(OrderStatus.PENDING.getDesc());

        DB.use().insert(order);
    }

    @Method(value = "支付订单", status = MethodStatus.COMPLETE)
    public void payOrder(String orderId) {
        Order order = DB.use().eq(Order::getId, orderId).get();

        // 更新订单状态
        order.setStatus(OrderStatus.PAID.getValue());
        order.setStatusDesc(OrderStatus.PAID.getDesc());

        DB.use().update(order);

        // 推送通知
        C.PUSH.push("order:paid", order, order.getUserId());
    }

    @Method(value = "查询订单", status = MethodStatus.COMPLETE)
    public List<Order> getOrders(int status) {
        // 根据状态查询
        return DB.use()
            .eq(Order::getStatus, status)
            .query();
    }
}
```

---

## 常量命名规范

### 命名规则

```java
// ✅ 推荐：全大写，下划线分隔
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_CHARSET = "UTF-8";
public static final long CACHE_EXPIRE_TIME = 3600L;

// ❌ 不推荐：驼峰命名
public static final int maxRetryCount = 3;
public static final String defaultCharset = "UTF-8";

// ✅ 推荐：使用内部类分组
public class Constant {
    public static class User {
        public static final int STATUS_ACTIVE = 1;
        public static final int STATUS_DISABLED = 0;
    }

    public static class Cache {
        public static final long EXPIRE_SHORT = 300L;
        public static final long EXPIRE_LONG = 86400L;
    }
}

// ✅ 推荐：枚举使用大写
public enum UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED
}
```

### 常量组织

```java
/**
 * 系统常量
 */
public class SystemConstant {
    // 按功能分组
    public static final class Http {
        public static final int DEFAULT_PORT = 8080;
        public static final int TIMEOUT = 30000;
    }

    public static final class Cache {
        public static final String PREFIX = "app:";
        public static final long EXPIRE_DEFAULT = 3600L;
    }

    public static final class Queue {
        public static final String USER_UPDATE = "user-update";
        public static final String DATA_CHANGE = "data-change";
    }

    // 按模块分组
    public static final class User {
        public static final int STATUS_ACTIVE = 1;
        public static final int STATUS_DISABLED = 0;
    }

    public static final class Order {
        public static final int STATUS_PENDING = 0;
        public static final int STATUS_PAID = 1;
    }
}
```

---

## 最佳实践总结

### 1. 使用枚举代替魔法数字

```java
// ❌ 不推荐
if (user.getStatus() == 1) {
    // ...
}

// ✅ 推荐
if (user.getStatus() == UserStatus.ACTIVE.getValue()) {
    // ...
}

// 或者使用枚举直接比较
UserStatus status = UserStatus.fromValue(user.getStatus());
if (status == UserStatus.ACTIVE) {
    // ...
}
```

### 2. 常量集中管理

```java
// ✅ 推荐：创建专门的常量类
public class BizConstant {
    public static class UserStatus { }
    public static class OrderStatus { }
    public static class CacheExpire { }
}

// ✅ 推荐：按功能创建Constant类
public class Constant {
    public static class QueueKafka { }
    public static class Push { }
    public static class Subscribe { }
}
```

### 3. 使用有意义的名称

```java
// ❌ 不推荐
public static final int N = 100;
public static final String S = "success";

// ✅ 推荐
public static final int MAX_RETRY_COUNT = 100;
public static final String STATUS_SUCCESS = "success";
```

### 4. 添加注释说明

```java
/**
 * 业务常量
 */
public class BizConstant {
    /**
     * 用户状态
     */
    public static class UserStatus {
        /** 正常 */
        public static final int ACTIVE = 1;

        /** 禁用 */
        public static final int DISABLED = 0;

        /** 锁定 */
        public static final int LOCKED = 2;
    }
}
```
