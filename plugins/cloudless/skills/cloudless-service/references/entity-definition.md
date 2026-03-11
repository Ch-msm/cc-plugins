# 实体定义

## `@Entity`

`@Entity` 用于标记实体类。它既可以表示数据库表实体，也可以表示接口参数或返回对象。

```java
@Entity(
    value = "用户",
    logicDelete = true,
    tableNamePrefix = "",
    tableName = ""
)
public class User {
}
```

字段说明：

- `value`：实体说明
- `logicDelete`：是否逻辑删除，默认 `true`
- `tableNamePrefix`：表名前缀
- `tableName`：自定义表名

说明：

- 做数据库持久化的类建议加 `@Entity`
- 作为接口单实体参数的 DTO 也建议加 `@Entity`
- 包名是否多层级不会改变这个要求
- 推荐表实体类名直接使用业务名本身，例如 `User`、`Order`
- 默认表名只看类名，不看包层级；`entity.table.user.User` 和 `entity.table.admin.User` 都会先按 `User -> user` 处理
- 如果复杂项目里不同业务域可能出现同名实体，应显式使用 `tableNamePrefix` 或 `tableName`

表名策略：

1. 默认情况下：表名由类名转下划线生成，例如 `User -> user`
2. 如果设置了 `tableNamePrefix = "user"`：表名会变成 `user_user`
3. 如果设置了 `tableName = "biz_user"`：直接使用这个表名
4. 如果当前是单库模式，框架还会在前面再加一层服务名前缀

建议：

- 同一个服务里只是包层级变深，不要因为“目录分组”就滥用 `tableNamePrefix`
- 表实体类名优先保持简洁，例如 `User`、`Order`，通常这样默认表名就已经是你想要的 `user`、`order`
- 只有在同一服务内需要区分同名实体、或你希望按业务域给数据库表分组时，再使用 `tableNamePrefix`
- 需要完全可控的命名时，直接使用 `tableName`

## `@Field`

```java
@Field(
    value = "字段说明",
    length = 255,
    sample = "示例值",
    required = false,
    defaultValue = "",
    check = "",
    primaryKey = false,
    autoIncrement = false,
    ignore = false,
    fieldIgnore = false
)
private String name;
```

真实属性：

- `value`
- `length`
- `sample`
- `required`
- `defaultValue`
- `check`
- `primaryKey`
- `autoIncrement`
- `ignore`
- `fieldIgnore`

注意：

- 没有 `primary`
- `ignore = true` 表示数据库层忽略该字段
- `fieldIgnore = true` 主要用于导入导出场景忽略该字段
- 包名是否多层级不会改变这个要求，表实体字段照样要写 `@Field`

## 表实体

```java
@Data
@Entity(value = "用户", logicDelete = true)
public class User {
    @Field(value = "ID", primaryKey = true)
    private String id;

    @Field(value = "姓名", required = true, length = 100)
    private String name;

    @Field(value = "手机号", length = 20)
    private String phone;

    @Field(value = "状态", defaultValue = "1", check = "0|1")
    private int status = 1;

    @Field("创建时间")
    private long createTime;

    @Field("更新时间")
    private long updateTime;
}
```

## 自增主键

```java
@Data
@Entity(value = "审批过程", logicDelete = false)
public class ApprovalProcess {
    @Field(value = "ID", primaryKey = true, autoIncrement = true)
    private int id;
}
```

## 忽略字段

适合把数据库中存储的 JSON 字段映射成运行时对象：

```java
@Data
@Entity(value = "审批过程", logicDelete = false)
public class ApprovalProcess {
    @Field(value = "审批人信息", length = -1)
    private String approveJson;

    @Field(value = "审批人", ignore = true)
    private UserBase approve;
}
```

## 参数实体

单实体参数、搜索参数、导入参数都建议加 `@Entity` 和 `@Field`：

接口入参优先复用表实体。只有接口结构与表实体不匹配，或者需要补充额外字段时，才定义参数实体；如果只是多几个字段，优先继承表实体再扩展。

```java
@Data
@Entity("用户查询条件")
public class UserSearch {
    @Field(value = "页码", defaultValue = "1")
    private int pageNo = 1;

    @Field(value = "每页条数", defaultValue = "20")
    private int pageSize = 20;

    @Field("关键字")
    private String keyword;

    @Field("部门ID")
    private String departmentId;
}
```

## 视图实体

接口返回值优先直接使用表实体。只有返回结构与表实体不匹配，或者需要补充聚合字段、格式化字段、关联字段时，才定义 `view`；如果只是多几个字段，优先继承表实体再扩展。

聚合返回值也可以加 `@Field`，便于接口元数据表达：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Entity("用户视图")
public class UserView extends User {

    @Field("附件列表")
    private List<Attachment> attachmentList;
}
```

## 导出实体

Excel 导出项使用 `ExportItem`：

```java
List<ExportItem> items = List.of(
    new ExportItem("name", "姓名", false, false),
    new ExportItem("attachmentList", "附件", false, true)
);
```

## 推荐做法

1. 接口参数和返回值优先复用表实体；只有结构不匹配时才新增参数实体或视图实体
2. 参数实体、视图实体如果只是扩展少量字段，优先继承表实体，避免重复定义相同字段
3. 主键使用 `primaryKey = true`
4. 需要在数据库中忽略的运行时字段使用 `ignore = true`
5. 需要默认值和枚举校验时优先写在 `@Field`

配套模板：

- [entity-template.java](../assets/entity-template.java)
- [request-dto-template.java](../assets/request-dto-template.java)
- [view-template.java](../assets/view-template.java)
