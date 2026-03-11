# Cloudless 项目结构

## 推荐目录

```text
your-service/
├── build.gradle
├── settings.gradle
└── src/
    └── main/
        ├── java/com/szigc/xxx/
        │   ├── App.java
        │   ├── component/
        │   ├── constant/
        │   ├── dependency/
        │   ├── entity/
        │   │   ├── param/
        │   │   │   ├── user/
        │   │   │   └── order/
        │   │   ├── table/
        │   │   │   ├── user/
        │   │   │   └── order/
        │   │   └── view/
        │   │       ├── user/
        │   │       └── order/
        │   └── service/
        │       ├── user/
        │       └── order/
        └── resources/
            ├── common.yml
            └── your-service.yml
```

可直接参考 [common.yml](../assets/common.yml) 和 [your-service.yml](../assets/your-service.yml)。

## 各目录职责

### `App.java`

启动入口，必须显式加载配置、注册服务并启动应用。

```java
package com.szigc.xxx;

import com.szigc.common.Register;
import com.szigc.common.server.Application;
import com.szigc.common.util.Config;

public class App {
    public static void main(String[] args) {
        Config.load("common", "your-service");
        new Register(App.class);
        new Application().start();
    }
}
```

### `service/`

只放服务类。

要求：

- 类名必须以 `Service` 结尾
- 类必须继承 `AbstractService`
- 类必须使用 `@Service`
- 只有这里的服务类会被自动扫描注册
- 允许继续按业务域拆多层包，例如 `service.user`、`service.user.admin`

说明：

- `service` 下的多层子包会参与路由生成
- 例如 `com.szigc.xxx.service.user.admin.UserService` 会生成 `user/admin/User`

### `component/`

放启动时自动实例化的组件类，使用 `@Component`。

```java
@Component(async = true)
public class CacheWarmUpComponent {
    public CacheWarmUpComponent() {
        // 启动时执行
    }
}
```

### `entity/table/`

放数据库实体，使用 `@Entity` 和 `@Field`。

说明：

- 允许按业务域继续拆多层包，例如 `entity.table.user.User`
- `table` 下的包层级只影响代码组织，不参与路由生成
- 即使放在多层包里，表实体本身仍然要正常写 `@Entity`，字段仍然要写 `@Field`
- 默认表名也不会自动带上这些包层级；如果你希望按业务域区分表名，请使用 `@Entity.tableNamePrefix` 或 `@Entity.tableName`

```java
@Data
@Entity(value = "用户", logicDelete = true)
public class User {
    @Field(value = "ID", primaryKey = true)
    private String id;

    @Field("姓名")
    private String name;
}
```

可直接参考 [entity-template.java](../assets/entity-template.java)。

### `entity/param/`

放业务请求参数、搜索条件、导入导出参数。

说明：

- 允许按业务域继续拆多层包，例如 `entity.param.user.UserSearch`
- `param` 下的包层级只影响代码组织，只要业务代码能正常 import 即可
- 接口入参优先复用 `entity.table/` 下的表实体；只有结构不匹配时才新增参数实体
- 如果只是比表实体多几个字段，优先继承表实体再扩展，避免重复定义相同字段

`Search` 不是 `common` 自带类，建议自己定义基础分页类：

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
}
```

可直接参考 [base-search-template.java](../assets/base-search-template.java)、[search-template.java](../assets/search-template.java) 和 [request-dto-template.java](../assets/request-dto-template.java)。

### `entity/view/`

放在表实体无法直接满足返回结构时使用的返回视图对象，适合补充聚合字段、格式化字段和关联字段。

说明：

- 接口返回值优先复用 `entity.table/` 下的表实体；只有结构不匹配时才新增 `view`
- 如果只是比表实体多几个字段，优先让 `view` 继承表实体再扩展
- 允许按业务域继续拆多层包，例如 `entity.view.user.UserDetailView`
- `view` 下的包层级只影响代码组织，不需要框架额外注册

可直接参考 [view-template.java](../assets/view-template.java)。

### `dependency/`

放服务间调用的依赖类，统一封装 `ClientStub`。

```java
public class UserCenterDependency {
    private UserCenterDependency() {
    }

    public static LoginUser getLoginUser(RequestBody requestBody) {
        var json = ClientStub.send(
            "/user-center/Authenticate/getLoginUser",
            requestBody,
            C.JSON.toJson(Map.of("token", requestBody.getToken()))
        );
        return C.JSON.fromJson(json, LoginUser.class);
    }
}
```

## 命名建议

- 表实体：`User`、`Order`
- 搜索条件：`UserSearch`、`OrderSearch`
- 请求参数：`CreateUserParam`、`UpdateUserParam`
- 视图对象：`UserView`、`OrderDetailView`
- 服务类：`UserService`、`OrderService`

## 推荐做法

1. 通用分页字段放在 `BaseSearch`
2. 一个业务领域一个 `Service`
3. 跨服务调用统一走 `dependency/`，不要在业务代码中散落 `ClientStub.send(...)`
4. 接口参数和返回值优先复用表实体，只有结构不匹配时才新增参数实体或视图实体
5. 项目复杂时，优先在 `service`、`entity.param`、`entity.table`、`entity.view` 下按业务域拆多层包
