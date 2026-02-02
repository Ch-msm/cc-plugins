# 消息订阅与数据同步

## 目录

- [@Subscribe 注解使用](#subscribe-注解使用)
- [消息队列订阅](#消息队列订阅)
- [Redis发布订阅](#redis发布订阅)
- [数据同步模式](#数据同步模式)
- [订阅最佳实践](#订阅最佳实践)

---

## @Subscribe 注解使用

### 基本用法

```java
// 订阅单个主题
@Subscribe("topic.name")
public void handleMessage(String message) {
    C.LOG.info("收到消息: {}", message);
}

// 订阅多个主题
@Subscribe({"topic1", "topic2", "topic3"})
public void handleMultiTopic(String message) {
    // 处理消息
}
```

### 订阅消息解析

```java
// 订阅并解析JSON
@Subscribe(Constant.QueueKafka.USER_UPDATE)
public void handleUserUpdate(String json) {
    User user = C.JSON.fromJson(json, User.class);
    // 处理用户更新
}

// 订阅并执行业务逻辑
@Subscribe(Constant.QueueKafka.DATA_CHANGE)
public void handleDataChange(String dataId) {
    // 清除缓存
    C.CACHE.del("data:" + dataId);

    // 推送通知
    C.PUSH.push(Constant.Push.DATA_CHANGE, dataId);
}
```

### 订阅方法要求

```java
// 1. 方法必须是public
// 2. 参数通常为String类型（消息内容）
// 3. 可以抛出异常
// 4. 支持在Service类中使用

@Service(value = "数据同步", author = "梅思铭", date = "2025-01-08")
public class DataSyncService extends AbstractService {

    // 正确的订阅方法
    @Subscribe(Constant.Subscribe.DATA_TABLE)
    public void handleTableChange(String dataId) {
        // 处理数据表变更
    }

    // 支持异常抛出
    @Subscribe(Constant.Subscribe.DEVICE_STATUS)
    public void handleDeviceStatus(String json) {
        try {
            DeviceStatus status = C.JSON.fromJson(json, DeviceStatus.class);
            processStatus(status);
        } catch (Exception e) {
            C.LOG.error("处理设备状态失败", e);
            throw e;
        }
    }
}
```

---

## 消息队列订阅

### Kafka订阅

```java
// 定义队列常量
public class Constant {
    public static final class QueueKafka {
        public static final String USER_UPDATE = "user-update";
        public static final String DATA_CHANGE = "data-change";
        public static final String DEVICE_STATUS = "device-status";
    }
}

// 订阅Kafka消息
@Subscribe(Constant.QueueKafka.USER_UPDATE)
public void handleUserUpdate(String json) {
    User user = C.JSON.fromJson(json, User.class);

    // 更新缓存
    C.CACHE.set("user:" + user.getId(), user, 1800);

    // 推送给在线用户
    C.PUSH.push(Constant.Push.USER_CHANGE, user.getId());
}
```

### RabbitMQ订阅

```java
@Subscribe("rabbitmq.queue.name")
public void handleRabbitMQMessage(String message) {
    // 处理RabbitMQ消息
}
```

### MQTT订阅

```java
@Subscribe("mqtt/topic/sensor")
public void handleSensorData(String data) {
    // 处理传感器数据
}
```

---

## Redis发布订阅

### Redis发布消息

```java
// 发布到Redis频道
C.CACHE.publish("channel:name", "message content");

// 发布JSON数据
C.CACHE.publish("user:login", C.JSON.toJson(userInfo));

// 发布到多个频道
C.CACHE.publish("channel1", message);
C.CACHE.publish("channel2", message);
```

### Redis订阅消息

```java
// 使用@Subscribe注解订阅
@Subscribe({"channel1", "channel2"})
public void handleRedisMessage(String message) {
    C.LOG.info("收到Redis消息: {}", message);
}

// 编程式订阅（不常用）
C.CACHE.subscribe("channel:name", message -> {
    // 处理消息
});
```

### Redis频道命名规范

```java
public class CacheChannels {
    // 用户登录频道
    public static String userLogin(int userId) {
        return "user:login:" + userId;
    }

    // 配置变更频道
    public static final String CONFIG_CHANGE = "config:change";

    // 数据变更频道
    public static String dataChange(String dataType) {
        return "data:change:" + dataType;
    }

    // 在线状态频道
    public static final String ONLINE_STATUS = "user:online:status";
}

// 使用
C.CACHE.publish(CacheChannels.userLogin(userId), C.JSON.toJson(userInfo));
C.CACHE.publish(CacheChannels.ONLINE_STATUS, C.JSON.toJson(status));
```

---

## 数据同步模式

### 模式1：发布-订阅同步

```java
// 发布端
@Service(value = "数据管理", author = "梅思铭", date = "2025-01-08")
public class DataService extends AbstractService {

    @Method(value = "删除数据", status = MethodStatus.COMPLETE)
    public void deleteData(String id) {
        // 1. 删除数据库
        DB.use().eq(Entity::getId, id).delete();

        // 2. 发布数据变更消息
        C.MQ.publish(Constant.Subscribe.DATA_TABLE, id);
    }
}

// 订阅端
@Service(value = "数据同步", author = "梅思铭", date = "2025-01-08")
public class DataSyncService extends AbstractService {

    @Subscribe(Constant.Subscribe.DATA_TABLE)
    public void handleDataChange(String id) {
        // 1. 清除缓存
        C.CACHE.del("data:" + id);

        // 2. 推送通知
        C.PUSH.push(Constant.Push.DATA_CHANGE, id);

        // 3. 记录日志
        C.LOG.info("数据已删除: {}", id);
    }
}
```

### 模式2：缓存更新同步

```java
// 更新数据
@Method(value = "更新数据", status = MethodStatus.COMPLETE)
public void updateData(Data data) {
    // 1. 更新数据库
    DB.use().update(data);

    // 2. 清除缓存
    C.CACHE.del("data:" + data.getId());

    // 3. 发布更新消息
    C.MQ.publish(Constant.QueueKafka.DATA_CHANGE, C.JSON.toJson(data));
}

// 订阅并更新缓存
@Subscribe(Constant.QueueKafka.DATA_CHANGE)
public void handleDataUpdate(String json) {
    Data data = C.JSON.fromJson(json, Data.class);

    // 更新缓存
    C.CACHE.set("data:" + data.getId(), data, 3600);

    // 推送通知
    C.PUSH.push(Constant.Push.DATA_CHANGE, data.getId());
}
```

### 模式3：多服务数据同步

```java
// 服务A：用户服务
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "更新用户", status = MethodStatus.COMPLETE)
    public void updateUser(User user) {
        // 更新数据库
        DB.use().update(user);

        // 发布用户变更消息
        C.MQ.publish(Constant.QueueKafka.USER_UPDATE, C.JSON.toJson(user));
    }
}

// 服务B：订单服务（订阅用户变更）
@Service(value = "订单同步", author = "梅思铭", date = "2025-01-08")
public class OrderSyncService extends AbstractService {

    @Subscribe(Constant.QueueKafka.USER_UPDATE)
    public void handleUserUpdate(String json) {
        User user = C.JSON.fromJson(json, User.class);

        // 更新订单表中的用户信息
        DB.use()
            .eq(Order::getUserId, user.getId())
            .update(Order::getUserName, user.getName());

        // 清除订单缓存
        C.CACHE.del("orders:user:" + user.getId());
    }
}

// 服务C：报表服务（订阅用户变更）
@Service(value = "报表同步", author = "梅思铭", date = "2025-01-08")
public class ReportSyncService extends AbstractService {

    @Subscribe(Constant.QueueKafka.USER_UPDATE)
    public void handleUserUpdate(String json) {
        User user = C.JSON.fromJson(json, User.class);

        // 更新报表统计
        updateReportStatistics(user);

        // 清除报表缓存
        C.CACHE.del("report:user:" + user.getId());
    }
}
```

---

## 订阅最佳实践

### 1. 订阅异常处理

```java
@Subscribe(Constant.QueueKafka.DATA_CHANGE)
public void handleDataChange(String json) {
    try {
        Data data = C.JSON.fromJson(json, Data.class);
        processData(data);
    } catch (Exception e) {
        C.LOG.error("处理数据变更失败: {}", json, e);
        // 不抛出异常，避免影响其他订阅者
    }
}
```

### 2. 订阅幂等性

```java
@Subscribe(Constant.QueueKafka.DATA_UPDATE)
public void handleDataUpdate(String json) {
    Data data = C.JSON.fromJson(json, Data.class);

    // 检查是否已处理过（使用时间戳或版本号）
    String processedKey = "processed:" + data.getId() + ":" + data.getVersion();
    if (C.CACHE.exists(processedKey)) {
        C.LOG.info("消息已处理，跳过: {}", data.getId());
        return;
    }

    // 处理数据
    processData(data);

    // 标记为已处理（24小时）
    C.CACHE.set(processedKey, "1", 86400);
}
```

### 3. 订阅性能优化

```java
// 异步处理
@Subscribe(Constant.QueueKafka.BULK_DATA)
public void handleBulkData(String json) {
    // 使用异步线程处理
    C.ASYNC.run(() -> {
        List<Data> dataList = C.JSON.fromJsonToList(json, Data.class);

        // 批量处理
        dataList.forEach(this::processData);
    });
}

// 批量订阅
@Subscribe({"queue1", "queue2", "queue3"})
public void handleMultiQueue(String message) {
    // 统一处理多个队列的消息
}
```

### 4. 订阅消息顺序

```java
// 使用分布式锁保证顺序
@Subscribe(Constant.QueueKafka.ORDERED_DATA)
public void handleOrderedData(String json) {
    Data data = C.JSON.fromJson(json, Data.class);
    String lockKey = "lock:data:" + data.getId();

    if (C.CACHE.tryLock(lockKey, 10)) {
        try {
            // 顺序处理
            processData(data);
        } finally {
            C.CACHE.delLock(lockKey);
        }
    } else {
        C.LOG.warn("获取锁失败，跳过: {}", data.getId());
    }
}
```

### 5. 订阅消息重试

```java
@Subscribe(Constant.QueueKafka.IMPORTANT_DATA)
public void handleImportantData(String json) {
    int retryCount = 0;
    int maxRetry = 3;

    while (retryCount < maxRetry) {
        try {
            Data data = C.JSON.fromJson(json, Data.class);
            processData(data);
            break;
        } catch (Exception e) {
            retryCount++;
            C.LOG.error("处理失败，重试 {}/{}", retryCount, maxRetry, e);

            if (retryCount >= maxRetry) {
                // 发送告警
                C.MAIL.sendHtml("admin@example.com", "数据处理失败",
                    "数据处理失败，请检查: " + json);
                throw e;
            }

            // 等待后重试
            try {
                Thread.sleep(1000 * retryCount);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

---

## 订阅场景示例

### 场景1：测点数据同步

```java
// 发布端：上报测点数据
@Method(value = "上报测点数据", status = MethodStatus.COMPLETE, printLog = false)
public void reportPointData(PointData data) {
    // 1. 保存到时序数据库
    TIME_DB.useTime().insert(data);

    // 2. 更新缓存（最新数据）
    C.CACHE.setHash("POINT:latest", data.getPointId(), C.JSON.toJson(data));

    // 3. 推送给订阅用户
    var subscribers = getSubscribers(data.getPointId());
    if (!subscribers.isEmpty()) {
        C.PUSH.push("point:data", data, subscribers);
    }
}

// 订阅端：处理测点数据
@Subscribe("point:data")
public void handlePointData(String json) {
    PointData data = C.JSON.fromJson(json, PointData.class);

    // 触发告警
    if (data.getValue() > data.getThreshold()) {
        triggerAlarm(data);
    }

    // 更新统计
    updateStatistics(data);
}
```

### 场景2：审批流程通知

```java
// 提交审批
@Method(value = "提交审批", status = MethodStatus.COMPLETE)
public void submitApproval(Approval approval) {
    // 1. 保存审批
    DB.use().insert(approval);

    // 2. 发布审批消息
    C.MQ.publish(Constant.QueueKafka.APPROVAL_SUBMIT, C.JSON.toJson(approval));

    // 3. 推送给审批人
    C.PUSH.push(Constant.Push.APPROVAL_NOTIFY, approval, approval.getApproverId());
}

// 订阅审批消息
@Subscribe(Constant.QueueKafka.APPROVAL_SUBMIT)
public void handleApprovalSubmit(String json) {
    Approval approval = C.JSON.fromJson(json, Approval.class);

    // 发送邮件通知
    var content = String.format(
        "<h2>审批通知</h2><p>您有一个新的审批需要处理：</p><p>%s</p>",
        approval.getTitle()
    );
    C.MAIL.sendHtml(approval.getApproverEmail(), "审批通知", content);

    // 记录日志
    C.LOG.info("审批已提交: {}", approval.getId());
}
```

### 场景3：报表自动生成

```java
// 定时任务触发报表生成
@Method(value = "生成报表", status = MethodStatus.COMPLETE)
public void generateReport(Report report) {
    // 1. 生成报表
    var fileId = generateReportFile(report);

    // 2. 发布报表生成完成消息
    C.MQ.publish(Constant.QueueKafka.REPORT_GENERATED,
        C.JSON.toJson(Map.of("reportId", report.getId(), "fileId", fileId)));

    // 3. 推送给用户
    C.PUSH.push(Constant.Push.REPORT_AUTO_DOWNLOAD,
        Map.of("fileId", fileId, "reportName", report.getName()),
        report.getUserId());
}

// 订阅报表生成完成
@Subscribe(Constant.QueueKafka.REPORT_GENERATED)
public void handleReportGenerated(String json) {
    var data = C.JSON.fromJsonToMap(json);

    // 更新报表状态
    DB.use()
        .eq(Report::getId, data.get("reportId"))
        .update(Report::getStatus, 2); // 完成

    // 记录日志
    C.LOG.info("报表生成完成: {}", data.get("fileId"));
}
```

---

## 常用常量定义

```java
public class Constant {
    // 队列常量
    public static final class QueueKafka {
        public static final String USER_UPDATE = "user-update";
        public static final String DATA_CHANGE = "data-change";
        public static final String DATA_TABLE = "data-table";
        public static final String DEVICE_STATUS = "device-status";
        public static final String APPROVAL_SUBMIT = "approval-submit";
        public static final String REPORT_GENERATED = "report-generated";
    }

    // 订阅常量
    public static final class Subscribe {
        public static final String DATA_TABLE = "data-table";
        public static final String DEVICE_STATUS = "device-status";
    }

    // 推送常量
    public static final class Push {
        public static final String DATA_CHANGE = "data:change";
        public static final String USER_CHANGE = "user:change";
        public static final String APPROVAL_NOTIFY = "approval:notify";
        public static final String REPORT_AUTO_DOWNLOAD = "report:auto:download";
        public static final String ALARM = "alarm:notification";
    }
}
```
