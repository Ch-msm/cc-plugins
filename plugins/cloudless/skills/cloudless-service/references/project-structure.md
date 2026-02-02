# Cloudless 项目目录结构

## 标准项目结构

```
your-service/
├── build.gradle                    # Gradle构建配置
├── settings.gradle                 # Gradle设置
├── src/
│   ├── main/
│   │   ├── java/com/szigc/xxx/
│   │   │   ├── App.java                    # 启动类
│   │   │   ├── constant/                   # 常量定义
│   │   │   │   ├── Constant.java           # 常量类
│   │   │   │   └── EnvConfig.java          # 环境配置映射
│   │   │   ├── dependency/                 # 外部服务依赖
│   │   │   │   └── OtherService.java       # 其他服务调用
│   │   │   ├── entity/                     # 实体类
│   │   │   │   ├── param/                  # 参数实体
│   │   │   │   │   ├── base/               # 基础参数类
│   │   │   │   │   │   └── BaseSearch.java # 基础搜索类
│   │   │   │   │   └── your_entity/        # 具体实体参数
│   │   │   │   │       ├── Search.java     # 搜索参数
│   │   │   │   │       └── OtherParam.java # 其他参数
│   │   │   │   ├── table/                  # 表实体
│   │   │   │   │   └── YourEntity.java     # 数据库表实体
│   │   │   │   └── view/                   # 视图实体（返回前端）
│   │   │   │       ├── EntityV1.java       # 视图1
│   │   │   │       └── EntityV2.java       # 视图2
│   │   │   ├── service/                    # 服务类
│   │   │   │   └── YourService.java        # 业务服务
│   │   │   └── util/                       # 工具类
│   │   │       └── YourUtil.java           # 项目特定工具
│   │   └── resources/
│   │       ├── your-service.yml            # 服务配置
│   │       └── log4j2.xml                  # 日志配置（可选）
│   └── test/
│       └── java/com/szigc/xxx/
│           └── service/                    # 测试类
│               └── YourServiceTest.java
└── docs/                                  # 文档（可选）
    └── API.md                             # API文档
```

## 目录说明

### App.java - 启动类

```java
package com.szigc.xxx;

import com.szigc.common.Register;
import com.szigc.common.application.Application;
import com.szigc.common.config.Config;

public class App {
    public static void main(String[] args) {
        // 1. 加载配置
        Config.load("common", "your-service");

        // 2. 注册服务（扫描@Service注解）
        new Register(App.class);

        // 3. 启动HTTP服务器
        new Application().start();
    }
}
```

### constant/ - 常量定义

```java
// Constant.java - 业务常量
public class Constant {
    public static final class Cache {
        public static final String USER_INFO = "user:info:";
        public static final String TOKEN = "TOKEN";
    }

    public static final class QueueKafka {
        public static final String USER_UPDATE = "user-update";
    }

    public static final class Subscribe {
        public static final String USER_TABLE = "user-table";
    }

    public static final class Push {
        public static final String USER_CHANGE = "user-change";
    }
}

// EnvConfig.java - 环境配置映射
public class EnvConfig {
    @EnvKey("your_service.timeout")
    public static int TIMEOUT = 30000;

    @EnvKey("your_service.max_retry")
    public static int MAX_RETRY = 3;
}
```

### dependency/ - 外部服务依赖

```java
// OtherService.java - 调用其他服务
public class OtherService {
    public static OtherData getData(String id) {
        var json = ClientStub.send(
            "/other-service/Data/get",
            new RequestBody(),
            C.JSON.toJson(id)
        );
        return C.JSON.fromJson(json, OtherData.class);
    }
}
```

### entity/param/ - 参数实体

```java
// base/BaseSearch.java - 基础搜索类
@Data
@Entity("查询条件基类")
public class BaseSearch {
    @Field(value = "分页:第几页", defaultValue = "1")
    private int pageNo = 1;

    @Field(value = "分页:每页条数", defaultValue = "20")
    private int pageSize = 20;
}

// your_entity/Search.java - 具体搜索参数
@Data
@Entity("查询条件")
public class Search extends BaseSearch {
    @Field(value = "关键字", sample = "名称/编码")
    private String keyword;

    @Field(value = "状态", sample = "0:禁用|1:启用")
    private List<Integer> status;

    @Field(value = "开始时间")
    private long startTime;

    @Field(value = "结束时间")
    private long endTime;

    @Field(value = "ID集合")
    private List<String> ids;
}
```

### entity/table/ - 表实体

```java
// YourEntity.java - 数据库表实体
@Data
@Entity(value = "表名", logicDelete = true)  // logicDelete是否逻辑删除
public class YourEntity {
    @Field(value = "主键", primary = true)
    private String id;

    @Field(value = "名称", required = true, length = 100)
    private String name;

    @Field(value = "编码", length = 50)
    private String code;

    @Field(value = "状态", check = "0|1|2", sample = "0:禁用|1:启用|2:删除")
    private int status;

    @Field(value = "排序")
    private int sortOrder;

    @Field(value = "创建时间")
    private long createTime;

    @Field(value = "更新时间")
    private long updateTime;

    // 内部类（关联表）
    @Entity("关联表名")
    public static class SubEntity {
        @Field(value = "主键")
        private String subId;

        @Field(value = "父ID")
        private String parentId;

        @Field(value = "名称")
        private String name;
    }
}
```

### entity/view/ - 视图实体

```java
// EntityV1.java - 返回前端的视图
@Data
public class EntityV1 extends YourEntity {
    @Field(value = "扩展字段")
    private String extraField;

    @Field(value = "关联数据")
    private List<SubEntity> subList = new ArrayList<>();

    @Field(value = "统计数量")
    private int count;
}

// EntityV2.java - 另一个视图
@Data
public class EntityV2 {
    @Field(value = "ID")
    private String id;

    @Field(value = "名称")
    private String name;

    @Field(value = "状态描述")
    private String statusDesc;

    @Field(value = "创建时间")
    private String createTimeStr;
}
```

### service/ - 服务类

```java
// YourService.java - 业务服务
@Service(value = "服务名称", author = "梅思铭", date = "2025-01-08")
public class YourService extends AbstractService {
    private static final MainDB<YourEntity> DB = new MainDB<>(YourEntity.class);

    @Override
    public void init() {
        DB.use().createTable();
        // 创建索引等
    }

    // 业务方法...
}
```

## resources/ 配置文件

### your-service.yml

```yaml
server:
  name: your-service
  alisa: 你的服务
  port: 9001

your_service:
  timeout: 30000
  max_retry: 3
```

## build.gradle

```gradle
plugins {
    id 'java'
}

group = 'com.szigc'
version = '1.0.0-SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    // cloudless-common依赖
    implementation 'com.szigc:common:1.0.0-SNAPSHOT'

    // 测试依赖
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.0'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.0'
}

test {
    useJUnitPlatform()
}
```

## 包命名规范

| 包名 | 说明 | 示例 |
|------|------|------|
| com.szigc.xxx | 项目基础包 | com.szigc.user_center |
| com.szigc.xxx.constant | 常量 | com.szigc.user_center.constant |
| com.szigc.xxx.dependency | 外部依赖 | com.szigc.user_center.dependency |
| com.szigc.xxx.entity | 实体 | com.szigc.user_center.entity |
| com.szigc.xxx.entity.param | 参数实体 | com.szigc.user_center.entity.param |
| com.szigc.xxx.entity.table | 表实体 | com.szigc.user_center.entity.table |
| com.szigc.xxx.entity.view | 视图实体 | com.szigc.user_center.entity.view |
| com.szigc.xxx.service | 服务 | com.szigc.user_center.service |
| com.szigc.xxx.util | 工具类 | com.szigc.user_center.util |

## 命名规范

### 类命名

- **服务类**: `XxxService` (例: `UserService`)
- **实体类**: `Xxx` (例: `User`)
- **视图类**: `XxxV1`, `XxxV2` (例: `UserV1`)
- **参数类**: `Search`, `XxxParam` (例: `Search`)
- **常量类**: `Constant`, `EnvConfig`
- **依赖类**: `XxxService`, `XxxCenter` (例: `UserCenter`)

### 方法命名

- **新增**: `insert`
- **更新**: `update`
- **删除**: `delete`
- **查询**: `find`
- **获取单个**: `get`
- **统计**: `statistics`
- **导出**: `export`
- **导入**: `importInsert`, `importUpdate`

### 字段命名

- 使用驼峰命名
- 布尔类型使用 `is` 前缀 (例: `isEnabled`)
- 时间戳使用 `xxxTime` (例: `createTime`)
- ID使用 `id` 或 `xxxId` (例: `userId`)
