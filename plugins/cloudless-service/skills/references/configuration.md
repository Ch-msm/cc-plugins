# 配置系统完整指南

## 目录

- [C.CONFIG 使用](#cconfig-使用)
- [YAML配置文件](#yaml配置文件)
- [环境变量](#环境变量)
- [@EnvKey 注解](#envkey-注解)
- [配置最佳实践](#配置最佳实践)

---

## C.CONFIG 使用

### 基础用法

```java
// 获取字符串配置
String value = C.CONFIG.get("config.key");

// 获取带默认值的配置
String value = C.CONFIG.get("config.key", "defaultValue");

// 获取整数
int port = C.CONFIG.getInt("server.port", 8080);

// 获取长整型
long timeout = C.CONFIG.getLong("server.timeout", 5000L);

// 获取布尔值
boolean debug = C.CONFIG.getBoolean("app.debug", false);

// 获取双精度浮点数
double rate = C.CONFIG.getDouble("tax.rate", 0.0);
```

### 数组类型配置

```java
// 获取字符串数组
List<String> servers = C.CONFIG.getArray("cluster.servers");
// 配置: cluster.servers: server1,server2,server3
// 结果: ["server1", "server2", "server3"]

// 获取整数数组
List<Integer> ports = C.CONFIG.getIntArray("server.ports");
// 配置: server.ports: 8080,8081,8082
// 结果: [8080, 8081, 8082]

// 获取长整型数组
List<Long> ids = C.CONFIG.getLongArray("user.admin_ids");

// 获取双精度数组
List<Double> rates = C.CONFIG.getDoubleArray("tax.rates");

// 获取布尔数组
List<Boolean> flags = C.CONFIG.getBooleanArray("feature.flags");
```

### 配置读取优先级

```java
// 配置读取优先级（从高到低）：
// 1. 环境变量（key中的.替换为_）
// 2. 系统属性（-D参数）
// 3. YAML配置文件
// 4. 默认值

// 示例：
// YAML配置: server.port: 8080
// 环境变量: SERVER_PORT=9090
// 系统属性: -Dserver_port=7070

// 实际读取会返回：7070（系统属性优先级最高）
int port = C.CONFIG.getInt("server.port", 8080);
```

---

## YAML配置文件

### 配置文件位置

```
src/main/resources/
├── app.yml           # 应用配置（主配置）
├── db.yml            # 数据库配置
├── redis.yml         # Redis配置
└── kafka.yml         # Kafka配置
```

### YAML配置格式

```yaml
# app.yml
app:
  name: "用户中心服务"
  version: "1.0.0"
  debug: true

server:
  port: 9001
  host: "0.0.0.0"
  timeout: 30000

database:
  url: "jdbc:postgresql://localhost:5432/mydb"
  username: "admin"
  password: "password"
  pool:
    min: 5
    max: 20

redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0

# 数组配置
cluster:
  nodes:
    - "node1:8080"
    - "node2:8080"
    - "node3:8080"

# 逗号分隔的数组（推荐）
servers:
  allowed: "server1,server2,server3"
```

### 多配置文件加载

```java
// Config类会自动加载多个配置文件
// 加载顺序：app.yml -> db.yml -> redis.yml -> kafka.yml
// 后加载的配置会覆盖先加载的同名配置

// Config.load() 在应用启动时自动调用
// 加载resources目录下的所有yml文件
```

### 配置文件示例

```yaml
# app.yml - 应用主配置
app:
  name: "数据服务"
  version: "1.0.0"
  debug: false
  author: "梅思铭"

server:
  port: 9001
  host: "0.0.0.0"

# 上传配置
upload:
  path: "/data/upload"
  max_size: 104857600  # 100MB
  allowed_types: "jpg,jpeg,png,pdf,doc,docx,xls,xlsx"

# 缓存配置
cache:
  expire: 3600  # 秒
  prefix: "app:"

# 消息队列配置
queue:
  kafka:
    bootstrap_servers: "localhost:9092"
    topics:
      user_update: "user-update"
      data_change: "data-change"
```

---

## 环境变量

### 环境变量命名规则

```bash
# YAML配置中的键: server.port
# 环境变量名称: SERVER_PORT（.替换为_，大写）

# 示例：
export SERVER_PORT=9001
export APP_DEBUG=true
export DATABASE_URL="jdbc:postgresql://localhost:5432/mydb"
export REDIS_HOST="localhost"
export REDIS_PORT=6379
```

### 环境变量使用场景

```java
// 场景1：Docker容器化部署
// docker-compose.yml
environment:
  - SERVER_PORT=9001
  - DATABASE_URL=jdbc:postgresql://db:5432/mydb
  - REDIS_HOST=redis

// 场景2：Kubernetes部署
// deployment.yaml
env:
  - name: SERVER_PORT
    value: "9001"
  - name: DATABASE_URL
    valueFrom:
      configMapKeyRef:
        name: db-config
        key: url

// 场景3：本地开发
// .env 文件
SERVER_PORT=9001
APP_DEBUG=true
DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
```

### 环境变量优先级示例

```java
// YAML配置
// app.yml:
//   server.port: 8080
//   app.debug: false

// 环境变量
// export SERVER_PORT=9001
// export APP_DEBUG=true

// 实际读取结果
int port = C.CONFIG.getInt("server.port");  // 9001（环境变量）
boolean debug = C.CONFIG.getBoolean("app.debug");  // true（环境变量）
```

### 系统属性（-D参数）

```bash
# 启动时设置系统属性
java -jar app.jar -Dserver_port=9001 -Dapp_debug=true

# 或在代码中设置
System.setProperty("server_port", "9001");
System.setProperty("app_debug", "true");
```

---

## @EnvKey 注解

### 基本用法

```java
@Service(value = "配置服务", author = "梅思铭", date = "2025-01-08")
public class ConfigService extends AbstractService {

    // 自动注入配置到字段
    @EnvKey("server.port")
    private int serverPort;

    @EnvKey("app.debug")
    private boolean debug;

    @EnvKey("upload.path")
    private String uploadPath;

    @Method(value = "获取配置", status = MethodStatus.COMPLETE)
    public void printConfig() {
        C.LOG.info("端口: {}", serverPort);
        C.LOG.info("调试: {}", debug);
        C.LOG.info("上传路径: {}", uploadPath);
    }
}
```

### 带默认值的注入

```java
@Service(value = "配置服务", author = "梅思铭", date = "2025-01-08")
public class ConfigService extends AbstractService {

    // 字段必须有初始值作为默认值
    @EnvKey("server.timeout")
    private int timeout = 30000;  // 默认30秒

    @EnvKey("cache.expire")
    private long cacheExpire = 3600L;  // 默认1小时

    @EnvKey("app.debug")
    private boolean debug = false;  // 默认关闭

    @EnvKey("upload.path")
    private String uploadPath = "/tmp/upload";  // 默认路径
}
```

### 注入数组类型

```java
@Service(value = "配置服务", author = "梅思铭", date = "2025-01-08")
public class ConfigService extends AbstractService {

    // 注入字符串数组
    @EnvKey("cluster.servers")
    private String[] servers;

    // 注入整数数组
    @EnvKey("server.ports")
    private int[] ports;

    // 注入长整型数组
    @EnvKey("user.admin_ids")
    private long[] adminIds;

    @Method(value = "打印配置", status = MethodStatus.COMPLETE)
    public void printConfig() {
        C.LOG.info("集群节点: {}", Arrays.toString(servers));
        C.LOG.info("端口列表: {}", Arrays.toString(ports));
        C.LOG.info("管理员ID: {}", Arrays.toString(adminIds));
    }
}
```

### @EnvKey 完整示例

```java
@Service(value = "文件服务", author = "梅思铭", date = "2025-01-08")
public class FileService extends AbstractService {

    // 配置自动注入
    @EnvKey("upload.path")
    private String uploadPath = "/data/upload";

    @EnvKey("upload.max_size")
    private long maxSize = 104857600L;  // 100MB

    @EnvKey("upload.allowed_types")
    private String allowedTypes = "jpg,png,pdf";

    @Method(value = "上传文件", status = MethodStatus.COMPLETE)
    public String uploadFile(File file) {
        // 验证文件大小
        if (file.length() > maxSize) {
            throw new AppRuntimeException("文件超过大小限制");
        }

        // 验证文件类型
        String ext = getFileExtension(file.getName());
        if (!Arrays.asList(allowedTypes.split(",")).contains(ext)) {
            throw new AppRuntimeException("不支持的文件类型");
        }

        // 上传文件
        return C.FILE.upload(file).getId();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
```

---

## 配置最佳实践

### 1. 配置分层

```yaml
# 推荐：按功能模块分组
app:
  name: "用户服务"
  version: "1.0.0"

server:
  port: 9001
  timeout: 30000

database:
  url: "jdbc:postgresql://localhost:5432/userdb"
  pool:
    min: 5
    max: 20

redis:
  mode: "cluster"
  nodes:
    - "redis1:6379"
    - "redis2:6379"
    - "redis3:6379"

cache:
  expire: 3600
  prefix: "user:"

# ❌ 不推荐：扁平化配置
app_name: "用户服务"
server_port: 9001
database_url: "jdbc:postgresql://localhost:5432/userdb"
```

### 2. 配置默认值

```java
// ✅ 推荐：总是提供默认值
int port = C.CONFIG.getInt("server.port", 8080);
boolean debug = C.CONFIG.getBoolean("app.debug", false);
String path = C.CONFIG.get("upload.path", "/tmp/upload");

@EnvKey("cache.expire")
private long cacheExpire = 3600L;  // 字段初始值作为默认值

// ❌ 不推荐：没有默认值可能导致NPE
int port = C.CONFIG.getInt("server.port");  // 可能抛出异常
```

### 3. 敏感配置处理

```yaml
# ❌ 不推荐：敏感信息写在配置文件中
database:
  password: "plain_text_password"

# ✅ 推荐：使用环境变量
# 配置文件只保留键名
database:
  password: ""  # 通过环境变量 DATABASE_PASSWORD 注入

# 或使用默认值提示需要配置
database:
  password: "${DATABASE_PASSWORD:please_set_password}"
```

```bash
# 设置环境变量
export DATABASE_PASSWORD="secure_password"
```

### 4. 配置验证

```java
@Service(value = "配置服务", author = "梅思铭", date = "2025-01-08")
public class ConfigService extends AbstractService {

    @EnvKey("server.port")
    private int port = 8080;

    @EnvKey("database.url")
    private String databaseUrl;

    @Override
    public void init() {
        // 验证必需配置
        if (C.OBJECT.isEmpty(databaseUrl)) {
            throw new AppRuntimeException("数据库URL未配置");
        }

        // 验证端口范围
        if (port < 1024 || port > 65535) {
            throw new AppRuntimeException("端口号超出范围: " + port);
        }

        C.LOG.info("配置验证通过");
    }
}
```

### 5. 配置集中管理

```java
// ✅ 推荐：创建配置常量类
public class AppConfig {
    private static final String PREFIX = "app";

    public static String getName() {
        return C.CONFIG.get(PREFIX + ".name", "Unknown");
    }

    public static String getVersion() {
        return C.CONFIG.get(PREFIX + ".version", "1.0.0");
    }

    public static boolean isDebug() {
        return C.CONFIG.getBoolean(PREFIX + ".debug", false);
    }
}

// 使用
@Service(value = "用户服务", author = "梅思铭", date = "2025-01-08")
public class UserService extends AbstractService {

    @Method(value = "获取信息", status = MethodStatus.COMPLETE)
    public void getInfo() {
        C.LOG.info("服务: {}", AppConfig.getName());
        C.LOG.info("版本: {}", AppConfig.getVersion());
        C.LOG.info("调试: {}", AppConfig.isDebug());
    }
}
```

### 6. 环境区分配置

```yaml
# app.yml
app:
  env: "dev"  # dev/test/prod

dev:
  database:
    url: "jdbc:postgresql://localhost:5432/dev_db"
  debug: true

test:
  database:
    url: "jdbc:postgresql://test-db:5432/test_db"
  debug: false

prod:
  database:
    url: "jdbc:postgresql://prod-db:5432/prod_db"
  debug: false
```

```java
// 根据环境读取配置
@Method(value = "获取数据库URL", status = MethodStatus.COMPLETE)
public String getDatabaseUrl() {
    String env = C.CONFIG.get("app.env", "dev");
    return C.CONFIG.get(env + ".database.url");
}
```

---

## 配置使用场景

### 场景1：数据库配置

```java
@Service(value = "数据源服务", author = "梅思铭", date = "2025-01-08")
public class DataSourceService extends AbstractService {

    @EnvKey("database.url")
    private String url;

    @EnvKey("database.username")
    private String username;

    @EnvKey("database.password")
    private String password;

    @EnvKey("database.pool.min")
    private int poolMin = 5;

    @EnvKey("database.pool.max")
    private int poolMax = 20;

    @Override
    public void init() {
        C.LOG.info("数据库连接: {}", url);
        C.LOG.info("连接池: {}-{}", poolMin, poolMax);
    }
}
```

### 场景2：第三方服务配置

```java
@Service(value = "短信服务", author = "梅思铭", date = "2025-01-08")
public class SmsService extends AbstractService {

    @EnvKey("sms.api_url")
    private String apiUrl;

    @EnvKey("sms.app_id")
    private String appId;

    @EnvKey("sms.app_key")
    private String appKey;

    @EnvKey("sms.timeout")
    private int timeout = 5000;

    @Method(value = "发送短信", status = MethodStatus.COMPLETE)
    public void sendSms(String phone, String code) {
        var url = apiUrl + "/send";
        var data = Map.of(
            "appId", appId,
            "appKey", appKey,
            "phone", phone,
            "code", code
        );

        String response = C.HTTP.post(url, C.JSON.toJson(data));
        C.LOG.info("短信发送成功: {}", phone);
    }
}
```

### 场景3：功能开关

```java
@Service(value = "功能管理", author = "梅思铭", date = "2025-01-08")
public class FeatureService extends AbstractService {

    @EnvKey("feature.new_ui")
    private boolean newUiEnabled = false;

    @EnvKey("feature.cache")
    private boolean cacheEnabled = true;

    @EnvKey("feature.async")
    private boolean asyncEnabled = false;

    @Method(value = "查询数据", status = MethodStatus.COMPLETE)
    public DataList<Entity> find(Search search) {
        // 功能开关控制
        if (cacheEnabled) {
            String cacheKey = "search:" + search.hashCode();
            String cached = C.CACHE.get(cacheKey);
            if (cached != null) {
                return C.JSON.fromJson(cached, DataList.class);
            }
        }

        var result = DB.use().query();

        if (cacheEnabled) {
            String cacheKey = "search:" + search.hashCode();
            C.CACHE.set(cacheKey, C.JSON.toJson(result), 600);
        }

        return result;
    }
}
```

---

## 配置调试

### 查看所有配置

```java
@Method(value = "查看配置", status = MethodStatus.COMPLETE)
public void printAllConfig() {
    // 打印系统属性
    System.getProperties().forEach((k, v) -> {
        C.LOG.info("{} = {}", k, v);
    });

    // 打印环境变量
    System.getenv().forEach((k, v) -> {
        C.LOG.info("{} = {}", k, v);
    });
}

@Method(value = "查看特定配置", status = MethodStatus.COMPLETE)
public void printConfig(String prefix) {
    // 打印特定前缀的配置
    System.getProperties().forEach((k, v) -> {
        if (k.toString().startsWith(prefix)) {
            C.LOG.info("{} = {}", k, v);
        }
    });
}
```

### 配置验证工具

```java
@Method(value = "验证配置", status = MethodStatus.COMPLETE)
public void validateConfig() {
    List<String> errors = new ArrayList<>();

    // 验证必需配置
    if (C.OBJECT.isEmpty(C.CONFIG.get("database.url"))) {
        errors.add("database.url 未配置");
    }

    if (C.OBJECT.isEmpty(C.CONFIG.get("redis.host"))) {
        errors.add("redis.host 未配置");
    }

    // 验证端口
    int port = C.CONFIG.getInt("server.port", 8080);
    if (port < 1024 || port > 65535) {
        errors.add("server.port 端口范围错误: " + port);
    }

    if (!errors.isEmpty()) {
        throw new AppRuntimeException("配置验证失败:\n" + String.join("\n", errors));
    }

    C.LOG.info("配置验证通过");
}
```
