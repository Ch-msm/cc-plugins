# MainDB 数据库操作完整指南

## 目录

- [基础操作](#基础操作)
- [条件构建](#条件构建)
- [链式查询](#链式查询)
- [聚合查询](#聚合查询)
- [时序数据库](#时序数据库)
- [表管理](#表管理)
- [最佳实践](#最佳实践)

---

## 基础操作

### DB声明

```java
// 默认数据库（PostgreSQL/达梦/Vastbase/OpenGauss）
private static final MainDB<Entity> DB = new MainDB<>(Entity.class);

// ClickHouse时序数据库
private static final MainDB<PointData> TIME_DB = new MainDB<>(PointData.class, "Clickhouse");

// 获取DB实例
var db = DB.use();
```

### 插入操作

```java
// 插入单个对象
Entity entity = new Entity();
entity.setId("001");
entity.setName("测试");
DB.use().insert(entity);

// 批量插入
List<Entity> list = List.of(
    new Entity("001", "测试1"),
    new Entity("002", "测试2")
);
DB.use().insert(list);

// 插入时忽略某些字段
DB.use().exclude(Entity::getStatus).insert(entity);
```

### 更新操作

```java
// 更新整个对象
entity.setName("新名称");
DB.use().update(entity);

// 更新指定字段
DB.use().eq(Entity::getId, "001")
    .update(Entity::getName, "新名称");

// 更新多个字段
DB.use().eq(Entity::getId, "001")
    .update(
        Entity::getName, "新名称",
        Entity::getStatus, 1
    );

// 计算更新（+1/-1）
DB.use()
    .eq(Entity::getId, "001")
    .updateCalculate(Entity::getCount, "+1")
    .update();

// 排除字段更新
DB.use().exclude(Entity::getType).update(entity);
```

### 删除操作

```java
// 根据条件删除
DB.use().eq(Entity::getId, "001").delete();

// 批量删除
DB.use().in(Entity::getId, ids).delete();

// 多条件删除
DB.use()
    .eq(Entity::getStatus, 0)
    .lt(Entity::getCreateTime, timestamp)
    .delete();
```

### 查询操作

```java
// 查询单个对象
Entity entity = DB.use().eq(Entity::getId, "001").get();

// 查询列表
List<Entity> list = DB.use().query();

// 查询指定类型（视图类）
List<EntityView> views = DB.use().query(EntityView.class);

// 查询并包含指定字段
List<String> names = DB.use()
    .include(Entity::getName)
    .query(String.class);

// 查询数量
long count = DB.use().count();

// 判断存在
boolean exists = DB.use().eq(Entity::getName, "测试").exist();
```

---

## 条件构建

### 等值条件

```java
// 等于（第三个参数为true时跳过该条件）
db.eq(Entity::getId, id, C.OBJECT.isEmpty(id))

// 不等于
db.notEq(Entity::getStatus, 0)

// 多个等于条件
db.eq(Entity::getType, 1)
  .eq(Entity::getStatus, 1)
```

### 范围条件

```java
// IN查询
db.in(Entity::getId, ids, C.OBJECT.isEmpty(ids))

// NOT IN查询
db.notIn(Entity::getStatus, List.of(0, 2))

// BETWEEN查询
db.between(Entity::getTime, startTime, endTime,
          C.OBJECT.isEmpty(startTime) || C.OBJECT.isEmpty(endTime))

// 大于
db.gt(Entity::getId, minId)

// 大于等于
db.ge(Entity::getCreateTime, timestamp)

// 小于
db.lt(Entity::getCreateTime, timestamp)

// 小于等于
db.le(Entity::getPrice, 100.0)
```

### 模糊查询

```java
// 单字段模糊查询
db.iLike(Entity::getName, "%测试%", skip)

// 多字段模糊查询
db.iLike(
    List.of(Entity::getName, Entity::getCode, Entity::getPhone),
    "%" + keyword + "%",
    C.OBJECT.isEmpty(keyword)
)

// 左模糊
db.iLike(Entity::getName, keyword + "%")

// 右模糊
db.iLike(Entity::getName, "%" + keyword)
```

### 包含查询

```java
// 字符串包含
db.contain(Entity::getTags, "tag1")

// 数组包含
db.contain(Entity::getUserIds, userId)
```

### 排序

```java
// 升序
db.orderBy(Entity::getId)

// 降序
db.orderByDesc(Entity::getCreateTime)

// 多字段排序
db.orderBy(Entity::getStatus)
  .orderByDesc(Entity::getId)
```

### 分页和限制

```java
// 分页
db.paging(pageNo, pageSize)

// 限制数量
db.limit(100)

// 先count再分页
dataList.setTotal(db.count());
dataList.setList(db.paging(pageNo, pageSize).query());
```

---

## 链式查询

### 基础链式查询

```java
var list = DB.use()
    .eq(Entity::getStatus, 1)
    .in(Entity::getType, types, C.OBJECT.isEmpty(types))
    .iLike(List.of(Entity::getName, Entity::getCode),
           "%" + keyword + "%",
           C.OBJECT.isEmpty(keyword))
    .orderByDesc(Entity::getId)
    .query();
```

### 复杂链式查询示例

```java
// 构建查询条件
private DB<Entity> getSearchDB(Search search) {
    return DB.use()
        .eq(Entity::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
        .in(Entity::getStatus, search.getStatus(), C.OBJECT.isEmpty(search.getStatus()))
        .in(Entity::getType, search.getTypes(), C.OBJECT.isEmpty(search.getTypes()))
        .between(Entity::getCreateTime, search.getStartTime(), search.getEndTime(),
                 C.OBJECT.isEmpty(search.getStartTime()) || C.OBJECT.isEmpty(search.getEndTime()))
        .iLike(List.of(Entity::getName, Entity::getCode, Entity::getPhone),
               "%" + search.getKeyword() + "%",
               C.OBJECT.isEmpty(search.getKeyword()))
        .orderByDesc(Entity::getCreateTime);
}

// 使用查询
var db = getSearchDB(search);
var total = db.count();
var list = db.paging(search.getPageNo(), search.getPageSize()).query();
```

### 分页查询完整示例

```java
@Method(value = "查询", status = MethodStatus.COMPLETE)
@ReturnData(type = Entity.class)
public DataList<Entity> find(Search search) {
    var dataList = new DataList<Entity>();
    var db = getSearchDB(search);

    // 设置总数
    if (search.getPageNo() != -1) {
        dataList.setTotal(db.count());
    }

    // 设置列表
    dataList.setList(
        db.paging(search.getPageNo(), search.getPageSize())
            .orderByDesc(Entity::getId)
            .query()
    );

    return dataList;
}
```

---

## 聚合查询

### 分组统计

```java
// 单字段分组
var result = DB.use()
    .include(Entity::getType)
    .count(Entity::getId)
    .groupBy(Entity::getType)
    .query(Statistics.class);

// 多字段分组
var result = DB.use()
    .include(Entity::getType, Entity::getStatus)
    .count(Entity::getId)
    .groupBy(Entity::getType, Entity::getStatus)
    .query(GroupStatistics.class);
```

### 聚合函数

```java
// 求和
var result = DB.use()
    .include(Entity::getType)
    .sum(Entity::getAmount)
    .groupBy(Entity::getType)
    .query(SumStatistics.class);

// 平均值
var result = DB.use()
    .include(Entity::getType)
    .avg(Entity::getPrice)
    .groupBy(Entity::getType)
    .query(AvgStatistics.class);

// 最大值/最小值
var result = DB.use()
    .include(Entity::getType)
    .max(Entity::getPrice)
    .min(Entity::getPrice)
    .groupBy(Entity::getType)
    .query(PriceStatistics.class);
```

### 时间分组统计

```java
// 按日期分组统计
var option = new FunExOption(1, "yyyy-MM-dd");
var result = DB.use()
    .count(Entity::getId)
    .groupBy(Entity::getCreateTime, option)
    .include(Entity::getCreateTime, option)
    .query(DayStatistics.class);

// 自动填充时间序列
var map = C.DATA.toMap(
    DB.use()
        .count(Entity::getId)
        .groupBy(Entity::getCreateTime, option)
        .query(DayStatistics.class),
    x -> x.getCreateTime()
);

var times = C.TIME.timeQuantumByStep(startTime, endTime, stepSize);
return times.stream()
    .map(x -> new DayStatistics(x, map.getOrDefault(x, 0L)))
    .toList();
```

---

## 时序数据库

### ClickHouse时序数据操作

```java
// 声明ClickHouse DB
private static final MainDB<PointData> DB = new MainDB<>(PointData.class, "Clickhouse");

// 使用时序DB
DB.useTime().insert(pointData);

// 批量插入
DB.useTime().insert(pointDataList);
```

### 时间桶查询

```java
// 时间桶查询（按时间间隔聚合数据）
var result = DB.useTime().timeBucketQuery(
    PointData::getTime,      // 时间字段
    PointData::getValue,      // 值字段
    PointData::getPointId,    // 分组字段
    new Object[] { startTime, endTime },  // 时间范围
    pointId,                  // 指定点ID
    interval                  // 时间间隔（"1minute", "5minute", "1hour"等）
);
```

### 时序数据聚合查询

```java
// 多点聚合统计
var result = DB.useTime()
    .include(PointData::getPointId)
    .max(PointData::getValue)
    .min(PointData::getValue)
    .avg(PointData::getValue)
    .in(PointData::getPointId, pointIds)
    .between(PointData::getTime, startTime, endTime)
    .groupBy(PointData::getPointId)
    .query(PointStatistics.class);

// 时间范围查询
var list = DB.useTime()
    .eq(PointData::getPointId, pointId)
    .between(PointData::getTime, startTime, endTime)
    .orderBy(PointData::getTime)
    .query();
```

---

## 表管理

### 创建表

```java
@Override
public void init() {
    var db = DB.use();
    // 创建表
    db.createTable();
}
```

### 创建索引

```java
@Override
public void init() {
    var db = DB.use();

    // 创建BTREE索引
    db.createIndex(Index.BTREE, null, List.of(Entity::getCode));

    // 创建BRIN索引（适合时间序列）
    db.createIndex(Index.BRIN, null, List.of(Entity::getCreateTime));

    // 创建组合索引
    db.createIndex(Index.BTREE, "idx_name_status",
        List.of(Entity::getName, Entity::getStatus));
}
```

### 初始化数据

```java
@Override
public void init() {
    var db = DB.use();
    db.createTable();

    // 插入初始数据
    if (!db.exist()) {
        db.insert(List.of(
            new Entity("001", "默认数据1"),
            new Entity("002", "默认数据2")
        ));
    }
}
```

---

## 最佳实践

### 1. 条件跳过模式

```java
// 第三个参数控制条件是否生效
db.eq(Entity::getId, id, C.OBJECT.isEmpty(id))
db.in(Entity::getStatus, list, C.OBJECT.isEmpty(list))
```

### 2. 数据校验

```java
private void dataValidation(Entity entity, boolean update) {
    // 更新时ID必填
    if (update && C.OBJECT.isEmpty(entity.getId())) {
        throw new AppRuntimeException("ID必填");
    }

    // 名称重复校验
    if (DB.use()
        .notEq(Entity::getId, entity.getId(), !update)
        .eq(Entity::getName, entity.getName())
        .exist()) {
        throw new AppRuntimeException("名称重复");
    }
}
```

### 3. 关联查询

```java
// 主表查询
var mainList = mainDB.use().query();
var mainIds = mainList.stream().map(MainEntity::getId).toList();

// 关联表查询
var subList = subDB.use().in(SubEntity::getMainId, mainIds).query();

// 数据组装
var subMap = C.DATA.toMap(subList, SubEntity::getMainId);
mainList.forEach(main -> {
    main.setSubList(subMap.getOrDefault(main.getId(), List.of()));
});
```

### 4. 使用C.DATA注入

```java
C.DATA
    .inject(mainList, MainEntity::getId)
    .add(MainEntity::getSubList, ids ->
        subDB.use().in(SubEntity::getMainId, ids).query()
    )
    .join();
```

### 5. 事务处理

```java
// 删除主表和关联表数据
try {
    mainDB.use().eq(MainEntity::getId, id).delete();
    subDB.use().eq(SubEntity::getMainId, id).delete();
} catch (Exception e) {
    C.LOG.error("删除失败", e);
    throw new AppRuntimeException("删除失败");
}
```

---

## 事务管理

### 开启事务

```java
// 开启事务
Connection connection = DB.use().beginTransaction();

try {
    // 执行数据库操作
    DB.use().insert(entity);
    DB.use().update(anotherEntity);

    // 提交事务
    DB.use().commit();
} catch (Exception e) {
    // 回滚事务
    DB.use().rollback();
    throw new AppRuntimeException("操作失败", e);
} finally {
    // 关闭连接
    DB.use().close();
}
```

### 事务模板

```java
// 使用事务模板执行
DB.use().executeTransaction(() -> {
    // 在事务中执行操作
    DB.use().insert(entity1);
    DB.use().update(entity2);
    DB.use().delete(entity3);
});

// 带返回值的事务
Entity result = DB.use().executeTransaction(() -> {
    DB.use().insert(entity1);
    return DB.use().eq(Entity::getId, entity1.getId()).get();
});
```

### 跨表事务

```java
@Method(value = "删除订单", status = MethodStatus.COMPLETE)
public void deleteOrder(String orderId) {
    Connection connection = null;
    try {
        // 开启事务
        connection = DB.use().beginTransaction();

        // 1. 删除订单明细
        orderDetailDB.use()
            .eq(OrderDetail::getOrderId, orderId)
            .delete();

        // 2. 删除订单
        orderDB.use()
            .eq(Order::getId, orderId)
            .delete();

        // 3. 清除缓存
        C.CACHE.del("order:" + orderId);

        // 提交事务
        DB.use().commit();

    } catch (Exception e) {
        // 回滚事务
        DB.use().rollback();
        C.LOG.error("删除订单失败: {}", orderId, e);
        throw new AppRuntimeException("删除订单失败");
    } finally {
        // 关闭连接
        DB.use().close();
    }
}
```

### 事务隔离级别

```java
// 开启事务并指定隔离级别
Connection connection = DB.use().beginTransaction(
    Connection.TRANSACTION_READ_COMMITTED
);

// 常用隔离级别
// Connection.TRANSACTION_READ_UNCOMMITTED  - 读未提交
// Connection.TRANSACTION_READ_COMMITTED   - 读已提交（默认）
// Connection.TRANSACTION_REPEATABLE_READ  - 可重复读
// Connection.TRANSACTION_SERIALIZABLE     - 串行化
```

### 事务最佳实践

```java
// 1. 事务范围要小
// ❌ 不好：事务包含太多业务逻辑
DB.use().executeTransaction(() -> {
    DB.use().insert(entity);

    // 发送邮件（不应在事务中）
    C.MAIL.sendHtml(email, subject, content);

    // 调用外部服务（不应在事务中）
    var result = ClientStub.send("/service/api", context(), data);
});

// ✅ 好：事务只包含数据库操作
DB.use().executeTransaction(() -> {
    DB.use().insert(entity);
    DB.use().update(logEntity);
});

// 事务外执行其他操作
C.MAIL.sendHtml(email, subject, content);

// 2. 避免长事务
// ❌ 不好：循环中执行数据库操作
DB.use().executeTransaction(() -> {
    for (var item : largeList) {
        DB.use().insert(item);
    }
});

// ✅ 好：批量操作
DB.use().executeTransaction(() -> {
    DB.use().insert(largeList);
});

// 3. 正确处理异常
@Method(value = "转账", status = MethodStatus.COMPLETE)
public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
    try {
        DB.use().executeTransaction(() -> {
            // 扣款
            accountDB.use()
                .eq(Account::getId, fromAccount)
                .updateCalculate(Account::getBalance, "-" + amount)
                .update();

            // 加款
            accountDB.use()
                .eq(Account::getId, toAccount)
                .updateCalculate(Account::getBalance, "+" + amount)
                .update();
        });

    } catch (Exception e) {
        C.LOG.error("转账失败: {} -> {}", fromAccount, toAccount, e);
        throw new AppRuntimeException("转账失败");
    }
}
```

### 嵌套事务

```java
// Cloudless框架不支持真正的嵌套事务
// 如果在事务中调用另一个事务方法，内部事务会加入外部事务

@Method(value = "外部方法", status = MethodStatus.COMPLETE)
public void outerMethod() {
    DB.use().executeTransaction(() -> {
        DB.use().insert(entity1);

        // 调用另一个事务方法
        innerMethod();  // 会加入当前事务，不会开启新事务

        DB.use().insert(entity2);
    });
}

@Method(value = "内部方法", status = MethodStatus.COMPLETE)
public void innerMethod() {
    DB.use().executeTransaction(() -> {
        DB.use().insert(entity3);
    });
}
```

### 事务与锁

```java
// 使用SELECT ... FOR UPDATE悲观锁
@Method(value = "扣减库存", status = MethodStatus.COMPLETE)
public void reduceStock(String productId, int quantity) {
    DB.use().executeTransaction(() -> {
        // 查询并锁定记录
        Product product = productDB.use()
            .eq(Product::getId, productId)
            .forUpdate()  // 悲观锁
            .get();

        // 检查库存
        if (product.getStock() < quantity) {
            throw new AppRuntimeException("库存不足");
        }

        // 扣减库存
        productDB.use()
            .eq(Product::getId, productId)
            .updateCalculate(Product::getStock, "-" + quantity)
            .update();
    });
}

// 使用乐观锁（版本号）
@Method(value = "更新数据", status = MethodStatus.COMPLETE)
public void updateData(Entity entity) {
    int retries = 3;
    while (retries > 0) {
        try {
            // 获取当前数据
            Entity current = DB.use().eq(Entity::getId, entity.getId()).get();

            // 更新数据
            current.setName(entity.getName());
            current.setVersion(current.getVersion() + 1);

            // 使用版本号作为条件
            int rows = DB.use()
                .eq(Entity::getId, entity.getId())
                .eq(Entity::getVersion, current.getVersion() - 1)
                .update(current);

            if (rows == 0) {
                throw new AppRuntimeException("数据已被修改，请重试");
            }

            break;

        } catch (Exception e) {
            retries--;
            if (retries == 0) {
                throw new AppRuntimeException("更新失败，数据冲突");
            }
        }
    }
}
```

---

## 批量操作

### 批量插入

```java
// 批量插入（推荐）
List<Entity> list = List.of(
    new Entity("001", "测试1"),
    new Entity("002", "测试2"),
    new Entity("003", "测试3")
);
DB.use().insert(list);

// 分批插入（大数据量）
List<Entity> largeList = // ... 大量数据
int batchSize = 1000;
for (int i = 0; i < largeList.size(); i += batchSize) {
    int end = Math.min(i + batchSize, largeList.size());
    List<Entity> batch = largeList.subList(i, end);
    DB.use().insert(batch);
}
```

### 批量更新

```java
// 批量更新（使用事务）
@Method(value = "批量更新", status = MethodStatus.COMPLETE)
public void batchUpdate(List<Entity> list) {
    DB.use().executeTransaction(() -> {
        for (Entity entity : list) {
            DB.use().update(entity);
        }
    });
}
```

### 批量删除

```java
// 批量删除
List<String> ids = List.of("001", "002", "003");
DB.use().in(Entity::getId, ids).delete();

// 分批删除（大数据量）
List<String> largeIds = // ... 大量ID
int batchSize = 1000;
for (int i = 0; i < largeIds.size(); i += batchSize) {
    int end = Math.min(i + batchSize, largeIds.size());
    List<String> batch = largeIds.subList(i, end);
    DB.use().in(Entity::getId, batch).delete();
}
```

---

## 条件表达式

### CaseWhen条件

```java
// 使用CaseWhen实现条件查询
var result = DB.use()
    .include(
        DB.use().caseWhen()
            .when(Entity::getStatus, 1, "启用")
            .when(Entity::getStatus, 0, "禁用")
            .otherwise("未知")
            .as("statusText")
    )
    .include(Entity::getId, Entity::getName)
    .query();

// 多条件CaseWhen
var result = DB.use()
    .include(
        DB.use().caseWhen()
            .when(Entity::getType, 1, "类型1")
            .when(Entity::getType, 2, "类型2")
            .when(Entity::getType, 3, "类型3")
            .otherwise("其他")
            .as("typeName")
    )
    .query();

// CaseWhen与聚合结合
var result = DB.use()
    .include(Entity::getType)
    .count(
        DB.use().caseWhen()
            .when(Entity::getStatus, 1, 1)
            .otherwise((Object) null)
            .as("activeCount")
    )
    .groupBy(Entity::getType)
    .query();
```

---

## 统计结果类

```java
// 统计数据
@Data
public class StatisticsData {
    @Field(value = "名称/标签")
    private String name;

    @Field(value = "数量")
    private long count;
}

// 时间统计
@Data
public class TimeStatistics {
    @Field(value = "时间")
    private String time;

    @Field(value = "数量")
    private long count;
}

// 分组统计
@Data
public class GroupStatistics {
    @Field(value = "类型")
    private String type;

    @Field(value = "数量")
    private long count;

    @Field(value = "总金额")
    private double totalAmount;
}
```
