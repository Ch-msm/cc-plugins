# 异常处理与响应完整指南

## 目录

- [AppRuntimeException - 业务异常](#appruntimeexception---业务异常)
- [异常处理模式](#异常处理模式)
- [R - 统一响应](#r---统一响应)
- [错误码定义](#错误码定义)
- [异常处理最佳实践](#异常处理最佳实践)

---

## AppRuntimeException - 业务异常

### 基本用法

```java
// 抛出简单异常
throw new AppRuntimeException("操作失败");

// 抛出带原因的异常
throw new AppRuntimeException("用户不存在", cause);

// 抛出带原因的异常（使用异常链）
try {
    doSomething();
} catch (Exception e) {
    throw new AppRuntimeException("处理失败", e);
}
```

### 常见异常场景

```java
// 数据验证异常
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void addUser(User user) {
    if (C.OBJECT.isEmpty(user.getName())) {
        throw new AppRuntimeException("用户名不能为空");
    }
    if (user.getAge() < 0 || user.getAge() > 150) {
        throw new AppRuntimeException("年龄范围不正确");
    }
    DB.use().insert(user);
}

// 数据不存在异常
@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public User getUser(String userId) {
    User user = DB.use().eq(User::getId, userId).get();
    if (user == null) {
        throw new AppRuntimeException("用户不存在: " + userId);
    }
    return user;
}

// 数据重复异常
@Method(value = "注册用户", status = MethodStatus.COMPLETE)
public void register(User user) {
    if (DB.use().eq(User::getPhone, user.getPhone()).exist()) {
        throw new AppRuntimeException("手机号已被注册");
    }
    DB.use().insert(user);
}

// 权限异常
@Method(value = "删除用户", status = MethodStatus.COMPLETE)
public void deleteUser(String userId) {
    var ctx = context();
    if (!ctx.isAdmin()) {
        throw new AppRuntimeException("无权删除用户");
    }
    DB.use().eq(User::getId, userId).delete();
}

// 业务逻辑异常
@Method(value = "扣减库存", status = MethodStatus.COMPLETE)
public void reduceStock(String productId, int quantity) {
    Product product = DB.use().eq(Product::getId, productId).get();
    if (product.getStock() < quantity) {
        throw new AppRuntimeException("库存不足，当前库存: " + product.getStock());
    }
    DB.use()
        .eq(Product::getId, productId)
        .updateCalculate(Product::getStock, "-" + quantity)
        .update();
}

// 状态异常
@Method(value = "取消订单", status = MethodStatus.COMPLETE)
public void cancelOrder(String orderId) {
    Order order = DB.use().eq(Order::getId, orderId).get();
    if (order.getStatus() != 0) {
        throw new AppRuntimeException("订单状态不允许取消");
    }
    DB.use().eq(Order::getId, orderId).update(Order::getStatus, 4);
}
```

---

## 异常处理模式

### 1. 参数校验异常

```java
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void insert(User user) {
    // 参数校验
    dataValidation(user, false);
    DB.use().insert(user);
}

private void dataValidation(User user, boolean update) {
    // 必填字段校验
    if (update && C.OBJECT.isEmpty(user.getId())) {
        throw new AppRuntimeException("ID必填");
    }

    if (C.OBJECT.isEmpty(user.getName())) {
        throw new AppRuntimeException("用户名不能为空");
    }

    if (C.OBJECT.isEmpty(user.getPhone())) {
        throw new AppRuntimeException("手机号不能为空");
    }

    // 格式校验
    if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
        throw new AppRuntimeException("手机号格式不正确");
    }

    // 数据重复校验
    if (DB.use()
        .notEq(User::getId, user.getId(), !update)
        .eq(User::getPhone, user.getPhone())
        .exist()) {
        throw new AppRuntimeException("手机号已存在");
    }

    // 业务规则校验
    if (user.getAge() < 18) {
        throw new AppRuntimeException("用户必须年满18岁");
    }
}
```

### 2. 数据库操作异常

```java
@Method(value = "更新用户", status = MethodStatus.COMPLETE)
public void update(User user) {
    try {
        dataValidation(user, true);
        int rows = DB.use().update(user);
        if (rows == 0) {
            throw new AppRuntimeException("用户不存在或数据未变更");
        }
    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        C.LOG.error("更新用户失败", e);
        throw new AppRuntimeException("更新失败，请稍后重试");
    }
}

@Method(value = "删除订单", status = MethodStatus.COMPLETE)
public void deleteOrder(String orderId) {
    try {
        // 开启事务
        DB.use().executeTransaction(() -> {
            // 检查订单状态
            Order order = orderDB.use().eq(Order::getId, orderId).get();
            if (order == null) {
                throw new AppRuntimeException("订单不存在");
            }

            // 删除订单明细
            orderDetailDB.use()
                .eq(OrderDetail::getOrderId, orderId)
                .delete();

            // 删除订单
            orderDB.use().eq(Order::getId, orderId).delete();
        });

    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        C.LOG.error("删除订单失败: {}", orderId, e);
        throw new AppRuntimeException("删除订单失败");
    }
}
```

### 3. 外部服务调用异常

```java
@Method(value = "发送短信", status = MethodStatus.COMPLETE)
public void sendSms(String phone, String code) {
    try {
        var url = "https://sms.api.com/send";
        var data = Map.of("phone", phone, "code", code);

        String response = C.HTTP.post(url, C.JSON.toJson(data));

        if (C.OBJECT.isEmpty(response)) {
            throw new AppRuntimeException("短信服务无响应");
        }

    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        C.LOG.error("发送短信失败: {}", phone, e);
        throw new AppRuntimeException("发送短信失败，请稍后重试");
    }
}

@Method(value = "调用用户服务", status = MethodStatus.COMPLETE)
public User getUserFromUserCenter(String userId) {
    try {
        var json = ClientStub.send(
            "/" + ServerName.USER_CENTER + "/User/get",
            new RequestBody(),
            C.JSON.toJson(userId)
        );

        return C.JSON.fromJson(json, User.class);

    } catch (Exception e) {
        C.LOG.error("调用用户服务失败: {}", userId, e);
        throw new AppRuntimeException("获取用户信息失败");
    }
}
```

### 4. 文件操作异常

```java
@Method(value = "上传文件", status = MethodStatus.COMPLETE)
public String upload(File file) {
    try {
        // 验证文件
        if (file == null) {
            throw new AppRuntimeException("请选择文件");
        }

        if (file.length() > 100 * 1024 * 1024) {
            throw new AppRuntimeException("文件大小超过限制（100MB）");
        }

        // 上传文件
        FileId fileId = C.FILE.upload(file);

        return fileId.getId();

    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        C.LOG.error("上传文件失败", e);
        throw new AppRuntimeException("上传文件失败");
    }
}

@Method(value = "导出Excel", status = MethodStatus.COMPLETE)
public String exportExcel(List<ExportItem> items, List<?> data) {
    try {
        return File.EXECL.export("数据导出", items, data);
    } catch (Exception e) {
        C.LOG.error("导出Excel失败", e);
        throw new AppRuntimeException("导出失败");
    }
}
```

### 5. 缓存操作异常

```java
@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public User getUser(String userId) {
    try {
        // 先从缓存获取
        String cacheKey = "user:" + userId;
        String cached = C.CACHE.get(cacheKey);
        if (cached != null) {
            return C.JSON.fromJson(cached, User.class);
        }

        // 从数据库查询
        User user = DB.use().eq(User::getId, userId).get();
        if (user != null) {
            // 写入缓存
            C.CACHE.set(cacheKey, C.JSON.toJson(user), 1800);
        }

        return user;

    } catch (Exception e) {
        C.LOG.error("获取用户失败: {}", userId, e);
        throw new AppRuntimeException("获取用户信息失败");
    }
}
```

---

## R - 统一响应

### 响应结构

```java
@Data
@Entity("统一返回结果")
public class R {
    // 响应码：0-成功 1-业务异常 2-运行时异常 3-连接超时
    private byte code = 0;

    // 响应消息
    private String message = "成功";

    // 响应数据
    private Object data;
}
```

### 响应码说明

| code | 含义 | 说明 |
|------|------|------|
| 0 | 成功 | 请求处理成功 |
| 1 | 业务异常 | AppRuntimeException异常 |
| 2 | 运行时异常 | 系统运行时异常 |
| 3 | 连接超时 | 服务间调用超时 |

### 框架自动处理

```java
// 框架会自动捕获异常并转换为R响应
// 开发者只需抛出AppRuntimeException即可

@Method(value = "删除用户", status = MethodStatus.COMPLETE)
public void deleteUser(String userId) {
    // 参数校验
    if (C.OBJECT.isEmpty(userId)) {
        throw new AppRuntimeException("用户ID不能为空");
        // 框架自动转换为: {code: 1, message: "用户ID不能为空", data: null}
    }

    // 业务校验
    if (!canDelete(userId)) {
        throw new AppRuntimeException("该用户有关联数据，无法删除");
        // 框架自动转换为: {code: 1, message: "该用户有关联数据，无法删除", data: null}
    }

    DB.use().eq(User::getId, userId).delete();
    // 框架自动转换为: {code: 0, message: "成功", data: null}
}

@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public User getUser(String userId) {
    User user = DB.use().eq(User::getId, userId).get();
    if (user == null) {
        throw new AppRuntimeException("用户不存在");
    }
    return user;
    // 框架自动转换为: {code: 0, message: "成功", data: {id: "xxx", name: "xxx"}}
}
```

---

## 错误码定义

### 创建错误码常量

```java
/**
 * 错误码常量
 */
public class ErrorCode {

    private ErrorCode() {}

    // 通用错误码（1-999）
    public static final int SUCCESS = 0;
    public static final int BUSINESS_ERROR = 1;
    public static final int RUNTIME_ERROR = 2;
    public static final int TIMEOUT_ERROR = 3;

    // 参数错误（1000-1999）
    public static final int PARAM_ERROR = 1000;
    public static final int PARAM_EMPTY = 1001;
    public static final int PARAM_INVALID = 1002;

    // 用户错误（2000-2999）
    public static final int USER_NOT_FOUND = 2000;
    public static final int USER_EXISTS = 2001;
    public static final int USER_DISABLED = 2002;
    public static final int PASSWORD_ERROR = 2003;

    // 权限错误（3000-3999）
    public static final int NO_PERMISSION = 3000;
    public static final int NOT_LOGIN = 3001;
    public static final int TOKEN_EXPIRED = 3002;

    // 数据错误（4000-4999）
    public static final int DATA_NOT_FOUND = 4000;
    public static final int DATA_EXISTS = 4001;
    public static final int DATA_CONFLICT = 4002;

    // 业务错误（5000-5999）
    public static final int STOCK_INSUFFICIENT = 5000;
    public static final int ORDER_PAID = 5001;
    public static final int ORDER_CANCELLED = 5002;
}
```

### 创建业务异常类

```java
/**
 * 业务异常类（带错误码）
 */
public class BusinessException extends AppRuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

### 使用错误码

```java
@Service(value = "用户服务", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "获取用户", status = MethodStatus.COMPLETE)
    public User getUser(String userId) {
        if (C.OBJECT.isEmpty(userId)) {
            throw new BusinessException(ErrorCode.PARAM_EMPTY, "用户ID不能为空");
        }

        User user = DB.use().eq(User::getId, userId).get();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        return user;
    }

    @Method(value = "注册用户", status = MethodStatus.COMPLETE)
    public void register(User user) {
        // 校验参数
        if (C.OBJECT.isEmpty(user.getPhone())) {
            throw new BusinessException(ErrorCode.PARAM_EMPTY, "手机号不能为空");
        }

        if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "手机号格式不正确");
        }

        // 检查是否已注册
        if (DB.use().eq(User::getPhone, user.getPhone()).exist()) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "手机号已被注册");
        }

        // 保存用户
        user.setId(C.TEXT.longId());
        user.setStatus(1);
        DB.use().insert(user);
    }

    @Method(value = "登录", status = MethodStatus.COMPLETE)
    public LoginResult login(String phone, String password) {
        // 查询用户
        User user = DB.use().eq(User::getPhone, phone).get();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // 检查状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        // 验证密码
        if (!user.getPassword().equals(encrypt(password))) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "密码错误");
        }

        // 返回登录结果
        return new LoginResult(user);
    }
}
```

---

## 异常处理最佳实践

### 1. 异常消息规范

```java
// ✅ 推荐：清晰明确的错误消息
throw new AppRuntimeException("用户名不能为空");
throw new AppRuntimeException("手机号已被注册");
throw new AppRuntimeException("库存不足，当前库存: 10");
throw new AppRuntimeException("订单状态不允许取消");

// ❌ 不推荐：模糊的错误消息
throw new AppRuntimeException("操作失败");
throw new AppRuntimeException("系统错误");
throw new AppRuntimeException("参数错误");
```

### 2. 异常处理层次

```java
// ✅ 推荐：分层处理异常
@Method(value = "业务方法", status = MethodStatus.COMPLETE)
public void businessMethod(User user) {
    // 第一层：参数校验
    if (C.OBJECT.isEmpty(user.getName())) {
        throw new AppRuntimeException("用户名不能为空");
    }

    // 第二层：业务校验
    if (DB.use().eq(User::getPhone, user.getPhone()).exist()) {
        throw new AppRuntimeException("手机号已被注册");
    }

    // 第三层：数据操作（捕获并转换为业务异常）
    try {
        DB.use().insert(user);
    } catch (Exception e) {
        C.LOG.error("保存用户失败", e);
        throw new AppRuntimeException("保存失败，请稍后重试");
    }
}
```

### 3. 异常日志记录

```java
// ✅ 推荐：记录异常日志
@Method(value = "处理订单", status = MethodStatus.COMPLETE)
public void processOrder(String orderId) {
    try {
        Order order = DB.use().eq(Order::getId, orderId).get();
        if (order == null) {
            C.LOG.warn("订单不存在: {}", orderId);
            throw new AppRuntimeException("订单不存在");
        }

        // 处理订单...
    } catch (AppRuntimeException e) {
        // 业务异常不记录堆栈（减少日志量）
        C.LOG.warn("处理订单失败: {}, {}", orderId, e.getMessage());
        throw e;
    } catch (Exception e) {
        // 系统异常记录完整堆栈
        C.LOG.error("处理订单异常: {}", orderId, e);
        throw new AppRuntimeException("处理订单失败");
    }
}
```

### 4. 异常与事务

```java
// ✅ 推荐：异常导致事务回滚
@Method(value = "转账", status = MethodStatus.COMPLETE)
public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
    try {
        DB.use().executeTransaction(() -> {
            // 扣款
            accountDB.use()
                .eq(Account::getId, fromAccount)
                .updateCalculate(Account::getBalance, "-" + amount)
                .update();

            // 检查余额
            Account from = accountDB.use().eq(Account::getId, fromAccount).get();
            if (from.getBalance() < 0) {
                throw new AppRuntimeException("余额不足");
            }

            // 加款
            accountDB.use()
                .eq(Account::getId, toAccount)
                .updateCalculate(Account::getBalance, "+" + amount)
                .update();
        });

    } catch (AppRuntimeException e) {
        // 业务异常：事务已回滚
        throw e;
    } catch (Exception e) {
        // 系统异常：事务已回滚
        C.LOG.error("转账失败", e);
        throw new AppRuntimeException("转账失败，请稍后重试");
    }
}
```

### 5. 异常与资源清理

```java
// ✅ 推荐：使用try-with-resources
@Method(value = "处理文件", status = MethodStatus.COMPLETE)
public void processFile(String fileId) {
    File file = null;
    try {
        file = C.FILE.download(fileId);

        // 处理文件...
        processFileContent(file);

    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        C.LOG.error("处理文件失败", e);
        throw new AppRuntimeException("处理文件失败");
    }
    // 注意：C.FILE.download()返回的File不需要手动关闭
}
```

### 6. 异常转换

```java
// ✅ 推荐：将底层异常转换为业务异常
@Method(value = "调用外部服务", status = MethodStatus.COMPLETE)
public ExternalData callExternalService(String params) {
    try {
        String response = C.HTTP.post("https://api.example.com", params);
        return C.JSON.fromJson(response, ExternalData.class);

    } catch (SocketTimeoutException e) {
        throw new AppRuntimeException("外部服务超时，请稍后重试");
    } catch (ConnectException e) {
        throw new AppRuntimeException("无法连接到外部服务");
    } catch (Exception e) {
        C.LOG.error("调用外部服务失败", e);
        throw new AppRuntimeException("外部服务调用失败");
    }
}
```

---

## 完整示例

### 标准CRUD异常处理

```java
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    private static final MainDB<User> DB = new MainDB<>(User.class);

    // 新增
    @Method(value = "新增", status = MethodStatus.COMPLETE)
    public void insert(User user) {
        try {
            dataValidation(user, false);

            if (C.OBJECT.isEmpty(user.getId())) {
                user.setId(C.TEXT.longId());
            }

            DB.use().insert(user);

        } catch (AppRuntimeException e) {
            throw e;
        } catch (Exception e) {
            C.LOG.error("新增用户失败", e);
            throw new AppRuntimeException("新增失败");
        }
    }

    // 更新
    @Method(value = "更新", status = MethodStatus.COMPLETE)
    public void update(User user) {
        try {
            dataValidation(user, true);

            int rows = DB.use().update(user);
            if (rows == 0) {
                throw new AppRuntimeException("用户不存在");
            }

        } catch (AppRuntimeException e) {
            throw e;
        } catch (Exception e) {
            C.LOG.error("更新用户失败", e);
            throw new AppRuntimeException("更新失败");
        }
    }

    // 删除
    @Method(value = "删除", status = MethodStatus.COMPLETE)
    public void delete(String id) {
        try {
            if (C.OBJECT.isEmpty(id)) {
                throw new AppRuntimeException("ID不能为空");
            }

            User user = DB.use().eq(User::getId, id).get();
            if (user == null) {
                throw new AppRuntimeException("用户不存在");
            }

            // 检查是否有关联数据
            if (hasRelatedData(id)) {
                throw new AppRuntimeException("该用户有关联数据，无法删除");
            }

            DB.use().eq(User::getId, id).delete();

        } catch (AppRuntimeException e) {
            throw e;
        } catch (Exception e) {
            C.LOG.error("删除用户失败", e);
            throw new AppRuntimeException("删除失败");
        }
    }

    // 获取
    @Method(value = "获取", status = MethodStatus.COMPLETE)
    public User get(String id) {
        try {
            if (C.OBJECT.isEmpty(id)) {
                throw new AppRuntimeException("ID不能为空");
            }

            User user = DB.use().eq(User::getId, id).get();
            if (user == null) {
                throw new AppRuntimeException("用户不存在");
            }

            return user;

        } catch (AppRuntimeException e) {
            throw e;
        } catch (Exception e) {
            C.LOG.error("获取用户失败", e);
            throw new AppRuntimeException("获取失败");
        }
    }

    // 数据校验
    private void dataValidation(User user, boolean update) {
        if (update && C.OBJECT.isEmpty(user.getId())) {
            throw new AppRuntimeException("ID必填");
        }

        if (C.OBJECT.isEmpty(user.getName())) {
            throw new AppRuntimeException("用户名不能为空");
        }

        if (C.OBJECT.isEmpty(user.getPhone())) {
            throw new AppRuntimeException("手机号不能为空");
        }

        if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new AppRuntimeException("手机号格式不正确");
        }

        if (DB.use()
            .notEq(User::getId, user.getId(), !update)
            .eq(User::getPhone, user.getPhone())
            .exist()) {
            throw new AppRuntimeException("手机号已存在");
        }
    }

    // 检查关联数据
    private boolean hasRelatedData(String userId) {
        return orderDB.use().eq(Order::getUserId, userId).exist();
    }
}
```
