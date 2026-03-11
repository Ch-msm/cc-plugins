# 配置系统

## 加载方式

配置不会自动加载，必须在启动类中显式调用：

```java
Config.load("common", "your-service");
```

配套模板：

- [common.yml](../assets/common.yml)
- [your-service.yml](../assets/your-service.yml)

常见写法：

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

## 读取 API

`C.CONFIG` 的真实常用方法如下：

```java
String fileServerHost = C.CONFIG.get("file_server.host");
String fileServerHostWithDefault = C.CONFIG.get("file_server.host", "127.0.0.1:9007");

int port = C.CONFIG.getInt("server.port");
boolean debug = C.CONFIG.getBoolean("app.debug");
double taxRate = C.CONFIG.getDouble("app.tax_rate");

List<String> mqHosts = C.CONFIG.getArray("message_queue.host");
List<Long> ids = C.CONFIG.getLongArray("app.ids");
List<Integer> ports = C.CONFIG.getIntArray("app.port_list");
```

注意：

- `getInt/getLong/getBoolean/getDouble` 没有“带默认值”的重载
- 如果需要默认值，请先用 `get(key)` 判断

业务里可以这样封装默认值：

```java
public static int getPortOrDefault(String key, int defaultValue) {
    String value = C.CONFIG.get(key);
    return C.OBJECT.isEmpty(value) ? defaultValue : Integer.parseInt(value);
}
```

## YAML 到键名的映射

YAML：

```yaml
server:
  port: 9015
app:
  debug: true
  tax_rate: 0.06
  ids:
    - 1001
    - 1002
  port_list:
    - 9015
    - 9016
message_queue:
  host:
    - 192.168.1.10:9092
    - 192.168.1.11:9092
```

读取方式：

```java
int port = C.CONFIG.getInt("server.port");
boolean debug = C.CONFIG.getBoolean("app.debug");
double taxRate = C.CONFIG.getDouble("app.tax_rate");
List<String> mqHosts = C.CONFIG.getArray("message_queue.host");
List<Long> ids = C.CONFIG.getLongArray("app.ids");
List<Integer> ports = C.CONFIG.getIntArray("app.port_list");
```

## 两层配置的推荐分工

推荐按下面方式拆分：

- `common.yml`：公共基础设施配置，例如数据源、Redis、消息队列、文件服务、安全密钥
- `your-service.yml`：当前业务服务自己的配置，例如 `server.name`、`server.alisa`、`server.port` 和业务自定义配置

例如：

```java
Config.load("common", "your-service");
```

表示先加载 `common.yml`，再加载 `your-service.yml`。后加载的同名键会覆盖前者。

补充说明：

- `server.package` 在常规业务项目里通常不需要显式配置
- `message_queue.host`、`redis.host` 既可以写成单个字符串，也可以写成 YAML 数组
- 如果写成 YAML 数组，框架会自动转成逗号分隔字符串再读取

## 优先级

真实优先级如下：

1. 环境变量
2. JVM 系统属性
3. `Config.load(...)` 加载的 YAML
4. `get(key, defaultValue)` 中提供的默认值

实现细节：

- 读取时会把 `.` 转成 `_`
- 框架不会自动把键名转成全大写

例如：

```text
server.port -> server_port
```

## 外部属性文件

`Config.load(...)` 在加载 YAML 后，还会尝试读取外部属性文件：

```text
server.config_path
```

如果没配置，默认路径是：

```text
/jre/config.properties
```

这个外部属性文件中的值会覆盖前面加载的 YAML 配置。

## `@EnvKey`

`@EnvKey` 用于把配置值注入静态字段。`Register` 会在启动时自动处理：

```java
public class EnvConfig {
    @EnvKey("server.port")
    public static int PORT = 9015;

    @EnvKey("app.debug")
    public static boolean DEBUG = false;
}
```

要求：

- 只能标注静态字段
- 字段所在类必须在 `com.szigc.common` 或当前业务根包下
- 注入发生在 `new Register(App.class)` 时

## 推荐做法

1. 启动类中统一调用 `Config.load("common", "your-service")`
2. 简单场景直接用 `C.CONFIG`
3. 需要全局常量时使用 `@EnvKey`
4. 数值默认值自己封装，不要假设框架存在 `getInt(key, default)` 这类重载
5. 把公共基础配置放到 `common.yml`，把业务服务覆盖和业务自定义配置放到 `your-service.yml`
