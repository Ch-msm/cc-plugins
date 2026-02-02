# 常用工具类参考

## 目录

- [C.CONFIG - 配置工具](#cconfig---配置工具)
- [C.JSON - JSON操作](#cjson---json操作)
- [C.LOG - 日志输出](#clog---日志输出)
- [C.HTTP - HTTP请求](#chttp---http请求)
- [C.ASYNC - 异步调用](#casync---异步调用)
- [C.MAIL - 邮件工具](#cmail---邮件工具)
- [C.TEXT - 文本操作](#ctext---文本操作)
- [C.OBJECT - 对象工具](#cobject---对象工具)
- [C.DATA - 数据处理](#cdata---数据处理)
- [C.TREE - 树结构操作](#ctree---树结构操作)
- [ClientStub - 服务间调用](#clientstub---服务间调用)

---

## C.CONFIG - 配置工具

从YAML配置文件读取配置值。

```java
// 获取配置值
String value = C.CONFIG.get("config.key");

// 获取带默认值的配置
String value = C.CONFIG.get("config.key", "defaultValue");

// 获取整数
int port = C.CONFIG.getInt("server.port", 8080);

// 获取布尔值
boolean debug = C.CONFIG.getBoolean("app.debug", false);

// 配置文件示例 (resources/app.yml)
// config:
//   key: value
// server:
//   port: 9001
// app:
//   debug: true
```

---

## C.JSON - JSON操作

JSON序列化和反序列化。

```java
// 对象转JSON
String json = C.JSON.toJson(object);

// JSON转对象
MyClass obj = C.JSON.fromJson(json, MyClass.class);

// JSON转Map
Map<String, Object> map = C.JSON.toHashMap(object);
Map<String, Object> map = C.JSON.fromJsonToMap(json);

// JSON转List
List<MyClass> list = C.JSON.fromJsonToList(json, MyClass.class);

// 对象转换
TargetClass target = C.OBJECT.convert(TargetClass.class, sourceObject);
```

---

## C.LOG - 日志输出

结构化日志输出。

```java
// 不同级别的日志
C.LOG.trace("追踪信息");
C.LOG.debug("调试信息");
C.LOG.info("普通信息");
C.LOG.warn("警告信息");
C.LOG.error("错误信息");

// 带参数的日志
C.LOG.info("用户ID: {}, 操作: {}, 结果: {}", userId, action, result);

// 带异常的日志
C.LOG.error("操作失败", exception);
C.LOG.catching(exception);  // 记录异常堆栈

// 禁用日志打印
@Method(value = "敏感方法", status = MethodStatus.COMPLETE, printLog = false)
public void sensitiveMethod() { }
```

---

## C.HTTP - HTTP请求

发送HTTP请求。

```java
// GET请求
String response = C.HTTP.get(url);

// POST请求
String response = C.HTTP.post(url, requestBody);

// 带请求头的请求
Map<String, String> headers = Map.of(
    "Authorization", "Bearer token",
    "Content-Type", "application/json"
);
String response = C.HTTP.get(url, headers);

// 带超时的请求
String response = C.HTTP.get(url, 5000);
```

---

## C.ASYNC - 异步调用

异步任务执行。

```java
// 执行异步任务
C.ASYNC.run(() -> {
    doSomething();
});

// 提交任务并获取Future
Future<String> future = C.ASYNC.submit(() -> {
    return "result";
});

// 带延迟的异步执行
C.ASYNC.runLater(() -> {
    doSomething();
}, 1000); // 1秒后执行

// 多个异步任务并发执行
List.of(
    C.ASYNC.run(() -> task1()),
    C.ASYNC.run(() -> task2()),
    C.ASYNC.run(() -> task3())
).forEach(CompletableFuture::join);
```

---

## C.MAIL - 邮件工具

发送邮件。

```java
// 发送HTML邮件
C.MAIL.sendHtml(email, subject, htmlContent);

// 发送带附件的邮件
C.MAIL.sendHtml(email, subject, content, attachmentFile);

// 发送简单邮件
C.MAIL.send(recipient, subject, content);

// 发送给多个收件人
List<String> emails = List.of("email1@example.com", "email2@example.com");
C.MAIL.send(emails, subject, content);

// 邮件发送示例
@Method(value = "发送验证码", status = MethodStatus.COMPLETE)
public void sendCaptcha(String email) {
    var captcha = C.TEXT.random(6);

    // 保存到缓存（5分钟）
    C.CACHE.set("captcha:" + email, captcha, 300);

    // 发送邮件
    var content = String.format(
        "<h2>验证码</h2><p>您的验证码是：<strong style='color:red;font-size:24px;'>%s</strong></p><p>5分钟内有效，请勿泄露给他人。</p>",
        captcha
    );
    C.MAIL.sendHtml(email, "邮箱验证码", content);
}
```

---

## C.TEXT - 文本操作

文本处理工具。

```java
// ID生成
String shortId = C.TEXT.shortId();   // 生成短ID（8位）
String longId = C.TEXT.longId();     // 生成长ID（16位）

// 判断是否为空
boolean empty = C.TEXT.isEmpty(text);
boolean notEmpty = C.TEXT.isNotEmpty(text);

// 加密
String encrypted = C.TEXT.encrypt(text);

// 生成随机字符串
String random = C.TEXT.random(10);  // 10位随机字符串

// 字符串拼接
String joined = C.TEXT.join(",", list);
```

---

## C.OBJECT - 对象工具

对象操作工具。

```java
// 判断是否为空
boolean empty = C.OBJECT.isEmpty(object);        // 支持String/Collection/Map等
boolean notEmpty = C.OBJECT.isNotEmpty(object);
boolean allEmpty = C.OBJECT.isAllEmpty(v1, v2, v3);

// 对象转换
Entity entity = C.OBJECT.convert(Entity.class, sourceObject);

// 类型转换
Integer num = C.OBJECT.toInteger(object);
String str = C.OBJECT.toString(object);
Long longValue = C.OBJECT.toLong(object);
```

---

## C.DATA - 数据处理

数据转换和处理。

```java
// List转Map
Map<Integer, Entity> map = C.DATA.toMap(list, Entity::getId);

// 数据注入（关联查询）
C.DATA
    .inject(mainList, MainEntity::getId)
    .add(MainEntity::getSubList, ids ->
        subDB.use().in(SubEntity::getParentId, ids).query()
    )
    .join();

// 深度克隆
Object cloned = C.DATA.deepClone(object);
```

---

## C.TREE - 树结构操作

树结构相关操作。

```java
// 查找子节点
List<Organization> children = C.TREE.findChildNode(list, parentId, true);

// 查找所有子孙节点
List<Organization> descendants = C.TREE.findAllChildNode(list, parentId);

// 构建树结构
List<TreeNode> tree = C.TREE.build(list, parentIdField, idField);

// 树结构使用示例
@Method(value = "获取组织树", status = MethodStatus.COMPLETE)
public List<Organization> getTree() {
    var list = DB.use().orderBy(Organization::getSortOrder).query();
    return C.TREE.build(list, Organization::getParentId, Organization::getId);
}
```

---

## ClientStub - 服务间调用

调用其他微服务。

### 基本调用

```java
// 发送请求
var json = ClientStub.send(
    "/user-center/User/get",     // 服务路径: /{服务名}/{类名}/{方法名}
    new RequestBody(),            // 请求上下文
    C.JSON.toJson(userId)         // 请求数据
);

// 解析响应
User user = C.JSON.fromJson(json, User.class);
```

### 创建依赖类

```java
// 创建依赖类简化调用
public class UserCenter {
    public static User get(String userId) {
        var json = ClientStub.send(
            "/user-center/User/get",
            new RequestBody(),
            C.JSON.toJson(userId)
        );
        return C.JSON.fromJson(json, User.class);
    }

    public static LoginUser getLoginUser(String token, String inPower) {
        var json = ClientStub.send(
            "/user-center/Authenticate/getLoginUser",
            new RequestBody(),
            C.JSON.toJson(Map.of("token", token, "inPower", inPower))
        );
        return C.JSON.fromJson(json, LoginUser.class);
    }
}

// 使用
User user = UserCenter.get(userId);
LoginUser user = UserCenter.getLoginUser(token, inPower);
```

### 心跳检测

```java
// 心跳检测
long timestamp = ClientStub.heartbeat(serviceAddress);
if (timestamp == 0) {
    throw new AppRuntimeException("节点不可用");
}

// 节点选择（任务调度）
var node = NodeService.getAvailableNode(task.getNodeId());
var server = node.getServiceAddress();

// 检测节点
if (ClientStub.heartbeat(server) == 0) {
    throw new AppRuntimeException("节点不可用");
}
```

### URL路径规则

```
/{服务名}/{Service类名去掉Service}/{方法名}
```

示例：
- `UserService.getUser()` → `/user-center/User/getUser`
- `AuthenticateService.login()` → `/user-center/Authenticate/login`
- `DataService.find()` → `/data-service/Data/find`

---

## RunTimeConstant - 运行时常量

获取当前请求上下文。

```java
// 获取当前请求上下文
RequestBody requestBody = RunTimeConstant.requestBody.get();

// 或使用context()方法（在Service中）
var context = context();

// 常用属性
int userId = requestBody.getUserId();
String token = requestBody.getToken();
List<Integer> roleIds = requestBody.getRoleIds();
String apiToken = requestBody.getApiToken();
String ip = requestBody.getIp();

// 使用示例
@Method(value = "获取当前用户信息", status = MethodStatus.COMPLETE)
public UserInfo getCurrentUserInfo() {
    var ctx = context();
    return UserCenter.getUser(ctx.getUserId());
}
```

---

## 工具类综合使用示例

```java
@Service(value = "数据管理", author = "梅思铭", date = "2025-01-08")
public class DataService extends AbstractService {

    @Method(value = "导入数据", status = MethodStatus.COMPLETE)
    public void importData(String fileId) {
        // 1. 下载文件
        var file = C.FILE.download(fileId);

        // 2. 读取Excel
        var list = File.EXECL.reader(file, DataImport.class);

        // 3. 异步处理
        C.ASYNC.run(() -> {
            int success = 0;
            for (var item : list) {
                try {
                    // 数据校验
                    if (C.OBJECT.isEmpty(item.getName())) {
                        continue;
                    }

                    // 生成ID
                    if (C.OBJECT.isEmpty(item.getId())) {
                        item.setId(C.TEXT.longId());
                    }

                    // 插入数据库
                    DB.use().insert(item);
                    success++;

                } catch (Exception e) {
                    C.LOG.error("导入失败: {}", item.getName(), e);
                }
            }

            // 4. 记录日志
            C.LOG.info("导入完成: 成功{}", success);

            // 5. 推送通知
            var data = Map.of("success", success, "total", list.size());
            C.PUSH.push("import:result", C.JSON.toJson(data), context().getUserId());
        });
    }

    @Method(value = "导出数据并发送邮件", status = MethodStatus.COMPLETE)
    public String exportAndEmail(String email, Search search) {
        // 1. 查询数据
        var list = DB.use()
            .eq(Data::getStatus, 1)
            .query();

        // 2. 构建导出项
        var items = List.of(
            new ExportItem("id", "ID", 100),
            new ExportItem("name", "名称", 200),
            new ExportItem("createTime", "创建时间", 150)
        );

        // 3. 导出Excel
        var fileId = File.EXECL.exportPermanent("数据导出", items, list);

        // 4. 下载文件
        var file = C.FILE.download(fileId);

        // 5. 发送邮件
        var content = String.format(
            "<h2>数据导出</h2><p>您的数据已导出完成，共%d条记录。</p><p>请查看附件。</p>",
            list.size()
        );
        C.MAIL.sendHtml(email, "数据导出完成", content, file);

        // 6. 记录日志
        C.LOG.info("导出并发送邮件: {}, {}", email, list.size());

        return fileId;
    }
}
```

---

## C.MATH - 数学计算

基于BigDecimal的精确数学计算，避免浮点数精度问题。

### 基本运算

```java
// 加法
BigDecimal result = C.MATH.add(d1, d2);
BigDecimal result = C.MATH.add(d1, d2, 2);  // 指定精度（小数位数）

// 减法
BigDecimal result = C.MATH.subtract(d1, d2);
BigDecimal result = C.MATH.subtract(d1, d2, 2);

// 乘法
BigDecimal result = C.MATH.multiply(d1, d2, 2);

// 除法
BigDecimal result = C.MATH.divide(d1, d2);  // 默认精度10位
BigDecimal result = C.MATH.divide(d1, d2, 2);  // 指定精度
```

### 使用场景

```java
// 金融计算
@Method(value = "计算利息", status = MethodStatus.COMPLETE)
public BigDecimal calculateInterest(BigDecimal principal, BigDecimal rate, int days) {
    // 利息 = 本金 * 利率 * 天数 / 365
    return C.MATH.divide(
        C.MATH.multiply(C.MATH.multiply(principal, rate), new BigDecimal(days), 2),
        new BigDecimal(365),
        2
    );
}

// 价格计算
@Method(value = "计算总价", status = MethodStatus.COMPLETE)
public BigDecimal calculateTotalPrice(BigDecimal price, int quantity, BigDecimal discount) {
    // 总价 = 价格 * 数量 * 折扣
    BigDecimal subtotal = C.MATH.multiply(price, new BigDecimal(quantity), 2);
    return C.MATH.multiply(subtotal, discount, 2);
}

// 金额分摊
@Method(value = "分摊金额", status = MethodStatus.COMPLETE)
public List<BigDecimal> splitAmount(BigDecimal amount, int count) {
    List<BigDecimal> result = new ArrayList<>();
    BigDecimal each = C.MATH.divide(amount, new BigDecimal(count), 2);

    for (int i = 0; i < count - 1; i++) {
        result.add(each);
    }

    // 最后一笔：总额 - 已分配
    BigDecimal allocated = C.MATH.multiply(each, new BigDecimal(count - 1), 2);
    result.add(C.MATH.subtract(amount, allocated, 2));

    return result;
}
```

---

## C.GEO - 地理计算

地理坐标和距离计算工具，基于WGS84坐标系。

### 计算两点距离

```java
// 使用坐标对象
Coordinate coord1 = new Coordinate(x1, y1);
Coordinate coord2 = new Coordinate(x2, y2);
double distance = C.GEO.distance(coord1, coord2);  // 单位：米

// 直接使用坐标值
double distance = C.GEO.distance(x1, y1, x2, y2);  // 单位：米
```

### 使用场景

```java
// 查找附近的位置
@Method(value = "查找附近商户", status = MethodStatus.COMPLETE)
public List<Merchant> findNearbyMerchant(double userX, double userY, double radius) {
    List<Merchant> all = DB.use().query();
    List<Merchant> nearby = new ArrayList<>();

    for (Merchant merchant : all) {
        double distance = C.GEO.distance(userX, userY, merchant.getX(), merchant.getY());
        if (distance <= radius) {
            merchant.setDistance(distance);
            nearby.add(merchant);
        }
    }

    // 按距离排序
    nearby.sort(Comparator.comparing(Merchant::getDistance));
    return nearby;
}

// 计算配送距离
@Method(value = "计算配送费", status = MethodStatus.COMPLETE)
public BigDecimal calculateDeliveryFee(double startX, double startY, double endX, double endY) {
    double distance = C.GEO.distance(startX, startY, endX, endY);

    // 配送费计算规则
    if (distance <= 1000) {
        return new BigDecimal("5.00");
    } else if (distance <= 3000) {
        return new BigDecimal("10.00");
    } else if (distance <= 5000) {
        return new BigDecimal("15.00");
    } else {
        throw new AppRuntimeException("超出配送范围");
    }
}

// 计算行驶距离
@Method(value = "计算里程", status = MethodStatus.COMPLETE)
public double calculateMileage(List<Coordinate> path) {
    double total = 0;

    for (int i = 0; i < path.size() - 1; i++) {
        double segment = C.GEO.distance(
            path.get(i).getX(), path.get(i).getY(),
            path.get(i + 1).getX(), path.get(i + 1).getY()
        );
        total += segment;
    }

    return total;  // 总里程（米）
}
```

---

## C.PUSH - 消息推送

实时消息推送（基于Redis Pub/Sub），详见[缓存、消息队列与推送操作指南](cache-mq-operations.md#cpush---消息推送)。

---

## C.MQ - 消息队列

消息队列发布和订阅，详见[缓存、消息队列与推送操作指南](cache-mq-operations.md#cmq---消息队列)。

---

## RunTimeConstant - 运行时常量

获取当前请求上下文，详见[常用工具类参考](common-tools.md#runtimeconstant---运行时常量)。

---
