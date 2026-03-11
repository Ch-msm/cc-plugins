---
name: cloudless-service
description: Cloudless common 框架下的 Java 微服务开发与排障指南。用于创建、修改和修正 Service、表实体、Search/DTO/View、CRUD、分页、导入导出、缓存消息、订阅消息、内部服务调用、启动配置与数据库操作；当用户提到 cloudless/common、@Service、@Method、MainDB、C 工具集、路由、鉴权、参数声明、上下文等问题时使用。
---

# Cloudless Service 开发指南

## 适用场景

- 创建新的 Cloudless 服务类
- 实现 CRUD、分页、导入导出、内部服务调用
- 修正 `@Method`、`@Parameter`、`@ReturnData` 使用错误
- 排查路由、鉴权、上下文、配置加载和服务启动问题

## 推荐阅读顺序

1. [项目结构](references/project-structure.md)
2. [运行时、路由与鉴权](references/runtime-routing-auth.md)
3. [注解与接口声明](references/method-annotation.md)
4. [实体定义](references/entity-definition.md)
5. [数据传输对象](references/data-transfer-objects.md)
6. [数据库操作](references/database-operations.md)
7. [常用工具](references/common-tools.md)
8. [配置系统](references/configuration.md)
9. [文件与时间](references/file-time-operations.md)
10. [缓存消息](references/cache-mq-operations.md)
11. [消息订阅](references/subscribe-message.md)
12. [内部服务](references/interior-services.md)
13. [异常处理](references/exception-response.md)
14. [枚举与常量](references/enums-constants.md)

## 核心规则

1. 服务类必须放在业务根包的 `service` 包下，类名必须以 `Service` 结尾，并继承 `AbstractService`。
2. `service`、`entity.param`、`entity.table`、`entity.view` 都允许按业务域继续拆多层包；只有 `service` 子包层级会参与路由生成。
3. URL 不是由 `@Service.value` 决定，而是由服务所在包、类名和方法名生成。
4. `@Method.nature` 默认值是 `MethodNature.CONTROLLED`，不是 `PUBLIC`。
5. 单个实体参数可以直接声明；多个参数、基础类型参数、字符串参数、集合参数必须逐个添加 `@Parameter`，且接口参数总数不能超过 4 个。
6. 返回值如果是 `String`、基础类型、`BigDecimal`、集合，必须添加 `@ReturnData`；返回 `DataList<T>` 时也应写 `@ReturnData(type = T.class)`。
7. `sensitiveData` 是布尔值。设置为 `true` 时，框架会隐藏整段入参日志，不支持“按字段脱敏”。
8. 表实体类名优先直接使用业务名本身，例如 `User`、`Order`，不要机械追加 `Entity` 后缀。
9. 接口参数和返回值优先复用 `entity.table` 下的表实体；只有结构不匹配时，才定义 `entity.param` 或 `entity.view`。
10. 当参数实体或视图实体只是比表实体多几个字段时，优先继承对应表实体再补充字段，避免重复定义相同字段。
11. `Search`、`BaseSearch`、各种请求 DTO / 响应 DTO 都是业务项目自己定义的类，不是 `common` 内置类型。
12. `context()` 只能在当前请求线程中使用，不要在线程池任务或异步回调里直接读取。
13. `Config.load(...)` 需要在启动类中显式调用。
14. 作者信息统一写 `梅思铭`，注释和接口说明统一使用中文。

## 最小示例

```java
@Service(value = "用户管理", author = "梅思铭", date = "2026-03-09", module = "基础资料")
public class UserService extends AbstractService {

    private static final MainDB<User> DB = new MainDB<>(User.class);

    @Override
    public void init() {
        DB.use().createTable();
    }

    @Method(value = "分页查询", status = MethodStatus.COMPLETE)
    @ReturnData(type = User.class)
    public DataList<User> find(UserSearch search) {
        var db = DB.use()
            .eq(User::getDepartmentId, search.getDepartmentId(),
                C.OBJECT.isEmpty(search.getDepartmentId()))
            .iLike(List.of(User::getName), "%" + search.getKeyword() + "%",
                C.OBJECT.isEmpty(search.getKeyword()));

        var result = new DataList<User>();
        if (search.getPageNo() != -1) {
            result.setTotal(db.count());
        }
        result.setList(db.paging(search.getPageNo(), search.getPageSize()).query());
        return result;
    }

    @Method(value = "删除", status = MethodStatus.COMPLETE)
    public void delete(@Parameter(value = "ID", required = true) String id) {
        DB.use().eq(User::getId, id).delete();
    }
}
```

完整模板见 [service-template.java](assets/service-template.java)。

常用配套模板：

- [entity-template.java](assets/entity-template.java)
- [base-search-template.java](assets/base-search-template.java)
- [search-template.java](assets/search-template.java)
- [request-dto-template.java](assets/request-dto-template.java)
- [view-template.java](assets/view-template.java)
- [common.yml](assets/common.yml)
- [your-service.yml](assets/your-service.yml)
