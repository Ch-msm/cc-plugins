# 常用工具类

## `C.JSON`

```java
String json = C.JSON.toJson(user);
User entity = C.JSON.fromJson(json, User.class);
List<User> list = C.JSON.fromJsonToList(json, User.class);
Map<String, Object> map = C.JSON.toHashMap(user);
String pretty = C.JSON.prettyPrinterToJson(user);
```

适用场景：

- 服务间调用参数序列化
- 日志中的结构化输出
- DTO / Entity 转换

## `C.LOG`

```java
C.LOG.info("用户创建成功: {}", userId);
C.LOG.warn("配置缺失: {}", key);
C.LOG.error("保存失败", e);
C.LOG.catching(e);
```

规则：

- 普通业务日志使用 `C.LOG`
- 不要在服务代码里使用 `System.out.println`

## `C.HTTP`

真实可用的方法是：

```java
String text = C.HTTP.getOfString(url, headers);
String post = C.HTTP.postOfString(url, headers, C.JSON.toJson(data));
String delete = C.HTTP.deleteOfString(url, headers);
java.io.File file = C.HTTP.getOfFile(url, headers);
String form = C.HTTP.postForm(url, headers, formData);
```

SSE：

```java
var future = C.HTTP.SSE(
    "https://example.com/sse",
    "POST",
    Map.of("Content-Type", "application/json"),
    Map.of("query", "hello")
);
future.thenAccept(lines -> lines.forEach(System.out::println));
```

注意：

- 只使用本节列出的 HTTP 方法名
- HTTP 调用失败会抛出 `AppRuntimeException`

## `C.ASYNC`

```java
C.ASYNC.run(() -> doSomething()).join();

String result = C.ASYNC.supply(() -> loadSomething()).join();

C.ASYNC.dbRun(() -> saveToDb()).join();
```

补充：

- `run` / `supply` 使用通用线程池
- `dbRun` 使用数据库线程池
- 异步线程里不要直接调用 `context()`

## `C.TEXT`

常用方法：

```java
String shortId = C.TEXT.shortId();
String longId = C.TEXT.longId();
String md5 = C.TEXT.encrypt("123456");
String pinyin = C.TEXT.toPinyin("梅思铭");
String underscore = C.TEXT.lowerUnderscore("UserInfo");
String upper = C.TEXT.firstToUpperCase("user");
String lower = C.TEXT.firstToLowerCas("User");
String cipher = C.TEXT.symmetryEncrypt("secret");
String plain = C.TEXT.symmetryDecrypt(cipher);
```

注意：

- 文本工具主要负责 ID、命名转换、加密、拼音和对称加解密
- 判空应使用 `C.OBJECT.isEmpty(...)`

## `C.OBJECT`

```java
boolean empty = C.OBJECT.isEmpty(value);
boolean notEmpty = C.OBJECT.isNotEmpty(value);
boolean anyEmpty = C.OBJECT.isAnyEmpty(name, code, type);
boolean allEmpty = C.OBJECT.isAllEmpty(startTime, endTime);

User casted = C.OBJECT.cast(User.class, map);
User converted = C.OBJECT.convert(User.class, dto);
User instance = C.OBJECT.newInstance(User.class);
```

注意：

- 不存在 `toInteger/toLong/toString` 这类转换方法
- `isEmpty(0)` 会被视为空值，业务判断时要注意

## `C.DATA`

`toMap`：

```java
Map<String, List<User>> byDept = C.DATA.toMap(userList, User::getDepartmentId);
Map<String, String> idNameMap = C.DATA.toMap(userList, User::getId, User::getName);
```

集合关系判断：

```java
List<User> onlyInNew = C.DATA.firstExistSecondNotExist(newList, oldList, User::getId);
List<User> intersection = C.DATA.intersection(list1, list2, User::getId);
List<User> merged = C.DATA.merge(list1, list2);
```

异步数据注入：

```java
var loader = C.DATA.inject(userViewList, UserView::getId);
loader.add(UserView::getAttachmentList, ids ->
    AttachmentService.get("user", ids.stream().map(String::valueOf).toList())
);
loader.join();
```

注意：

- `C.DATA.toMap(list, key)` 返回的是 `Map<K, List<T>>`
- 如果想得到 `Map<K, V>`，要传第三个 `valueMapper`

## `C.TREE`

真实可用的方法是：

```java
List<Organization> directChildren = C.TREE.findChildNode(list, parentId, false);
List<Organization> allChildren = C.TREE.findChildNode(list, parentId, true);
```

要求：

- 节点对象里需要有 `id` 和 `parentId` 字段
- 不存在 `build(...)`、`findAllChildNode(...)` 这类方法

## `C.MATH`

```java
BigDecimal total = C.MATH.add(price, tax);
BigDecimal left = C.MATH.subtract(total, discount);
BigDecimal amount = C.MATH.multiply(price, count, 2);
BigDecimal avg = C.MATH.divide(total, BigDecimal.valueOf(3), 2);
```

## `C.GEO`

```java
double distance = C.GEO.distance(
    new Coordinate(120.63, 31.31),
    new Coordinate(120.64, 31.30)
);
```

## `C.MAIL`

```java
C.MAIL.sendText("a@example.com", "主题", "正文");
C.MAIL.sendHtml("a@example.com", "主题", "<b>正文</b>");
```

邮件配置来自配置中心的“邮箱服务”。

## `ClientStub`

服务间调用建议统一封装在 `dependency/` 下的依赖类中。

```java
var json = ClientStub.send(
    "/user-center/User/getApiUser",
    context(),
    C.JSON.toJson(Map.of("id", userId))
);
User user = C.JSON.fromJson(json, User.class);
```

要点：

- 第一个参数使用完整服务 URL
- 第二个参数传 `context()` 时会自动透传用户、调用链等头信息
- 如果没有上下文，可以传 `new RequestBody()`

## 配置工具

`C.CONFIG` 的详细说明单独见 [configuration.md](configuration.md)。
