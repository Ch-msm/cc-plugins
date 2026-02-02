# Cloudless 实体类定义完整指南

## 目录

- [@Entity 注解](#entity-注解)
- [@Field 注解](#field-注解)
- [表实体定义](#表实体定义)
- [视图实体定义](#视图实体定义)
- [参数实体定义](#参数实体定义)
- [字段类型映射](#字段类型映射)
- [实体类最佳实践](#实体类最佳实践)

---

## @Entity 注解

标记类为数据库表实体。

```java
@Entity(
    value = "表名",           // 表名（必填）
    logicDelete = true        // 是否逻辑删除（默认true）
)
public class YourEntity { }
```

### 属性说明

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| value | String | 是 | - | 数据库表名 |
| logicDelete | boolean | 否 | true | 是否使用逻辑删除 |

### logicDelete 说明

- `true`: 启用逻辑删除，删除时只标记 `deleteTime` 字段
- `false`: 物理删除，直接从数据库删除记录

---

## @Field 注解

标记实体类字段，映射到数据库列。

```java
@Field(
    value = "字段说明",     // 必填
    primary = false,       // 是否主键
    required = false,      // 是否必填
    length = 255,          // 字段长度
    check = "",            // 数据校验规则
    sample = "",           // 示例值
    defaultValue = ""      // 默认值
)
private String field;
```

### 属性说明

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| value | String | 是 | - | 字段说明 |
| primary | boolean | 否 | false | 是否主键 |
| required | boolean | 否 | false | 是否必填 |
| length | int | 否 | 255 | 字段长度（-1表示TEXT） |
| check | String | 否 | "" | 数据校验规则 |
| sample | String | 否 | "" | 示例值 |
| defaultValue | String | 否 | "" | 默认值 |

### check 校验规则

使用 `|` 分隔多个允许值：

```java
@Field(value = "状态", check = "0|1|2", sample = "0:禁用\|1:启用\|2:删除")
private int status;
```

---

## 表实体定义

### 基础表实体

```java
@Data
@Entity(value = "用户", logicDelete = true)
public class User {
    @Field(value = "主键", primary = true)
    private String id;

    @Field(value = "用户名", required = true, length = 50)
    private String username;

    @Field(value = "密码", required = true, length = 100)
    private String password;

    @Field(value = "姓名", length = 100)
    private String name;

    @Field(value = "手机号", length = 20)
    private String phone;

    @Field(value = "邮箱", length = 100)
    private String email;

    @Field(value = "状态", check = "0|1", sample = "0:禁用\|1:启用", defaultValue = "1")
    private int status = 1;

    @Field(value = "排序", defaultValue = "0")
    private int sortOrder = 0;

    @Field(value = "创建时间")
    private long createTime;

    @Field(value = "更新时间")
    private long updateTime;
}
```

### 带内部类的表实体

```java
@Data
@Entity(value = "角色", logicDelete = true)
public class Role {
    @Field(value = "主键", primary = true)
    private String id;

    @Field(value = "角色名称", required = true, length = 50)
    private String name;

    @Field(value = "角色编码", length = 50)
    private String code;

    // 内部类 - 角色权限关联
    @Entity("角色权限关联")
    public static class Power {
        @Field(value = "角色ID", primary = true)
        private String roleId;

        @Field(value = "权限ID", primary = true)
        private String powerId;
    }

    // 内部类 - 角色用户关联
    @Entity("角色用户关联")
    public static class User {
        @Field(value = "角色ID", primary = true)
        private String roleId;

        @Field(value = "用户ID", primary = true)
        private String userId;
    }
}
```

### 时序数据实体

```java
@Data
@Entity(value = "测点数据", logicDelete = false)
public class PointData {
    @Field(value = "测点ID")
    private String pointId;

    @Field(value = "时间", primary = true)
    private long time;

    @Field(value = "数值")
    private double value;

    @Field(value = "数据质量")
    private int quality;
}
```

---

## 视图实体定义

视图实体用于返回给前端的数据，不直接映射数据库表。

### 基础视图实体

```java
@Data
public class UserV1 extends User {
    @Field(value = "角色列表")
    private List<Role> roles = new ArrayList<>();

    @Field(value = "权限数量")
    private int powerCount;

    @Field(value = "最后登录时间")
    private String lastLoginTimeStr;
}
```

### 组合视图实体

```java
@Data
public class UserRoleView {
    @Field(value = "用户ID")
    private String userId;

    @Field(value = "用户名")
    private String username;

    @Field(value = "姓名")
    private String name;

    @Field(value = "角色列表")
    private List<Role> roles;

    @Field(value = "权限列表")
    private List<Power> powers;

    @Field(value = "所属组织")
    private Organization organization;
}
```

### 统计视图实体

```java
@Data
public class StatisticsData {
    @Field(value = "时间/标签")
    private String name;

    @Field(value = "数量")
    private long count;

    @Field(value = "平均值")
    private double avg;

    @Field(value = "最大值")
    private double max;

    @Field(value = "最小值")
    private double min;
}
```

---

## 参数实体定义

### 基础搜索参数

```java
@Data
@Entity("查询条件基类")
public class BaseSearch {
    @Field(value = "分页:第几页", defaultValue = "1")
    private int pageNo = 1;

    @Field(value = "分页:每页条数", defaultValue = "20")
    private int pageSize = 20;
}
```

### 完整搜索参数

```java
@Data
@Entity("查询条件")
public class Search extends BaseSearch {
    @Field(value = "关键字", sample = "用户名/姓名/手机号")
    private String keyword;

    @Field(value = "ID集合")
    private List<String> ids;

    @Field(value = "状态", sample = "0:禁用\|1:启用")
    private List<Integer> status;

    @Field(value = "角色ID")
    private String roleId;

    @Field(value = "组织ID")
    private String organizationId;

    @Field(value = "开始时间")
    private long startTime;

    @Field(value = "结束时间")
    private long endTime;
}
```

### 操作参数

```java
@Data
@Entity("批量操作参数")
public class BatchOperation {
    @Field(value = "ID集合", required = true)
    private List<String> ids;

    @Field(value = "操作类型", required = true, check = "1|2|3",
           sample = "1:启用\|2:禁用\|3:删除")
    private int operation;
}
```

### 导入导出参数

```java
@Data
@Entity("导出项")
public class ExportItem {
    @Field(value = "字段名", required = true)
    private String field;

    @Field(value = "显示名称", required = true)
    private String label;

    @Field(value = "宽度", defaultValue = "200")
    private int width = 200;
}

@Data
@Entity("导出参数")
public class ExportParam {
    @Field(value = "查询条件")
    private Search search;

    @Field(value = "导出项", required = true)
    private List<ExportItem> exportItems;

    @Field(value = "文件名", required = true)
    private String fileName;
}
```

---

## 字段类型映射

### Java类型到数据库类型映射

| Java类型 | 数据库类型 | 说明 |
|---------|-----------|------|
| String | VARCHAR(length) | 字符串 |
| String (length=-1) | TEXT | 长文本 |
| int / Integer | INT | 整数 |
| long / Long | BIGINT | 长整数 |
| double / Double | DOUBLE | 浮点数 |
| boolean / Boolean | BOOLEAN | 布尔值 |
| java.util.Date | TIMESTAMP | 日期时间 |
| java.time.LocalDateTime | TIMESTAMP | 日期时间 |
| java.time.LocalDate | DATE | 日期 |
| List<String> | TEXT | 存储为JSON数组 |

### 特殊字段

```java
// 主键
@Field(value = "主键", primary = true)
private String id;

// 必填字段
@Field(value = "名称", required = true)
private String name;

// 枚举字段
@Field(value = "类型", check = "1|2|3", sample = "1:类型A\|2:类型B\|3:类型C")
private int type;

// 长文本
@Field(value = "内容", length = -1)
private String content;

// 时间戳
@Field(value = "创建时间")
private long createTime;
```

---

## 实体类最佳实践

### 1. 使用Lombok简化代码

```java
@Data                    // 自动生成getter/setter
@Entity(value = "表名")
public class YourEntity {
    // 字段定义
}
```

### 2. 实体类继承

```java
// 基础实体
@Data
@Entity("基础实体")
public abstract class BaseEntity {
    @Field(value = "创建时间")
    private long createTime;

    @Field(value = "更新时间")
    private long updateTime;
}

// 具体实体
@Data
@Entity(value = "用户", logicDelete = true)
public class User extends BaseEntity {
    @Field(value = "主键", primary = true)
    private String id;

    @Field(value = "用户名")
    private String username;
}
```

### 3. 字段默认值

```java
@Data
@Entity(value = "配置", logicDelete = false)
public class Config {
    @Field(value = "配置键", primary = true)
    private String key;

    @Field(value = "配置值")
    private String value;

    @Field(value = "是否启用", defaultValue = "true")
    private boolean enabled = true;

    @Field(value = "排序", defaultValue = "0")
    private int sortOrder = 0;
}
```

### 4. 枚举映射

```java
public enum UserStatus {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    DELETED(2, "删除");

    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

// 在实体中使用
@Field(value = "状态", check = "0|1|2", sample = "0:禁用\|1:启用\|2:删除")
private int status;
```

### 5. 日期时间处理

```java
@Data
@Entity(value = "事件", logicDelete = true)
public class Event {
    @Field(value = "主键", primary = true)
    private String id;

    // 使用时间戳（推荐）
    @Field(value = "事件时间")
    private long eventTime;

    // 或使用LocalDateTime
    @Field(value = "创建时间")
    private LocalDateTime createTime;

    // 方法：转换时间戳为字符串
    public String getEventTimeStr() {
        return C.TIME.toDateTimeString(eventTime);
    }
}
```

### 6. 关联数据处理

```java
@Data
@Entity(value = "订单", logicDelete = true)
public class Order {
    @Field(value = "主键", primary = true)
    private String id;

    @Field(value = "用户ID")
    private String userId;

    // 不在数据库存储，通过查询填充
    @Transient
    private User user;

    // 关联的订单项
    @Transient
    private List<OrderItem> items;
}

// 查询后填充关联数据
var orders = DB.use().query();
var userIds = orders.stream().map(Order::getUserId).toList();
var users = userDB.use().in(User::getId, userIds).query();
var userMap = C.DATA.toMap(users, User::getId);

orders.forEach(order -> {
    order.setUser(userMap.get(order.getUserId()));
});
```

---

## 实体类注解完整示例

```java
package com.szigc.xxx.entity.table;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import lombok.Data;

/**
 * 用户实体
 */
@Data
@Entity(value = "用户", logicDelete = true)
public class User {
    // 主键
    @Field(value = "主键", primary = true)
    private String id;

    // 基本信息
    @Field(value = "用户名", required = true, length = 50)
    private String username;

    @Field(value = "密码", required = true, length = 100)
    private String password;

    @Field(value = "姓名", length = 100)
    private String name;

    @Field(value = "手机号", length = 20)
    private String phone;

    @Field(value = "邮箱", length = 100)
    private String email;

    // 状态字段
    @Field(value = "状态", check = "0|1", sample = "0:禁用\|1:启用", defaultValue = "1")
    private int status = 1;

    @Field(value = "排序", defaultValue = "0")
    private int sortOrder = 0;

    // 时间字段
    @Field(value = "创建时间")
    private long createTime;

    @Field(value = "更新时间")
    private long updateTime;

    @Field(value = "删除时间")
    private Long deleteTime;

    // 关联表 - 用户角色
    @Entity("用户角色关联")
    public static class Role {
        @Field(value = "用户ID", primary = true)
        private String userId;

        @Field(value = "角色ID", primary = true)
        private String roleId;
    }

    // 关联表 - 用户组织
    @Entity("用户组织关联")
    public static class Organization {
        @Field(value = "用户ID", primary = true)
        private String userId;

        @Field(value = "组织ID", primary = true)
        private String organizationId;
    }
}
```
