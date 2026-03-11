# 数据库操作

## `MainDB`

普通数据库：

```java
private static final MainDB<User> DB = new MainDB<>(User.class);
```

时序数据库：

```java
private static final MainDB<PointData> TIME_DB = new MainDB<>(PointData.class, "Clickhouse");
```

入口：

- `DB.use()`：普通数据库
- `TIME_DB.useTime()`：时序数据库

## 初始化表

```java
@Override
public void init() {
    var db = DB.use();
    db.createTable();
    db.createIndex(Index.BTREE, null, List.of(User::getId));
}
```

索引类型：

- `Index.UNIQUE`
- `Index.BTREE`
- `Index.BRIN`
- `Index.HASH`

## 查询

```java
User entity = DB.use().eq(User::getId, id).get();

List<User> list = DB.use().query();

List<UserView> views = DB.use().query(UserView.class);

long count = DB.use().count();

boolean exist = DB.use().eq(User::getName, name).exist();
```

查询单列：

```java
String name = DB.use()
    .include(User::getName)
    .eq(User::getId, id)
    .get(String.class);
```

## 插入

```java
String id = DB.use().insert(entity);

DB.use().insert(List.of(entity1, entity2));
```

说明：

- 单条插入返回主键 ID
- 批量插入无返回值

## 更新

整对象更新：

```java
DB.use().update(entity);
DB.use().update(entity, true); // ignoreEmpty = true
```

按字段更新：

```java
DB.use()
    .eq(User::getId, id)
    .update(User::getName, "新名称");

DB.use()
    .eq(User::getId, id)
    .update(User::getName, "新名称", User::getStatus, 1);
```

按 Map 更新：

```java
Map<FunEx<User, ?>, Object> data = new HashMap<>();
data.put(User::getName, "新名称");
data.put(User::getStatus, 1);

DB.use().eq(User::getId, id).update(data);
```

计算更新：

```java
DB.use()
    .eq(Account::getId, id)
    .updateCalculate(Account::getBalance, "-100")
    .update();
```

## 删除

```java
DB.use().eq(User::getId, id).delete();
DB.use().in(User::getId, ids).delete();
```

说明：

- 逻辑删除表会更新删除标记
- 物理删除表不允许无条件全表删除

## 条件构建

```java
var db = DB.use()
    .eq(User::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
    .notEq(User::getStatus, 0)
    .in(User::getDepartmentId, departmentIds, C.OBJECT.isEmpty(departmentIds))
    .notIn(User::getStatus, List.of(2, 3))
    .between(User::getCreateTime, startTime, endTime,
        C.OBJECT.isEmpty(startTime) || C.OBJECT.isEmpty(endTime))
    .gt(User::getCreateTime, startTime, C.OBJECT.isEmpty(startTime))
    .lt(User::getCreateTime, endTime, C.OBJECT.isEmpty(endTime))
    .iLike(List.of(User::getName, User::getPhone),
        "%" + search.getKeyword() + "%",
        C.OBJECT.isEmpty(search.getKeyword()))
    .contain(User::getTags, List.of("重点", "公开"), C.OBJECT.isEmpty(search.getTags()));
```

## 排序、分页、去重

```java
var list = DB.use()
    .eq(User::getStatus, 1)
    .orderByDesc(User::getCreateTime)
    .paging(pageNo, pageSize)
    .query();

var distinctList = DB.use()
    .distinct()
    .include(User::getDepartmentId)
    .query(String.class);
```

## 分组与聚合

```java
var stat = DB.use()
    .sum(Order::getAmount)
    .count(Order::getId)
    .groupBy(Order::getDepartmentId)
    .query(OrderStatView.class);
```

时间字段格式化分组：

```java
var option = new FunExOption(1, "yyyy-MM-dd");
var stat = DB.use()
    .count(LogEntity::getId)
    .groupBy(LogEntity::getCreateTime, option)
    .query(LogStatView.class);
```

## 事务

```java
DB.use().transaction(db -> {
    db.insert(order);
    stockDb.use()
        .eq(Product::getId, order.getProductId())
        .updateCalculate(Product::getStock, "-1")
        .update();
});
```

补充：

- 手动事务可用 `beginTransaction()` / `commit()` / `rollback()`
- `noTransaction()` 可在事务环境中强制绕过当前事务连接

## 时序数据库

写入：

```java
TIME_DB.useTime().insert(pointData);
TIME_DB.useTime().insert(pointDataList);
```

查询：

```java
var list = TIME_DB.useTime()
    .eq(PointData::getPointId, pointId)
    .between(PointData::getTime, startTime, endTime)
    .orderBy(PointData::getTime)
    .query();
```

时间桶查询：

```java
var result = TIME_DB.useTime().timeBucketQuery(
    PointData::getTime,
    PointData::getValue,
    PointData::getPointId,
    new Object[]{startTime, endTime},
    pointId,
    60
);
```

## 推荐做法

1. 服务中统一声明 `private static final MainDB<...> DB`
2. 搜索条件提取到 `getSearchDB(...)`
3. 时间范围查询先用 `C.TIME.validYearTimestamp`
4. 多步写操作用事务模板
5. 时序数据才使用 `useTime()`
