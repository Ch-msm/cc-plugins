# 数据传输对象

## `RequestBody`

`RequestBody` 是框架运行时上下文，不是普通业务 DTO。

常用字段：

- `url`
- `token`
- `apiToken`
- `dataPermissionId`
- `deviceType`
- `body`
- `realIp`
- `traceId`
- `userId`
- `username`
- `user`
- `dataScope`
- `method`

在服务中优先通过 `context()` 获取：

```java
@Method(value = "当前用户", status = MethodStatus.COMPLETE)
public Map<String, Object> currentUserInfo() {
    var ctx = context();
    return Map.of(
        "userId", ctx.getUserId(),
        "username", ctx.getUsername(),
        "user", ctx.getUser(),
        "ip", ctx.getRealIp(),
        "traceId", ctx.getTraceId(),
        "dataScope", ctx.getDataScope()
    );
}
```

注意：

- IP 使用 `getRealIp()`
- 角色信息通常需要通过 `LoginUser` 或用户中心获取
- `context()` 只能在当前请求线程使用

## `DataList<T>`

标准分页返回对象：

```java
var result = new DataList<User>();
result.setTotal(total);
result.setList(list);
return result;
```

接口上建议配合：

```java
@ReturnData(type = User.class)
public DataList<User> find(UserSearch search) {
    return new DataList<>();
}
```

## `Search` 不是框架内置类型

`common` 没有通用 `Search` 类。搜索条件应由业务项目自己定义。

推荐写法：

```java
@Data
@Entity("基础查询条件")
public class BaseSearch {
    @Field(value = "页码", defaultValue = "1")
    private int pageNo = 1;

    @Field(value = "每页条数", defaultValue = "20")
    private int pageSize = 20;
}

@Data
@Entity("用户查询条件")
public class UserSearch extends BaseSearch {
    @Field("关键字")
    private String keyword;

    @Field("部门ID")
    private String departmentId;

    @Field("ID")
    private String id;
}
```

## `ExportItem`

Excel 导出时使用：

```java
List<ExportItem> exportItems = List.of(
    new ExportItem("name", "姓名", false, false),
    new ExportItem("phone", "手机号", false, false)
);
```

字段说明：

- `key`：数据字段名
- `name`：导出列名
- `picture`：是否图片列
- `attachment`：是否附件列

## `LoginUser`

登录用户结构通常来自用户中心或缓存：

```java
var loginUser = UserCenter.getLoginUser(context().getToken(), context().getApiToken());
int userId = loginUser.getId();
Set<Integer> roleIds = loginUser.getRoleId();
Set<Integer> dataPermissionIds = loginUser.getDataPermissionId();
```

## DTO 建模建议

1. 接口参数和返回值优先复用表实体，只有结构不匹配时才新增参数实体或 `view`
2. 参数实体、`view` 实体如果只是补充少量字段，优先继承表实体再扩展，避免重复建相同字段
3. 搜索条件统一继承业务自己的 `BaseSearch`
4. 需要多参数时，优先收敛成一个实体 DTO
5. 返回值需要聚合字段、格式化字段或关联字段时，再使用 `view` 类

配套模板：

- [base-search-template.java](../assets/base-search-template.java)
- [search-template.java](../assets/search-template.java)
- [request-dto-template.java](../assets/request-dto-template.java)
- [view-template.java](../assets/view-template.java)
