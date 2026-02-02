---
name: cloudless-service
description: Cloudless微服务框架服务开发指南。使用此技能创建和实现服务接口，包括使用@Service/@Method注解创建服务、调用项目工具类（C.CONFIG、C.JSON、C.TIME、C.FILE、C.CACHE、C.MQ等）、MainDB数据库操作、时序数据库操作、文件上传下载等。当用户请求创建新服务、实现HTTP接口、CRUD操作、或需要使用cloudless-common工具类时触发。
user_invocable: true
---

# Cloudless Service 开发指南

## 快速开始

服务开发核心步骤：
1. 创建服务类，继承`AbstractService`，使用`@Service`注解
2. 声明`MainDB`实例进行数据库操作
3. 重写`init()`方法初始化数据库表
4. 使用`@Method`注解注册方法
5. 通过`C`类访问工具类

## 基础服务结构

```java
@Service(value = "服务名称", author = "梅思铭", date = "2025-01-08")
public class YourService extends AbstractService {

    private static final MainDB<YourEntity> DB = new MainDB<>(YourEntity.class);

    @Override
    public void init() {
        DB.use().createTable();
    }

    @Method(value = "方法说明", status = MethodStatus.COMPLETE)
    public void yourMethod() {
        // 使用C类工具
        C.CONFIG.get("key");
        C.JSON.toJson(obj);
        C.FILE.upload(file);
        C.CACHE.set("key", value);
        // MainDB操作
        DB.use().insert(entity);
    }
}
```

完整服务模板见 `assets/service-template.java`

## 常用代码片段

### CRUD基础

```java
// 新增
@Method(value = "新增", status = MethodStatus.COMPLETE)
public void insert(YourEntity entity) {
    if (C.OBJECT.isEmpty(entity.getId())) {
        entity.setId(C.TEXT.longId());
    }
    DB.use().insert(entity);
}

// 更新
@Method(value = "更新", status = MethodStatus.COMPLETE)
public void update(YourEntity entity) {
    DB.use().update(entity);
}

// 删除
@Method(value = "删除", status = MethodStatus.COMPLETE)
public void delete(String id) {
    DB.use().eq(YourEntity::getId, id).delete();
}

// 查询
@Method(value = "查询", status = MethodStatus.COMPLETE)
@ReturnData(type = YourEntity.class)
public DataList<YourEntity> find(Search search) {
    var db = DB.use()
        .eq(YourEntity::getField, search.getValue(), C.OBJECT.isEmpty(search.getValue()))
        .iLike(List.of(YourEntity::getName), "%" + search.getKeyword() + "%",
               C.OBJECT.isEmpty(search.getKeyword()));
    var dataList = new DataList<YourEntity>();
    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());
    }
    dataList.setList(db.paging(search.getPageNo(), search.getPageSize()).query());
    return dataList;
}
```

### 服务间调用

```java
// 调用其他服务
var json = ClientStub.send(
    "/service-name/ServiceClass/methodName",
    new RequestBody(),
    C.JSON.toJson(params)
);
Result result = C.JSON.fromJson(json, Result.class);
```

## 参考文档

详细文档按需查阅：

| 文档 | 内容 |
|------|------|
| [项目结构](references/project-structure.md) | 目录组织、包命名规范、类命名规则 |
| [实体定义](references/entity-definition.md) | @Entity/@Field注解、表实体、视图实体、参数实体 |
| [注解详解](references/method-annotation.md) | @Method/@Parameter/@ReturnData注解完整说明 |
| [数据库操作](references/database-operations.md) | MainDB CRUD、事务管理、时序数据库、表管理 |
| [缓存消息](references/cache-mq-operations.md) | C.CACHE缓存、C.MQ消息队列、C.PUSH推送 |
| [消息订阅](references/subscribe-message.md) | @Subscribe订阅、数据同步、消息处理最佳实践 |
| [文件时间](references/file-time-operations.md) | C.FILE上传下载、Excel导入导出、C.TIME时间处理 |
| [常用工具](references/common-tools.md) | C.CONFIG/JSON/LOG/HTTP/ASYNC/MAIL/TEXT/OBJECT/DATA/TREE/MATH/GEO |
| [内部服务](references/interior-services.md) | PersonService、SerialNumberService、AttachmentService 等 |
| [配置系统](references/configuration.md) | C.CONFIG使用、YAML配置、环境变量、@EnvKey注解 |
| [枚举常量](references/enums-constants.md) | MethodStatus、MethodNature、ServerName、自定义常量 |
| [异常处理](references/exception-response.md) | AppRuntimeException、异常处理模式、错误码定义 |
| [数据传输](references/data-transfer-objects.md) | RequestBody、DataList、Search、自定义DTO |

## 开发规范

1. **包结构**: 所有服务类必须放在`service`包下
2. **作者信息**: 服务作者统一填写自己的名字
3. **方法注释**: 使用中文描述
4. **日志输出**: 使用C.LOG输出，不在方法内使用System.out
5. **异常处理**: 使用`AppRuntimeException`抛出业务异常
6. **ID生成**: 使用`C.TEXT.shortId()`或`C.TEXT.longId()`
7. **判空操作**: 使用`C.OBJECT.isEmpty()`和`C.OBJECT.isNotEmpty()`
8. **条件跳过**: MainDB条件第三个参数控制是否生效
9. **时间验证**: 涉及时间范围查询时使用`C.TIME.validYearTimestamp()`
10. **敏感数据**: 登录、密码等接口使用`sensitiveData = true`

## URL路径规则

服务方法访问路径：
```
/{服务名}/{Service类名去掉Service}/{方法名}
```

示例：
- 服务：`@Service(value = "用户服务")` 的 `UserService.getUser()`
- 路径：`/user-center/User/getUser`
