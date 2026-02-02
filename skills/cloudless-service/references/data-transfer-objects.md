# 数据传输对象完整指南

## 目录

- [RequestBody - 请求上下文](#requestbody---请求上下文)
- [DataList - 分页响应](#datalist---分页响应)
- [Search - 查询条件](#search---查询条件)
- [自定义DTO](#自定义dto)
- [DTO最佳实践](#dto最佳实践)

---

## RequestBody - 请求上下文

### 基本结构

```java
@Data
public class RequestBody {
    // 请求URL
    private String url;

    // Token授权码
    private String token;

    // API授权
    private String apiToken;

    // 数据权限ID
    private String dataPermissionId;

    // 终端类型（1:PC端, 2:移动端）
    private int deviceType = 1;

    // 请求体（JSON字符串）
    private String body;

    // 真实IP
    private String realIp;

    // 接口映射
    private MethodMapping methodMapping;

    // 是否内部调用
    private boolean inner;

    // 调用链ID
    private String traceId;

    // 用户ID
    private int userId;

    // 用户名
    private String username = "";

    // 用户姓名
    private String user = "";

    // 数据域
    private List<String> dataScope = List.of();

    // 请求头信息
    private Map<String, String> header;

    // 响应头
    private Map<String, String> responseHeader;

    // 请求方法（GET/POST等）
    private String method;
}
```

### 获取请求上下文

```java
// 方法1：在Service中使用context()方法
@Service(value = "用户服务", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "获取当前用户", status = MethodStatus.COMPLETE)
    public User getCurrentUser() {
        // 获取请求上下文
        RequestBody ctx = context();

        // 获取用户信息
        int userId = ctx.getUserId();
        String username = ctx.getUsername();
        String user = ctx.getUser();

        C.LOG.info("当前用户: {}({})", username, userId);

        return DB.use().eq(User::getId, String.valueOf(userId)).get();
    }
}

// 方法2：使用RunTimeConstant
@Method(value = "获取IP地址", status = MethodStatus.COMPLETE)
public String getIp() {
    RequestBody ctx = RunTimeConstant.requestBody.get();
    return ctx.getRealIp();
}
```

### 常用属性访问

```java
@Method(value = "请求信息", status = MethodStatus.COMPLETE)
public Map<String, Object> getRequestInfo() {
    RequestBody ctx = context();

    return Map.of(
        "userId", ctx.getUserId(),
        "username", ctx.getUsername(),
        "user", ctx.getUser(),
        "ip", ctx.getRealIp(),
        "deviceType", ctx.getDeviceType(),
        "token", ctx.getToken(),
        "traceId", ctx.getTraceId(),
        "dataScope", ctx.getDataScope(),
        "isInner", ctx.isInner()
    );
}
```

### 用户信息获取

```java
// 获取当前用户ID
@Method(value = "获取用户ID", status = MethodStatus.COMPLETE)
public int getCurrentUserId() {
    return context().getUserId();
}

// 获取当前用户信息（通过服务间调用）
@Method(value = "获取登录用户", status = MethodStatus.COMPLETE)
public LoginUser getLoginUser() {
    var ctx = context();

    // 调用用户中心获取完整用户信息
    var json = ClientStub.send(
        "/" + ServerName.USER_CENTER + "/Authenticate/getLoginUser",
        ctx,
        C.JSON.toJson(Map.of("token", ctx.getToken()))
    );

    return C.JSON.fromJson(json, LoginUser.class);
}
```

### 数据权限使用

```java
// 使用数据域过滤数据
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
public DataList<User> findUsers() {
    var ctx = context();
    var dataScope = ctx.getDataScope();

    var db = DB.use();

    // 如果有数据权限限制
    if (C.OBJECT.isNotEmpty(dataScope) && !dataScope.isEmpty()) {
        db.in(User::getDepartmentId, dataScope);
    }

    var list = db.query();
    var dataList = new DataList<User>();
    dataList.setTotal(list.size());
    dataList.setList(list);

    return dataList;
}
```

### 终端类型判断

```java
@Method(value = "上传文件", status = MethodStatus.COMPLETE)
public String uploadFile(File file) {
    var ctx = context();

    // 根据终端类型处理
    if (ctx.getDeviceType() == 1) {
        // PC端
        C.LOG.info("PC端上传");
    } else if (ctx.getDeviceType() == 2) {
        // 移动端
        C.LOG.info("移动端上传");
    }

    return C.FILE.upload(file).getId();
}
```

### 内部调用判断

```java
@Method(value = "敏感操作", status = MethodStatus.COMPLETE)
public void sensitiveOperation() {
    var ctx = context();

    // 仅允许内部调用
    if (!ctx.isInner()) {
        throw new AppRuntimeException("无权访问");
    }

    // 执行敏感操作
}
```

### 调用链追踪

```java
@Method(value = "业务操作", status = MethodStatus.COMPLETE)
public void businessOperation() {
    var ctx = context();

    // 获取调用链ID
    String traceId = ctx.getTraceId();

    // 记录日志（调用链ID会自动记录）
    C.LOG.info("调用链ID: {}", traceId);

    // 调用其他服务时传递调用链
    var response = ClientStub.send(
        "/other-service/Operation/process",
        ctx,  // 会自动传递调用链
        C.JSON.toJson(data)
    );
}
```

---

## DataList - 分页响应

### 基本结构

```java
@Data
@Entity("数据列表")
public class DataList<T> {
    // 总数
    private long total;

    // 列表数据
    private List<T> list = new ArrayList<>();
}
```

### 基本用法

```java
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> findUsers(Search search) {
    var dataList = new DataList<User>();
    var db = DB.use();

    // 设置总数
    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());
    }

    // 设置列表
    dataList.setList(
        db.paging(search.getPageNo(), search.getPageSize())
            .query()
    );

    return dataList;
}
```

### 完整分页查询

```java
@Method(value = "查询", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> find(Search search) {
    var dataList = new DataList<User>();
    var db = getSearchDB(search);

    // pageNo=-1表示不分页，查询全部
    if (search.getPageNo() != -1) {
        // 分页查询：先count再分页
        dataList.setTotal(db.count());
        dataList.setList(
            db.paging(search.getPageNo(), search.getPageSize())
                .query()
        );
    } else {
        // 不分页：直接查询
        dataList.setList(db.query());
        dataList.setTotal(dataList.getList().size());
    }

    return dataList;
}

private DB<User> getSearchDB(Search search) {
    return DB.use()
        .eq(User::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
        .in(User::getStatus, search.getStatus(), C.OBJECT.isEmpty(search.getStatus()))
        .iLike(List.of(User::getName, User::getPhone),
               "%" + search.getKeyword() + "%",
               C.OBJECT.isEmpty(search.getKeyword()))
        .orderByDesc(User::getCreateTime);
}
```

### 空结果处理

```java
@Method(value = "查询用户", status = MethodStatus.COMPLETE)
public DataList<User> findUsers(Search search) {
    var dataList = new DataList<User>();
    var db = getSearchDB(search);

    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());

        if (dataList.getTotal() == 0) {
            // 没有数据时返回空列表
            dataList.setList(List.of());
            return dataList;
        }
    }

    dataList.setList(
        db.paging(search.getPageNo(), search.getPageSize())
            .query()
    );

    return dataList;
}
```

### DataList序列化

```java
// DataList会自动序列化为JSON
{
  "total": 100,
  "list": [
    {"id": "001", "name": "张三"},
    {"id": "002", "name": "李四"}
  ]
}
```

---

## Search - 查询条件

### 基本结构

```java
@Data
@Entity("查询条件")
public class Search {
    // 当前页码（-1表示不分页）
    @Field("页码")
    private int pageNo = 1;

    // 每页数量
    @Field("页大小")
    private int pageSize = 20;

    // 主键ID
    @Field("ID")
    private String id;

    // 关键词（模糊搜索）
    @Field("关键词")
    private String keyword;

    // 状态列表
    @Field("状态列表")
    private List<Integer> status;

    // 类型列表
    @Field("类型列表")
    private List<Integer> types;

    // 开始时间
    @Field("开始时间")
    private long startTime;

    // 结束时间
    @Field("结束时间")
    private long endTime;

    // 其他自定义字段...
}
```

### 使用示例

```java
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

private DB<User> getSearchDB(Search search) {
    return DB.use()
        // ID查询
        .eq(User::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))

        // 状态查询
        .in(User::getStatus, search.getStatus(), C.OBJECT.isEmpty(search.getStatus()))

        // 类型查询
        .in(User::getType, search.getTypes(), C.OBJECT.isEmpty(search.getTypes()))

        // 时间范围
        .between(User::getCreateTime, search.getStartTime(), search.getEndTime(),
                 C.OBJECT.isEmpty(search.getStartTime()) || C.OBJECT.isEmpty(search.getEndTime()))

        // 关键词模糊查询
        .iLike(List.of(User::getName, User::getPhone, User::getEmail),
               "%" + search.getKeyword() + "%",
               C.OBJECT.isEmpty(search.getKeyword()))

        // 排序
        .orderByDesc(User::getCreateTime);
}
```

### 自定义Search类

```java
// 用户查询条件
@Data
@Entity("用户查询条件")
public class UserSearch extends Search {
    @Field("部门ID")
    private String departmentId;

    @Field("角色ID")
    private Integer roleId;

    @Field("最小年龄")
    private Integer minAge;

    @Field("最大年龄")
    private Integer maxAge;
}

@Method(value = "查询用户", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> findUsers(UserSearch search) {
    var db = DB.use()
        .eq(User::getDepartmentId, search.getDepartmentId(),
              C.OBJECT.isEmpty(search.getDepartmentId()))
        .eq(User::getRoleId, search.getRoleId(),
              C.OBJECT.isEmpty(search.getRoleId()))
        .ge(User::getAge, search.getMinAge(),
              search.getMinAge() != null)
        .le(User::getAge, search.getMaxAge(),
              search.getMaxAge() != null)
        .orderByDesc(User::getCreateTime);

    // ... 分页处理
}
```

---

## 自定义DTO

### 请求DTO

```java
// 用户登录请求
@Data
@Entity("用户登录请求")
public class LoginRequest {
    @Field(value = "用户名", required = true)
    private String username;

    @Field(value = "密码", required = true)
    private String password;

    @Field(value = "验证码")
    private String captcha;

    @Field(value = "终端类型", defaultValue = "1")
    private Integer deviceType = 1;
}

@Method(value = "登录", status = MethodStatus.COMPLETE)
public LoginResult login(@Parameter(value = "登录信息", required = true) LoginRequest request) {
    // 验证验证码
    if (C.OBJECT.isEmpty(request.getCaptcha())) {
        throw new AppRuntimeException("验证码不能为空");
    }

    // 查询用户
    User user = DB.use().eq(User::getUsername, request.getUsername()).get();
    if (user == null) {
        throw new AppRuntimeException("用户名或密码错误");
    }

    // 验证密码
    if (!user.getPassword().equals(encrypt(request.getPassword()))) {
        throw new AppRuntimeException("用户名或密码错误");
    }

    // 返回登录结果
    return new LoginResult(user, generateToken(user));
}
```

### 响应DTO

```java
// 用户信息响应（视图类）
@Data
@Entity("用户信息")
public class UserView {
    @Field("ID")
    private String id;

    @Field("用户名")
    private String username;

    @Field("姓名")
    private String name;

    @Field("手机号")
    private String phone;

    @Field("部门")
    private String departmentName;

    @Field("角色")
    private String roleName;

    @Field("状态")
    private Integer status;

    @Field("状态名称")
    public String getStatusName() {
        return status == 1 ? "正常" : "禁用";
    }
}

@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public UserView getUser(String userId) {
    return DB.use().eq(User::getId, userId).get(UserView.class);
}
```

### 复杂DTO

```java
// 订单详情响应
@Data
@Entity("订单详情")
public class OrderDetail {
    @Field("订单ID")
    private String orderId;

    @Field("订单号")
    private String orderNo;

    @Field("订单金额")
    private BigDecimal amount;

    @Field("订单状态")
    private Integer status;

    @Field("用户信息")
    private UserInfo user;

    @Field("收货地址")
    private Address address;

    @Field("订单明细")
    private List<OrderItem> items;

    @Field("物流信息")
    private List<Logistics> logistics;
}

@Method(value = "获取订单详情", status = MethodStatus.COMPLETE)
public OrderDetail getOrderDetail(String orderId) {
    // 查询订单
    Order order = orderDB.use().eq(Order::getId, orderId).get();
    if (order == null) {
        throw new AppRuntimeException("订单不存在");
    }

    // 组装详情
    OrderDetail detail = new OrderDetail();
    detail.setOrderId(order.getId());
    detail.setOrderNo(order.getOrderNo());
    detail.setAmount(order.getAmount());
    detail.setStatus(order.getStatus());

    // 查询用户信息
    detail.setUser(UserCenter.getUser(order.getUserId()));

    // 查询收货地址
    detail.setAddress(AddressService.get(order.getAddressId()));

    // 查询订单明细
    detail.setItems(orderDetailDB.use()
        .eq(OrderDetail::getOrderId, orderId)
        .query());

    // 查询物流信息
    detail.setLogistics(LogisticsService.getList(orderId));

    return detail;
}
```

### 更新DTO

```java
// 用户更新请求
@Data
@Entity("用户更新")
public class UserUpdate {
    @Field("ID")
    private String id;

    @Field("姓名")
    private String name;

    @Field("手机号")
    private String phone;

    @Field("邮箱")
    private String email;

    @Field("部门ID")
    private String departmentId;
}

@Method(value = "更新用户", status = MethodStatus.COMPLETE)
public void update(@Parameter(value = "用户信息", required = true) UserUpdate update) {
    // 查询用户
    User user = DB.use().eq(User::getId, update.getId()).get();
    if (user == null) {
        throw new AppRuntimeException("用户不存在");
    }

    // 更新字段（只更新非空字段）
    if (C.OBJECT.isNotEmpty(update.getName())) {
        user.setName(update.getName());
    }
    if (C.OBJECT.isNotEmpty(update.getPhone())) {
        user.setPhone(update.getPhone());
    }
    if (C.OBJECT.isNotEmpty(update.getEmail())) {
        user.setEmail(update.getEmail());
    }
    if (C.OBJECT.isNotEmpty(update.getDepartmentId())) {
        user.setDepartmentId(update.getDepartmentId());
    }

    DB.use().update(user);
}
```

---

## DTO最佳实践

### 1. DTO与Entity分离

```java
// ✅ 推荐：Entity用于数据库，DTO用于接口

// Entity（数据库实体）
@Data
@Entity("用户")
public class User {
    @Field(value = "ID", primary = true)
    private String id;

    @Field(value = "用户名")
    private String username;

    @Field(value = "密码")
    private String password;

    @Field(value = "创建时间")
    private long createTime;
}

// DTO（接口响应）
@Data
@Entity("用户信息")
public class UserDTO {
    @Field("ID")
    private String id;

    @Field("用户名")
    private String username;

    // 不包含密码等敏感字段
}

@Method(value = "获取用户", status = MethodStatus.COMPLETE)
public UserDTO getUser(String userId) {
    return DB.use().eq(User::getId, userId).get(UserDTO.class);
}
```

### 2. 使用视图类

```java
// ✅ 推荐：使用视图类返回需要的数据

@Data
@Entity("用户视图")
public class UserView {
    @Field("ID")
    private String id;

    @Field("姓名")
    private String name;

    @Field("部门名称")
    private String departmentName;  // 关联字段

    @Field("角色名称")
    private String roleName;  // 关联字段
}

@Method(value = "查询用户", status = MethodStatus.COMPLETE)
@ReturnData(type = UserView.class)
public DataList<UserView> findUsers(Search search) {
    var dataList = new DataList<UserView>();
    var db = getSearchDB(search);

    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());
    }

    // 直接查询视图类
    dataList.setList(db.query(UserView.class));

    return dataList;
}
```

### 3. 参数对象校验

```java
@Method(value = "添加用户", status = MethodStatus.COMPLETE)
public void insert(@Parameter(value = "用户信息", required = true) User user) {
    // 在方法内校验
    if (C.OBJECT.isEmpty(user.getName())) {
        throw new AppRuntimeException("姓名不能为空");
    }

    if (C.OBJECT.isEmpty(user.getPhone())) {
        throw new AppRuntimeException("手机号不能为空");
    }

    // ...
}
```

### 4. 响应数据格式化

```java
@Data
@Entity("用户视图")
public class UserView {
    @Field("ID")
    private String id;

    @Field("创建时间")
    private long createTime;

    @Field("创建时间格式化")
    public String getCreateTimeStr() {
        return C.TIME.toDateTimeString(createTime);
    }

    @Field("状态")
    private Integer status;

    @Field("状态名称")
    public String getStatusName() {
        return status == 1 ? "正常" : "禁用";
    }

    @Field("性别")
    private Integer gender;

    @Field("性别名称")
    public String getGenderName() {
        if (gender == null) {
            return "未知";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }
}
```

### 5. 分页参数处理

```java
// ✅ 推荐：处理分页边界条件
@Method(value = "查询", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> find(Search search) {
    var dataList = new DataList<User>();
    var db = getSearchDB(search);

    // pageNo=-1表示不分页
    if (search.getPageNo() != -1) {
        // 确保页码合法
        int pageNo = Math.max(1, search.getPageNo());
        int pageSize = Math.min(100, Math.max(1, search.getPageSize()));

        dataList.setTotal(db.count());
        dataList.setList(
            db.paging(pageNo, pageSize).query()
        );
    } else {
        // 不分页
        dataList.setList(db.query());
        dataList.setTotal(dataList.getList().size());
    }

    return dataList;
}
```

---

## 完整示例

### 标准CRUD DTO

```java
@Service(value = "用户管理", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    // 查询条件
    @Data
    @Entity("用户查询条件")
    public static class UserSearch extends Search {
        @Field("部门ID")
        private String departmentId;

        @Field("角色ID")
        private Integer roleId;
    }

    // 用户视图
    @Data
    @Entity("用户视图")
    public static class UserView {
        @Field("ID")
        private String id;

        @Field("姓名")
        private String name;

        @Field("手机号")
        private String phone;

        @Field("部门名称")
        private String departmentName;

        @Field("角色名称")
        private String roleName;
    }

    // 查询
    @Method(value = "查询", status = MethodStatus.COMPLETE)
    @ReturnData(type = UserView.class)
    public DataList<UserView> find(UserSearch search) {
        var dataList = new DataList<UserView>();
        var db = getSearchDB(search);

        if (search.getPageNo() != -1) {
            dataList.setTotal(db.count());
        }

        dataList.setList(db.query(UserView.class));
        return dataList;
    }

    private DB<User> getSearchDB(UserSearch search) {
        return DB.use()
            .eq(User::getDepartmentId, search.getDepartmentId(),
                  C.OBJECT.isEmpty(search.getDepartmentId()))
            .eq(User::getRoleId, search.getRoleId(),
                  C.OBJECT.isEmpty(search.getRoleId()))
            .iLike(List.of(User::getName, User::getPhone),
                   "%" + search.getKeyword() + "%",
                   C.OBJECT.isEmpty(search.getKeyword()))
            .orderByDesc(User::getCreateTime);
    }
}
```
