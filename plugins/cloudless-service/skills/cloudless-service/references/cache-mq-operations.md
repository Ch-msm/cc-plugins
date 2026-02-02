# 缓存、消息队列与推送操作指南

## 目录

- [C.CACHE - 缓存操作](#ccache---缓存操作)
- [C.MQ - 消息队列](#cmq---消息队列)
- [C.PUSH - 消息推送](#cpush---消息推送)

---

## C.CACHE - 缓存操作

Redis缓存操作，支持单机和集群模式。

### 字符串缓存

```java
// 设置缓存
C.CACHE.set("user:1001", userData);
C.CACHE.set("key", "value", 3600);  // 带过期时间（秒）

// 获取缓存
String value = C.CACHE.get("key");
User user = C.CACHE.get("user:1001", User.class);

// 删除缓存
C.CACHE.del("key");
C.CACHE.del("key1", "key2", "key3");  // 批量删除

// 检查是否存在
boolean exists = C.CACHE.exists("key");

// 设置过期时间
C.CACHE.expire("key", 3600);  // 秒

// 获取剩余过期时间
long ttl = C.CACHE.ttl("key");  // 返回秒数，-1表示永不过期，-2表示key不存在
```

### Hash操作

```java
// 设置Hash
C.CACHE.setHash("TOKEN", token, C.JSON.toJson(user));
C.CACHE.setHash("session", sessionId, userData, 1800);  // 30分钟过期

// 获取Hash
String json = C.CACHE.getHash("TOKEN", token);
User user = C.CACHE.getHash("TOKEN", token, User.class);

// 删除Hash
C.CACHE.delHash("TOKEN", token);

// 检查Hash字段是否存在
boolean exists = C.CACHE.hexists("TOKEN", token);

// 获取所有Hash字段
Map<String, String> allTokens = C.CACHE.hgetAll("TOKEN");
```

### 分布式锁

```java
// 尝试获取锁
if (C.CACHE.tryLock("lock:key", 10)) {  // 10秒过期
    try {
        // 执行业务逻辑
        doSomething();
    } finally {
        // 释放锁
        C.CACHE.delLock("lock:key");
    }
}

// 检查锁状态
boolean locked = C.CACHE.isLocked("lock:key");
```

### 发布订阅

```java
// 发布消息
C.CACHE.publish("channel:name", "message content");

// 订阅消息（使用@Subscribe注解）
@Subscribe({"channel1", "channel2"})
public void handleMessage(String message) {
    C.LOG.info("收到消息: {}", message);
}

// 编程式订阅
C.CACHE.subscribe("channel:name", message -> {
    // 处理消息
});
```

### 集合操作

```java
// 添加到集合
C.CACHE.sadd("set:key", "member1", "member2", "member3");

// 获取集合所有成员
Set<String> members = C.CACHE.smembers("set:key");

// 检查成员是否存在
boolean isMember = C.CACHE.sismember("set:key", "member1");

// 获取集合大小
long size = C.CACHE.scard("set:key");

// 移除成员
C.CACHE.srem("set:key", "member1");
```

### 缓存使用最佳实践

```java
// 缓存key设计
public class CacheKeys {
    public static String userInfo(String userId) {
        return "user:info:" + userId;
    }

    public static String userToken(String token) {
        return "TOKEN:" + token;
    }

    public static String detectionPoint(String pointId) {
        return "detection:point:" + pointId;
    }
}

// 缓存使用示例
@Method(value = "获取用户信息", status = MethodStatus.COMPLETE)
public User getUserInfo(String userId) {
    // 先从缓存获取
    String cacheKey = CacheKeys.userInfo(userId);
    User user = C.CACHE.get(cacheKey, User.class);
    if (user != null) {
        return user;
    }

    // 从数据库查询
    user = DB.use().eq(User::getId, userId).get();
    if (user != null) {
        // 写入缓存（30分钟）
        C.CACHE.set(cacheKey, user, 1800);
    }
    return user;
}

// 更新时清除缓存
@Method(value = "更新用户", status = MethodStatus.COMPLETE)
public void updateUser(User user) {
    DB.use().update(user);
    // 清除缓存
    C.CACHE.del(CacheKeys.userInfo(user.getId()));
}

// 使用Hash存储Token
public void saveToken(String token, LoginUser user) {
    C.CACHE.setHash("TOKEN", token, C.JSON.toJson(user), 7 * 24 * 3600);  // 7天
}

public LoginUser getUserByToken(String token) {
    return C.CACHE.getHash("TOKEN", token, LoginUser.class);
}

public void removeToken(String token) {
    C.CACHE.delHash("TOKEN", token);
}
```

---

## C.MQ - 消息队列

消息队列发布和订阅，支持Kafka、RabbitMQ等。

### 发布消息

```java
// 发布单条消息
C.MQ.publish("topic.name", "message content");

// 发布多条消息
List<String> messages = List.of("msg1", "msg2", "msg3");
C.MQ.publish("topic.name", messages);

// 发布JSON消息
C.MQ.publish("user.update", C.JSON.toJson(user));

// 发布到多个topic
C.MQ.publish("topic1", message);
C.MQ.publish("topic2", message);
```

### 订阅消息

```java
// 使用@Subscribe注解订阅
@Subscribe({"topic1", "topic2"})
public void handleMessage(String message) {
    C.LOG.info("收到消息: {}", message);
    // 处理消息
}

// 订阅并解析JSON
@Subscribe("user.update")
public void handleUserUpdate(String json) {
    User user = C.JSON.fromJson(json, User.class);
    // 处理用户更新
}
```

### 队列常量定义

```java
public class Constant {
    public static final class QueueKafka {
        public static final String USER_UPDATE = "user-update";
        public static final String DATA_CHANGE = "data-change";
        public static final String DEVICE_STATUS = "device-status";
    }
}

// 使用
C.MQ.publish(Constant.QueueKafka.USER_UPDATE, message);
```

### 消息队列使用场景

```java
// 数据变更通知
@Method(value = "更新数据", status = MethodStatus.COMPLETE)
public void updateData(Data data) {
    DB.use().update(data);

    // 发布数据变更消息
    C.MQ.publish(Constant.QueueKafka.DATA_CHANGE, C.JSON.toJson(data));

    // 清除相关缓存
    C.CACHE.del("data:" + data.getId());
}

// 订阅数据变更
@Subscribe(Constant.QueueKafka.DATA_CHANGE)
public void handleDataChange(String json) {
    Data data = C.JSON.fromJson(json, Data.class);
    // 处理数据变更（如更新缓存、推送通知等）
    C.CACHE.del("data:" + data.getId());
}
```

---

## C.PUSH - 消息推送

实时消息推送（基于Redis Pub/Sub）。

### 推送API

```java
// 推送到指定用户
C.PUSH.push("channel:name", data, userId);

// 推送到用户集合
Set<Integer> userIds = Set.of(1, 2, 3);
C.PUSH.push("channel:name", data, userIds);

// 推送到指定用户和设备类型
Set<Integer> userIds = Set.of(1, 2);
Set<Integer> deviceTypes = Set.of(1, 2);  // 1:Web, 2:App
C.PUSH.push("channel:name", data, userIds, deviceTypes);

// 推送到所有在线用户
C.PUSH.push("channel:name", data);

// 异步推送
C.PUSH.push("channel:name", C.JSON.toJson(data), userId).join();

// 带dataId的推送
C.PUSH.push("channel:name", data, userIds, deviceTypes, dataId);
```

### 推送频道定义

```java
public class Constant {
    public static final class Push {
        // IM消息推送
        public static final String IM_MESSAGE = ServerName.IM + ":message";
        public static final String IM_CHAT = ServerName.IM + ":chat";

        // 在线状态推送
        public static final String ONLINE_STATUS = "user:online:status";

        // 报表推送
        public static final String REPORT_AUTO_DOWNLOAD = "report:auto:download";

        // 数据变更推送
        public static final String DATA_CHANGE = "data:change";

        // 告警推送
        public static final String ALARM = "alarm:notification";
    }
}

// ServerName是服务名常量
public class ServerName {
    public static final String IM = "im";
    public static final String USER_CENTER = "user-center";
}
```

### 推送使用示例

```java
// IM消息推送
@Method(value = "发送消息", status = MethodStatus.COMPLETE)
public Message postMessage(Message message) {
    // 保存消息
    DB.use().insert(message);

    // 获取会话成员
    var members = ChatService.getChatMember(message.getChatId());

    // 过滤发送者
    var receivers = members.stream()
        .filter(x -> x != message.getSender())
        .collect(Collectors.toSet());

    // 推送消息
    if (!receivers.isEmpty()) {
        C.PUSH.push(Constant.Push.IM_MESSAGE, message, receivers);
    }

    return message;
}

// 用户状态变更推送
@Method(value = "更新用户状态", status = MethodStatus.COMPLETE)
public void updateStatus(int userId, int status) {
    // 更新数据库
    DB.use().eq(User::getId, userId).update(User::getStatus, status);

    // 推送状态变更
    var data = Map.of("userId", userId, "status", status);
    C.PUSH.push(Constant.Push.ONLINE_STATUS, C.JSON.toJson(data), userId);
}

// 报表下载完成推送
public void reportDownloadComplete(int userId, String fileId) {
    var data = Map.of(
        "fileId", fileId,
        "message", "报表已生成，请下载"
    );
    C.PUSH.push(Constant.Push.REPORT_AUTO_DOWNLOAD, data, userId);
}

// 告警推送
public void sendAlarm(Alarm alarm) {
    var data = Map.of(
        "alarmId", alarm.getId(),
        "level", alarm.getLevel(),
        "message", alarm.getMessage(),
        "time", C.TIME.toDateTimeString(alarm.getTime())
    );

    // 推送给相关用户
    C.PUSH.push(Constant.Push.ALARM, data, alarm.getUserIds());
}
```

### 推送数据格式

```java
// 推送数据通常使用JSON格式
var pushData = Map.of(
    "type", "data_update",
    "id", entity.getId(),
    "name", entity.getName(),
    "time", C.TIME.localTimestamp()
);

C.PUSH.push("channel", C.JSON.toJson(pushData), userIds);
```

### 推送与缓存结合

```java
// 更新数据并推送
@Method(value = "更新配置", status = MethodStatus.COMPLETE)
public void updateConfig(Config config) {
    // 更新数据库
    DB.use().update(config);

    // 更新缓存
    C.CACHE.set("config:" + config.getId(), config, 3600);

    // 推送配置变更
    var data = Map.of(
        "id", config.getId(),
        "name", config.getName(),
        "action", "update"
    );
    C.PUSH.push("config:change", C.JSON.toJson(data), config.getUserIds());
}
```

---

## 缓存、消息队列、推送综合使用

### 场景1：用户信息更新

```java
@Method(value = "更新用户信息", status = MethodStatus.COMPLETE)
public void updateUserInfo(User user) {
    // 1. 更新数据库
    DB.use().update(user);

    // 2. 清除缓存
    C.CACHE.del(CacheKeys.userInfo(user.getId()));

    // 3. 发布消息
    C.MQ.publish(Constant.QueueKafka.USER_UPDATE, C.JSON.toJson(user));

    // 4. 推送通知（如果有在线用户）
    C.PUSH.push(Constant.Push.USER_CHANGE, user.getId());
}
```

### 场景2：数据同步

```java
// 发布端
@Method(value = "删除数据", status = MethodStatus.COMPLETE)
public void deleteData(String id) {
    DB.use().eq(Entity::getId, id).delete();
    C.CACHE.del("data:" + id);

    // 发布数据变更
    C.MQ.publish(Constant.Subscribe.DATA_TABLE, id);
}

// 订阅端
@Subscribe(Constant.Subscribe.DATA_TABLE)
public void handleDataChange(String id) {
    // 清除缓存
    C.CACHE.del("data:" + id);

    // 推送通知
    C.PUSH.push(Constant.Push.DATA_CHANGE, id);
}
```

### 场景3：实时数据推送

```java
// 测点数据变更
@Method(value = "上报测点数据", status = MethodStatus.COMPLETE, printLog = false)
public void reportPointData(PointData data) {
    // 保存到时序数据库
    TIME_DB.useTime().insert(data);

    // 更新缓存（最新数据）
    C.CACHE.setHash("POINT:latest", data.getPointId(), C.JSON.toJson(data));

    // 推送给订阅用户
    var subscribers = getSubscribers(data.getPointId());
    if (!subscribers.isEmpty()) {
        C.PUSH.push("point:data", data, subscribers);
    }
}

// 订阅用户获取最新数据
public PointData getLatestPointData(String pointId) {
    // 先从缓存获取
    String json = C.CACHE.getHash("POINT:latest", pointId);
    if (json != null) {
        return C.JSON.fromJson(json, PointData.class);
    }

    // 从数据库查询最新数据
    return TIME_DB.useTime()
        .eq(PointData::getPointId, pointId)
        .orderByDesc(PointData::getTime)
        .limit(1)
        .get();
}
```
