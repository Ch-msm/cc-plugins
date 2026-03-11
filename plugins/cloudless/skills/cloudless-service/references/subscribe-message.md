# 订阅与数据同步

## `@Subscribe`

`@Subscribe` 不是 HTTP 接口注解，而是订阅入口。

真实定义：

```java
@Subscribe(
    value = {"key1", "key2"},
    dataTable = true,
    distribute = true
)
```

字段说明：

- `value`：订阅 key 或频道
- `dataTable`：是否按“数据表变更”模式处理，默认 `true`
- `distribute`：是否做分发订阅，默认 `true`

## 方法签名

根据 `Register` 的调用逻辑，订阅方法应定义为两个参数：

```java
@Subscribe("user:update")
public void handleUserUpdate(String key, String value) {
}
```

说明：

- `dataTable = true` 时，`key` 是数据变更 key，`value` 是消息体
- `dataTable = false` 时，`key` 是频道名，`value` 是频道消息

不要写成单参数方法。

## Redis 频道订阅

```java
@Subscribe(value = {"config:change"}, dataTable = false, distribute = false)
public void handleConfigChannel(String channel, String message) {
    C.LOG.info("频道: {}, 消息: {}", channel, message);
}
```

对应发布：

```java
C.CACHE.publish("config:change", C.JSON.toJson(data));
```

## 数据变更订阅

`dataTable = true` 适合监听数据更新同步 key。

```java
@Subscribe("user")
public void handleUserDataChange(String key, String value) {
    C.LOG.info("数据变更: {}, {}", key, value);
}
```

说明：

- 框架会在内部拼接数据更新前缀
- 分发模式下会基于订阅者做分流

## 业务示例

```java
@Service(value = "用户同步", author = "梅思铭", date = "2026-03-09")
public class UserSyncService extends AbstractService {

    @Subscribe(value = {"user:update"}, dataTable = false, distribute = false)
    public void handleUserUpdate(String channel, String message) {
        User entity = C.JSON.fromJson(message, User.class);
        C.CACHE.del("user:" + entity.getId());
    }
}
```

## 选择建议

- 只需要跨实例广播频道消息：`dataTable = false`
- 需要做数据更新同步：`dataTable = true`
- 希望每个实例都收到：`distribute = false`
- 希望同类订阅只由一个实例处理：`distribute = true`

## 推荐做法

1. 订阅方法统一写两个 `String` 参数
2. 订阅方法内部先做 JSON 解析，再进入业务逻辑
3. 需要所有实例都消费时显式设置 `distribute = false`
4. 订阅方法里自己处理异常和日志
