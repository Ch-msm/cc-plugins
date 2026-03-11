# 运行时、路由与鉴权

## 启动链路

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

启动顺序固定为：

1. `Config.load(...)` 加载配置
2. `new Register(App.class)` 注册服务、初始化 `@EnvKey`
3. `new Application().start()` 启动 HTTP 服务

## Register 会做什么

- 扫描业务根包下的 `service` 包，注册所有 `@Service`
- 扫描业务根包下的 `@Component`
- 初始化 `com.szigc.common` 和业务根包里的 `@EnvKey` 静态字段
- 自动加载 `com.szigc.common.service` 下的内置服务
- 创建服务实例后调用 `init()`；`init()` 异常会记日志，但不会阻塞服务启动

## 路由生成规则

最终 URL 规则：

```text
/{当前服务名}/{服务路径}/{方法名}
```

其中：

- `当前服务名` 来自 `Env.Server.NAME`
- `服务路径` 由服务类所在包和类名推导
- `@Service.value` 只用于描述，不参与 URL 生成

示例：

```java
package com.szigc.demo.service;

@Service(value = "用户管理", author = "梅思铭", date = "2026-03-09")
public class UserService extends AbstractService {

    @Method(value = "登录")
    public void login(UserLoginParam param) {
    }
}
```

如果当前服务名是 `demo-center`，则 URL 为：

```text
/demo-center/User/login
```

如果服务类在子包：

```java
package com.szigc.demo.service.admin;

public class UserService extends AbstractService {
}
```

则服务路径为 `admin/User`，最终 URL：

```text
/demo-center/admin/User/login
```

## 请求与参数规则

- 框架只接受 `POST`
- 如果方法只有一个 `@Entity` 参数，框架会把整个 JSON 反序列化成该实体
- 如果是多个参数，或参数是 `String` / 基础类型 / 集合，则每个参数都必须加 `@Parameter`
- 参数数量超过 4 个时，应改为一个实体参数

正确示例：

```java
@Method(value = "删除")
public void delete(@Parameter(value = "ID", required = true) String id) {
}

@Method(value = "分页查询")
public DataList<User> find(UserSearch search) {
}
```

错误示例：

```java
@Method(value = "删除")
public void delete(String id) {
}
```

## 返回值规则

- 返回实体时可以不写 `@ReturnData`
- 返回 `String`、基础类型、`BigDecimal`、集合时必须写 `@ReturnData`
- 返回 `DataList<T>` 时建议写 `@ReturnData(type = T.class)`，否则框架无法正确表达列表项类型

示例：

```java
@Method(value = "当前用户ID")
@ReturnData("用户ID")
public int currentUserId() {
    return context().getUserId();
}

@Method(value = "分页查询")
@ReturnData(type = User.class)
public DataList<User> find(UserSearch search) {
    return new DataList<>();
}
```

## 鉴权语义

`MethodNature` 的真实语义如下：

- `PUBLIC`：公开接口，外部请求不要求登录
- `PROTECTED`：外部请求要求登录，但不校验接口授权
- `CONTROLLED`：外部请求要求登录，并校验接口授权

注意：

- `CONTROLLED` 是默认值
- 内部调用会携带 `x-security-key` 和 `x-trace-id`，可以跳过外部鉴权流程
- 如果调用了 `Application.addFilter(...)`，则默认鉴权逻辑会被自定义过滤器接管

## 日志行为

- `printLog = false`：不打印接口入参日志
- `sensitiveData = true`：日志中直接清空整段入参

这不是“字段级脱敏”。下面才是正确写法：

```java
@Method(
    value = "登录",
    nature = MethodNature.PUBLIC,
    status = MethodStatus.COMPLETE,
    printLog = false,
    sensitiveData = true
)
public LoginResult login(UserLoginParam param) {
    return new LoginResult();
}
```

## 上下文与线程

`AbstractService.context()` 底层来自 `ThreadLocal<RequestBody>`。

可以安全使用的场景：

- 当前请求线程内读取 `context().getUserId()`
- 使用 `context()` 继续向其他微服务透传头信息

不要这样做：

```java
C.ASYNC.run(() -> {
    var ctx = context();
});
```

异步逻辑如果需要上下文，请先把需要的字段提取出来再传入线程任务。

## 动态接口

`Application.addDynamicInterface(...)` 用于处理未注册到 `@Service` / `@Method` 的路径。

适合场景：

- 自定义健康检查
- 特殊代理接口
- 非标准路由兼容

它不是普通业务接口的首选方案。默认还是优先使用 `@Service` + `@Method`。
