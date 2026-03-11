# 异常处理与响应

## 响应语义

框架默认返回统一结构：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

`code` 语义：

- `0`：成功
- `1`：业务异常，通常是 `AppRuntimeException`
- `2`：服务内部异常

## `AppRuntimeException`

业务校验、参数检查、外部依赖失败时，优先抛 `AppRuntimeException`：

```java
if (C.OBJECT.isEmpty(id)) {
    throw new AppRuntimeException("ID必填");
}
```

删除前校验：

```java
@Method(value = "删除", status = MethodStatus.COMPLETE)
public void delete(@Parameter(value = "ID", required = true) String id) {
    if (C.OBJECT.isEmpty(id)) {
        throw new AppRuntimeException("ID必填");
    }
    DB.use().eq(User::getId, id).delete();
}
```

## 参数错误

建议把参数错误尽早拦住：

```java
@Method(value = "详情", status = MethodStatus.COMPLETE)
public User get(@Parameter(value = "ID", required = true) String id) {
    if (C.OBJECT.isEmpty(id)) {
        throw new AppRuntimeException("ID必填");
    }
    return DB.use().eq(User::getId, id).get();
}
```

## 外部服务调用

调用外部 HTTP 时，使用真实方法名：

```java
@Method(value = "发送短信", status = MethodStatus.COMPLETE)
public void sendSms(SendSmsParam param) {
    try {
        String response = C.HTTP.postOfString(
            "https://sms.example.com/send",
            Map.of("Content-Type", "application/json"),
            C.JSON.toJson(param)
        );
        if (C.OBJECT.isEmpty(response)) {
            throw new AppRuntimeException("短信服务无响应");
        }
    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        throw new AppRuntimeException("短信服务调用失败");
    }
}
```

## 服务间调用

`ClientStub.send(...)` 出错时也会抛 `AppRuntimeException`：

```java
public LoginUser loadLoginUser() {
    var json = ClientStub.send(
        "/user-center/Authenticate/getLoginUser",
        context(),
        C.JSON.toJson(Map.of("token", context().getToken()))
    );
    return C.JSON.fromJson(json, LoginUser.class);
}
```

## 文件处理

```java
@Method(value = "导入", status = MethodStatus.COMPLETE)
public void importData(@Parameter(value = "文件ID", required = true) String fileId) {
    try {
        var file = C.FILE.download(fileId);
        var rows = com.szigc.common.util.File.EXECL.reader(file, UserImportRow.class);
        for (UserImportRow row : rows) {
            var param = C.OBJECT.convert(SaveUserParam.class, row);
            insert(param);
        }
    } catch (AppRuntimeException e) {
        throw e;
    } catch (Exception e) {
        throw new AppRuntimeException("导入失败");
    }
}
```

## 事务处理

涉及多步写操作时优先使用事务模板：

```java
DB.use().transaction(db -> {
    db.insert(entity);
    otherDb.use().noTransaction().insert(log);
});
```

说明：

- 事务模板内部会自动提交和回滚
- 如果你抛出 `AppRuntimeException`，会按业务异常返回

## 推荐做法

1. 面向用户的错误统一抛 `AppRuntimeException`
2. 不要把底层异常原样暴露给前端
3. 外部调用失败时转成明确的业务语义
4. 事务内不要吞异常
