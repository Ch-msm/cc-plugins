# @Method 注解完整指南

## 目录

- [@Method 注解概述](#method-注解概述)
- [注解属性详解](#注解属性详解)
- [方法类型与访问控制](#方法类型与访问控制)
- [参数注解 @Parameter](#参数注解-parameter)
- [返回数据注解 @ReturnData](#返回数据注解-returndata)
- [注解使用最佳实践](#注解使用最佳实践)

---

## @Method 注解概述

### 基本用法

```java
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "添加用户", status = MethodStatus.COMPLETE)
    public void addUser(User user) {
        DB.use().insert(user);
    }
}
```

### 注解作用

1. **服务方法注册**：自动注册为HTTP接口
2. **元数据管理**：记录方法的描述、状态、作者等信息
3. **日志控制**：控制方法调用的日志打印
4. **权限控制**：配合nature属性控制访问权限
5. **敏感数据处理**：标记敏感数据字段

---

## 注解属性详解

### value - 方法名称

```java
// 基本用法
@Method(value = "添加用户")
public void addUser(User user) { }

// 详细描述
@Method(value = "根据用户ID查询用户信息")
public User getUser(String userId) { }

// 业务操作
@Method(value = "用户登录验证")
public LoginResult login(String username, String password) { }
```

### status - 开发状态

```java
// 已完成
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
public User getUser(String id) { }

// 开发中
@Method(value = "批量导入", status = MethodStatus.DEVELOPING)
public void batchImport(List<User> list) {
    throw new AppRuntimeException("功能开发中");
}

// 需要修改
@Method(value = "数据统计", status = MethodStatus.NEED_TO_MODIFY)
public Statistics getStatistics() {
    // TODO: 需要优化性能
}
```

**MethodStatus枚举值：**
- `COMPLETE` - 已完成（生产可用）
- `DEVELOPING` - 开发中（不可用）
- `NEED_TO_MODIFY` - 需要修改（需要改进）

### printLog - 日志控制

```java
// 开启日志（默认）
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void addUser(User user) {
    // 方法调用时会自动打印日志
}

// 禁用日志（敏感数据方法）
@Method(value = "登录验证", status = MethodStatus.COMPLETE, printLog = false)
public LoginResult login(String username, String password) {
    // 不会打印密码等敏感信息
}

// 禁用日志（高频调用方法）
@Method(value = "上报测点数据", status = MethodStatus.COMPLETE, printLog = false)
public void reportPointData(PointData data) {
    // 高频调用，禁用日志避免性能影响
}
```

### nature - 访问权限

```java
// 公开接口（默认，所有人可访问）
@Method(value = "用户注册", status = MethodStatus.COMPLETE,
         nature = MethodNature.PUBLIC)
public void register(User user) { }

// 受控接口（需要权限验证）
@Method(value = "删除用户", status = MethodStatus.COMPLETE,
         nature = MethodNature.CONTROLLED)
public void deleteUser(String userId) {
    // 框架会自动验证权限
}

// 内部接口（仅服务内部调用）
@Method(value = "数据初始化", status = MethodStatus.COMPLETE,
         nature = MethodNature.PROTECTED)
public void initData() {
    // 只能通过服务间调用，外部无法访问
}
```

**MethodNature枚举值：**
- `PUBLIC` - 公开接口（默认，所有人可访问）
- `PROTECTED` - 受保护接口（仅内部调用）
- `CONTROLLED` - 受控接口（需要权限验证）

### author - 作者信息

```java
@Method(value = "数据同步", status = MethodStatus.COMPLETE,
         author = "梅思铭")
public void syncData() { }

// 团队协作
@Method(value = "支付回调", status = MethodStatus.COMPLETE,
         author = "张三")
public void paymentCallback(PaymentData data) { }
```

### date - 创建日期

```java
@Method(value = "数据导出", status = MethodStatus.COMPLETE,
         author = "梅思铭", date = "2025-01-08")
public String exportData(Search search) { }

// 使用LocalDate
@Method(value = "报表生成", status = MethodStatus.COMPLETE,
         author = "李四", date = "2025-01-07")
public String generateReport(ReportConfig config) { }
```

### explain - 方法说明

```java
// 详细说明方法逻辑
@Method(value = "批量删除", status = MethodStatus.COMPLETE,
         explain = "批量删除数据，同时删除关联数据和缓存")
public void batchDelete(List<String> ids) {
    // 删除主数据
    DB.use().in(Entity::getId, ids).delete();

    // 删除关联数据
    subDB.use().in(SubEntity::getMainId, ids).delete();

    // 清除缓存
    ids.forEach(id -> C.CACHE.del("data:" + id));
}

// 说明注意事项
@Method(value = "数据迁移", status = MethodStatus.COMPLETE,
         explain = "数据迁移时需要停服，迁移完成后需要重启")
public void migrateData() { }

// 说明性能影响
@Method(value = "全量统计", status = MethodStatus.COMPLETE,
         explain = "全量统计数据量大时较慢，建议使用缓存")
public Statistics getFullStatistics() { }
```

### sensitiveData - 敏感数据字段

```java
// 标记敏感字段（日志中会脱敏）
@Method(value = "用户注册", status = MethodStatus.COMPLETE,
         sensitiveData = {"password", "idCard", "phone"})
public void register(User user) {
    // password、idCard、phone字段在日志中会显示为 ****
    DB.use().insert(user);
}

// 多个敏感字段
@Method(value = "绑定银行卡", status = MethodStatus.COMPLETE,
         sensitiveData = {"cardNo", "cvv", "expireDate"})
public void bindCard(BankCard card) {
    // 银行卡信息在日志中脱敏
}

// 敏感查询参数
@Method(value = "查询订单", status = MethodStatus.COMPLETE,
         sensitiveData = {"orderId", "payPassword"})
public Order getOrder(String orderId, String payPassword) {
    // 敏感参数在日志中脱敏
}
```

---

## 方法类型与访问控制

### 公开方法（PUBLIC）

```java
// 无需登录即可访问
@Method(value = "用户登录", status = MethodStatus.COMPLETE,
         nature = MethodNature.PUBLIC)
public LoginResult login(String username, String password) {
    // 验证用户名密码
    // 返回登录结果
}

// 需要登录但无特殊权限
@Method(value = "获取个人信息", status = MethodStatus.COMPLETE,
         nature = MethodNature.PUBLIC)
public UserInfo getMyInfo() {
    var ctx = context();
    return UserCenter.getUser(ctx.getUserId());
}
```

### 受控方法（CONTROLLED）

```java
// 需要特定权限才能访问
@Method(value = "删除用户", status = MethodStatus.COMPLETE,
         nature = MethodNature.CONTROLLED)
public void deleteUser(String userId) {
    // 框架会自动验证用户是否有删除权限
    DB.use().eq(User::getId, userId).delete();
}

// 需要管理员权限
@Method(value = "系统配置", status = MethodStatus.COMPLETE,
         nature = MethodNature.CONTROLLED)
public void updateConfig(Config config) {
    // 只有管理员可以调用
    DB.use().update(config);
}

// 需要数据所有者权限
@Method(value = "修改个人资料", status = MethodStatus.COMPLETE,
         nature = MethodNature.CONTROLLED)
public void updateProfile(UserProfile profile) {
    // 验证是否为本人
    var ctx = context();
    if (!ctx.getUserId().equals(profile.getUserId())) {
        throw new AppRuntimeException("无权修改");
    }
    DB.use().update(profile);
}
```

### 受保护方法（PROTECTED）

```java
// 仅服务内部调用
@Method(value = "数据初始化", status = MethodStatus.COMPLETE,
         nature = MethodNature.PROTECTED)
public void initData() {
    // 只能通过服务间调用或本地调用
    // 外部HTTP请求无法访问
}

// 定时任务方法
@Method(value = "清理过期数据", status = MethodStatus.COMPLETE,
         nature = MethodNature.PROTECTED)
public void cleanExpiredData() {
    // 定时任务调用，不对外暴露
}

// 消息订阅处理（通常与@Subscribe配合）
@Subscribe(Constant.QueueKafka.DATA_CHANGE)
@Method(value = "处理数据变更", status = MethodStatus.COMPLETE,
         nature = MethodNature.PROTECTED)
public void handleDataChange(String dataId) {
    // 只处理消息队列推送的数据
}
```

---

## 参数注解 @Parameter

### 基本用法

```java
// 单个参数
@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public User getUser(
    @Parameter(value = "用户ID", required = true) String userId
) {
    return DB.use().eq(User::getId, userId).get();
}

// 多个参数
@Method(value = "更新用户", status = MethodStatus.COMPLETE)
public void updateUser(
    @Parameter(value = "用户ID") String userId,
    @Parameter(value = "用户名") String name,
    @Parameter(value = "状态") Integer status
) {
    DB.use().eq(User::getId, userId)
        .update(User::getName, name,
                User::getStatus, status);
}
```

### required - 必填参数

```java
// 必填参数
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void addUser(
    @Parameter(value = "用户名", required = true) String username,
    @Parameter(value = "密码", required = true) String password,
    @Parameter(value = "邮箱") String email  // 可选
) {
    // 框架会自动验证必填参数
    User user = new User();
    user.setUsername(username);
    user.setPassword(password);
    user.setEmail(email);
    DB.use().insert(user);
}

// 复杂对象必填字段
@Method(value = "保存订单", status = MethodStatus.COMPLETE)
public void saveOrder(
    @Parameter(value = "订单信息", required = true) Order order
) {
    dataValidation(order, false);
    DB.use().insert(order);
}
```

### 参数描述

```java
// 详细描述参数用途
@Method(value = "查询订单", status = MethodStatus.COMPLETE)
public DataList<Order> findOrders(
    @Parameter(value = "开始时间（时间戳）") Long startTime,
    @Parameter(value = "结束时间（时间戳）") Long endTime,
    @Parameter(value = "订单状态列表") List<Integer> statusList,
    @Parameter(value = "关键词（订单号/客户名称）") String keyword
) {
    // ...
}
```

---

## 返回数据注解 @ReturnData

### 基本用法

```java
// 简单类型返回
@Method(value = "获取用户名", status = MethodStatus.COMPLETE)
@ReturnData("用户名")
public String getUserName(String userId) {
    return DB.use().eq(User::getId, userId).get().getName();
}

// 对象返回
@Method(value = "获取用户", status = MethodStatus.COMPLETE)
@ReturnData("用户信息")
public User getUser(String userId) {
    return DB.use().eq(User::getId, userId).get();
}

// 集合返回
@Method(value = "获取用户列表", status = MethodStatus.COMPLETE)
@ReturnData("用户列表")
public List<User> getUserList() {
    return DB.use().query();
}

// 分页返回
@Method(value = "查询", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> find(Search search) {
    var dataList = new DataList<User>();
    var db = getSearchDB(search);

    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());
    }

    dataList.setList(
        db.paging(search.getPageNo(), search.getPageSize())
            .query()
    );

    return dataList;
}
```

### type - 返回类型

```java
// 指定泛型类型
@Method(value = "查询订单", status = MethodStatus.COMPLETE)
@ReturnData(type = Order.class)
public DataList<Order> findOrders(Search search) {
    // ...
}

// 指定视图类型
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
@ReturnData(type = UserView.class)
public List<UserView> findUsers(Search search) {
    return DB.use().query(UserView.class);
}
```

---

## 注解使用最佳实践

### 1. 完整的注解信息

```java
// ✅ 推荐：提供完整的注解信息
@Method(
    value = "批量删除用户",
    status = MethodStatus.COMPLETE,
    author = "梅思铭",
    date = "2025-01-08",
    explain = "批量删除用户，同时删除关联的订单和数据",
    nature = MethodNature.CONTROLLED
)
public void batchDeleteUsers(List<String> userIds) {
    // 实现
}

// ❌ 不推荐：信息不完整
@Method("删除")
public void delete(List<String> ids) {
    // 缺少状态、作者等信息
}
```

### 2. 合理的status状态

```java
// ✅ 推荐：明确标识开发状态
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void addUser(User user) {
    // 已完成，可以投入使用
}

@Method(value = "数据迁移", status = MethodStatus.DEVELOPING)
public void migrateData() {
    throw new AppRuntimeException("功能开发中");
}

@Method(value = "报表统计", status = MethodStatus.NEED_TO_MODIFY)
public Statistics getStatistics() {
    // 功能可用但需要优化
}
```

### 3. 正确的nature使用

```java
// ✅ 推荐：根据接口性质设置访问权限
// 公开接口
@Method(value = "用户注册", nature = MethodNature.PUBLIC)
public void register(User user) { }

// 受控接口
@Method(value = "删除用户", nature = MethodNature.CONTROLLED)
public void deleteUser(String userId) { }

// 内部接口
@Method(value = "数据同步", nature = MethodNature.PROTECTED)
@Subscribe("data.sync")
public void syncData(String json) { }
```

### 4. 敏感数据处理

```java
// ✅ 推荐：标记敏感数据
@Method(
    value = "用户登录",
    status = MethodStatus.COMPLETE,
    printLog = false,           // 禁用日志
    sensitiveData = {"password", "idCard"}  // 敏感字段
)
public LoginResult login(String username, String password) {
    // 密码不会出现在日志中
}
```

### 5. 参数验证注解

```java
// ✅ 推荐：使用@Parameter标记必填参数
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void addUser(
    @Parameter(value = "用户信息", required = true) User user
) {
    // 框架会自动验证参数
}

// ✅ 推荐：详细描述参数
@Method(value = "查询", status = MethodStatus.COMPLETE)
public DataList<User> find(
    @Parameter(value = "开始时间（毫秒时间戳）") Long startTime,
    @Parameter(value = "结束时间（毫秒时间戳）") Long endTime,
    @Parameter(value = "关键词（用户名/手机号/邮箱）") String keyword
) {
    // 参数描述清晰
}
```

### 6. 返回数据注解

```java
// ✅ 推荐：明确返回数据类型
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> find(Search search) {
    // ...
}

// ✅ 推荐：描述返回数据
@Method(value = "获取用户", status = MethodStatus.COMPLETE)
@ReturnData("用户详细信息")
public User getUser(String userId) {
    // ...
}
```

---

## 完整示例

### 标准CRUD服务

```java
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    private static final MainDB<User> DB = new MainDB<>(User.class);

    // 添加
    @Method(
        value = "添加用户",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.CONTROLLED
    )
    public void insert(
        @Parameter(value = "用户信息", required = true) User user
    ) {
        dataValidation(user, false);
        DB.use().insert(user);
    }

    // 更新
    @Method(
        value = "更新用户",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.CONTROLLED
    )
    public void update(
        @Parameter(value = "用户信息", required = true) User user
    ) {
        dataValidation(user, true);
        DB.use().update(user);
    }

    // 删除
    @Method(
        value = "删除用户",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.CONTROLLED,
        explain = "删除用户同时删除关联数据"
    )
    public void delete(
        @Parameter(value = "用户ID", required = true) String userId
    ) {
        // 删除关联数据
        orderDB.use().eq(Order::getUserId, userId).delete();

        // 删除用户
        DB.use().eq(User::getId, userId).delete();
    }

    // 查询
    @Method(
        value = "查询用户",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.PUBLIC
    )
    @ReturnData(type = User.class)
    public DataList<User> find(
        @Parameter(value = "查询条件") Search search
    ) {
        var dataList = new DataList<User>();
        var db = getSearchDB(search);

        if (search.getPageNo() != -1) {
            dataList.setTotal(db.count());
        }

        dataList.setList(
            db.paging(search.getPageNo(), search.getPageSize())
                .orderByDesc(User::getCreateTime)
                .query()
        );

        return dataList;
    }

    // 获取单个
    @Method(
        value = "获取用户",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.PUBLIC
    )
    @ReturnData("用户信息")
    public User get(
        @Parameter(value = "用户ID", required = true) String userId
    ) {
        return DB.use().eq(User::getId, userId).get();
    }

    // 构建查询条件
    private DB<User> getSearchDB(Search search) {
        return DB.use()
            .eq(User::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
            .in(User::getStatus, search.getStatus(), C.OBJECT.isEmpty(search.getStatus()))
            .iLike(List.of(User::getName, User::getPhone, User::getEmail),
                   "%" + search.getKeyword() + "%",
                   C.OBJECT.isEmpty(search.getKeyword()))
            .orderByDesc(User::getCreateTime);
    }

    // 数据校验
    private void dataValidation(User user, boolean update) {
        if (update && C.OBJECT.isEmpty(user.getId())) {
            throw new AppRuntimeException("ID必填");
        }

        if (DB.use()
            .notEq(User::getId, user.getId(), !update)
            .eq(User::getPhone, user.getPhone())
            .exist()) {
            throw new AppRuntimeException("手机号已存在");
        }
    }
}
```

### 定时任务服务

```java
@Service(value = "数据清理", author = "梅思铭", date = "2025-01-08")
public class DataCleanService extends AbstractService {

    // 清理过期数据（受保护，定时任务调用）
    @Method(
        value = "清理过期数据",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.PROTECTED,
        explain = "清理30天前的过期数据"
    )
    public void cleanExpiredData() {
        var expireTime = C.TIME.localTimestamp() - (30 * 24 * 3600 * 1000L);

        // 删除过期数据
        DB.use()
            .lt(Data::getCreateTime, expireTime)
            .eq(Data::getStatus, 0)
            .delete();

        C.LOG.info("清理过期数据完成");
    }

    // 数据归档（受保护，定时任务调用）
    @Method(
        value = "数据归档",
        status = MethodStatus.COMPLETE,
        nature = MethodNature.PROTECTED,
        explain = "将90天前的数据归档到历史表"
    )
    public void archiveData() {
        var archiveTime = C.TIME.localTimestamp() - (90 * 24 * 3600 * 1000L);

        // 查询需要归档的数据
        var list = DB.use()
            .lt(Data::getCreateTime, archiveTime)
            .query();

        // 插入到历史表
        historyDB.use().insert(list);

        // 删除原数据
        DB.use()
            .in(Data::getId, list.stream().map(Data::getId).toList())
            .delete();

        C.LOG.info("数据归档完成: {}条", list.size());
    }
}
```
