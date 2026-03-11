# 服务注解与接口声明

## `@Service`

类级别注解，标记一个服务。

```java
@Service(
    value = "用户管理",
    author = "梅思铭",
    date = "2026-03-09",
    module = "基础资料"
)
public class UserService extends AbstractService {
}
```

字段说明：

- `value`：服务中文描述
- `author`：作者
- `date`：日期
- `module`：自由字符串，用于归类展示

注意：

- 路由不使用 `value`
- `module` 不是 `Module.USER` 这类枚举写法

## `@Method`

方法级别注解，声明一个接口。

```java
@Method(
    value = "分页查询",
    status = MethodStatus.COMPLETE,
    nature = MethodNature.CONTROLLED,
    explain = "支持关键字查询"
)
public DataList<User> find(UserSearch search) {
    return new DataList<>();
}
```

字段说明：

- `value`：接口描述
- `printLog`：是否打印入参日志，默认 `true`
- `nature`：访问级别，默认 `MethodNature.CONTROLLED`
- `author`：方法作者，留空时继承 `@Service.author`
- `date`：方法日期，留空时继承 `@Service.date`
- `status`：开发状态，默认 `MethodStatus.DEVELOPING`
- `explain`：补充说明
- `sensitiveData`：是否隐藏整段入参日志，类型是 `boolean`

## 访问级别

```java
@Method(value = "登录", nature = MethodNature.PUBLIC)
public LoginResult login(UserLoginParam param) {
    return new LoginResult();
}

@Method(value = "当前用户", nature = MethodNature.PROTECTED)
public UserInfo currentUser() {
    return new UserInfo();
}

@Method(value = "删除用户", nature = MethodNature.CONTROLLED)
public void delete(@Parameter(value = "ID", required = true) String id) {
}
```

真实语义：

- `PUBLIC`：外部请求无需登录
- `PROTECTED`：外部请求需要登录，但不校验接口授权
- `CONTROLLED`：外部请求需要登录，并校验接口授权

默认值是 `CONTROLLED`。

## `@Parameter`

多参数接口、字符串参数、基础类型参数、集合参数必须写 `@Parameter`。

```java
@Method(value = "删除", status = MethodStatus.COMPLETE)
public void delete(@Parameter(value = "ID", required = true) String id) {
}

@Method(value = "批量删除", status = MethodStatus.COMPLETE)
public void batchDelete(
    @Parameter(value = "ID列表", required = true) List<String> ids
) {
}
```

字段说明：

- `value`：参数中文名
- `sample`：示例值
- `required`：是否必填
- `defaultValue`：默认值
- `check`：校验规则

## 单实体参数

如果接口只有一个实体参数，可以不写 `@Parameter`：

```java
@Method(value = "新增", status = MethodStatus.COMPLETE)
public void insert(User entity) {
}
```

但如果有多个参数，就必须逐个写 `@Parameter`。

## `@ReturnData`

非实体返回值必须写 `@ReturnData`：

```java
@Method(value = "当前用户ID", status = MethodStatus.COMPLETE)
@ReturnData("用户ID")
public int currentUserId() {
    return context().getUserId();
}
```

`DataList<T>` 建议这样写：

```java
@Method(value = "分页查询", status = MethodStatus.COMPLETE)
@ReturnData(type = User.class)
public DataList<User> find(UserSearch search) {
    return new DataList<>();
}
```

## 正确示例

```java
@Service(value = "用户管理", author = "梅思铭", date = "2026-03-09", module = "基础资料")
public class UserService extends AbstractService {

    private static final MainDB<User> DB = new MainDB<>(User.class);

    @Method(value = "详情", status = MethodStatus.COMPLETE)
    public User get(@Parameter(value = "ID", required = true) String id) {
        return DB.use().eq(User::getId, id).get();
    }

    @Method(value = "新增", status = MethodStatus.COMPLETE)
    public void insert(User entity) {
        if (C.OBJECT.isEmpty(entity.getId())) {
            entity.setId(C.TEXT.longId());
        }
        DB.use().insert(entity);
    }

    @Method(
        value = "登录",
        nature = MethodNature.PUBLIC,
        status = MethodStatus.COMPLETE,
        printLog = false,
        sensitiveData = true
    )
    @ReturnData(type = LoginResult.class)
    public LoginResult login(UserLoginParam param) {
        return new LoginResult();
    }
}
```

## 常见错误

错误 1：

```java
public void delete(String id) {
}
```

问题：缺少 `@Parameter`。

错误 2：

把 `sensitiveData` 当成“字段数组”来写。

问题：`sensitiveData` 不是数组，而是 `boolean`。

错误 3：

```java
@Method(value = "公开接口")
public void ping() {
}
```

问题：如果你没有显式声明 `nature = MethodNature.PUBLIC`，它默认是 `CONTROLLED`。
