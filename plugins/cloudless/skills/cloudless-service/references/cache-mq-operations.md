# 缓存、消息队列与推送

## `C.CACHE`

### 字符串缓存

```java
C.CACHE.set("user:1001", user);
C.CACHE.set("captcha:email", 300, code);

String code = C.CACHE.get("captcha:email");
User user = C.CACHE.get("user:1001", User.class);

C.CACHE.del("captcha:email");
C.CACHE.expire("user:1001", 1800);
```

注意：

- 带过期时间的签名是 `set(key, seconds, value)`
- 不要假设这里提供 Redis 常见的“是否存在”和“读取剩余过期时间”快捷接口

### Hash

整体 Hash：

```java
C.CACHE.setHash("config:third-party", Map.of(
    "appId", "123",
    "appKey", "abc"
));

Map<String, String> config = C.CACHE.getHash("config:third-party");
```

字段级 Hash：

```java
C.CACHE.setHash("TOKEN", token, loginUser);

LoginUser loginUser = C.CACHE.getHash("TOKEN", token, LoginUser.class);

C.CACHE.delHash("TOKEN", token);
```

注意：

- 字段级 `setHash(key, field, value)` 没有单独过期时间参数

### 列表与集合

```java
C.CACHE.rpush("task:queue", "1", "2");
C.CACHE.lpush("task:queue", "0");

List<String> popped = C.CACHE.brpop(5, "task:queue");

C.CACHE.sadd("user:online", "1001", "1002");
Set<String> online = C.CACHE.smembers("user:online");
boolean contains = C.CACHE.sismember("user:online", "1001");
```

### 发布订阅

```java
C.CACHE.publish("config:change", C.JSON.toJson(data));

C.CACHE.subscribe((channel, message) -> {
    C.LOG.info("收到频道消息: {}, {}", channel, message);
}, "config:change");
```

### 分布式锁

```java
if (C.CACHE.tryLock("report:generate:" + reportId, 10)) {
    try {
        doGenerate();
    } finally {
        C.CACHE.delLock("report:generate:" + reportId);
    }
}
```

### 自增和地理能力

```java
long no = C.CACHE.incr("serial:order");

C.CACHE.geoadd("device:location", 120.63, 31.31, "device-1");
var list = C.CACHE.geoRadius("device:location", 120.63, 31.31, 1000, 100);
```

## `C.MQ`

发布消息：

```java
C.MQ.publish("user:update", C.JSON.toJson(user));
C.MQ.publish("batch:task", List.of("a", "b", "c"));
```

说明：

- `C.MQ` 当前实现是 Kafka
- 主题名建议由业务常量维护

## `C.PUSH`

```java
C.PUSH.push("report:ready", data);
C.PUSH.push("report:ready", data, userId);
C.PUSH.push("report:ready", data, List.of(1001, 1002));
C.PUSH.push("report:ready", data, List.of(1001, 1002), List.of(1, 2));
C.PUSH.push("report:ready", data, List.of(1001, 1002), List.of(1, 2), reportId);
```

说明：

- 返回值是 `CompletableFuture<Void>`
- 频道名通常由业务项目自己定义

## 常见组合

缓存 + 数据库：

```java
public User get(String id) {
    String key = "user:" + id;
    User entity = C.CACHE.get(key, User.class);
    if (entity != null) {
        return entity;
    }
    entity = DB.use().eq(User::getId, id).get();
    if (entity != null) {
        C.CACHE.set(key, 1800, entity);
    }
    return entity;
}
```

数据库 + MQ：

```java
public void update(User entity) {
    DB.use().update(entity);
    C.MQ.publish("user:update", C.JSON.toJson(entity));
}
```

数据库 + 推送：

```java
public void finishReport(String reportId, int userId) {
    C.PUSH.push("report:ready", Map.of("reportId", reportId), userId).join();
}
```
