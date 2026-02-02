# 文件与时间操作指南

## 目录

- [C.FILE - 文件操作](#cfile---文件操作)
- [C.TIME - 时间操作](#ctime---时间操作)

---

## C.FILE - 文件操作

文件读写、上传下载、Excel导入导出。

### 文件上传下载

```java
// 上传文件到文件服务器
FileId fileId = C.FILE.upload(file);

// 上传并指定是否为永久文件
FileResult result = C.FILE.upload(file, false);  // false=临时文件
String fileId = result.getId();

// 从文件服务器下载
File file = C.FILE.download(fileId);

// 根据fileId获取文件URL
String url = C.FILE.getFileUrl(fileId);
```

### 文件读写

```java
// 读取文件
String content = C.FILE.read("/path/to/file");

// 写入文件
C.FILE.write("/path/to/file", "content");

// 追加写入
C.FILE.append("/path/to/file", "content");

// 删除文件
C.FILE.delete("/path/to/file");

// 检查文件是否存在
boolean exists = C.FILE.exists("/path/to/file");

// 文件大小转人类可读格式
String size = C.FILE.humanReadableUnits(1234567);  // "1.234MB"
```

### Base64转换

```java
// 文件转Base64
String base64 = C.FILE.fileToBase64(file);

// Base64转文件
File file = C.FILE.base64ToFile(base64, "filename.jpg");
```

### YAML配置加载

```java
// 加载YAML配置
BuiltMenu config = C.FILE.loadYml("built-menu", BuiltMenu.class);

// 配置文件路径: resources/built-menu.yml
```

### Excel导出

```java
// 导出Excel（临时文件）
@Method(value = "导出", status = MethodStatus.COMPLETE)
@ReturnData("文件ID")
public String export(Search search, List<ExportItem> exportItems, String fileName) {
    // 查询数据
    var list = getSearchDB(search).query();

    // 导出Excel
    return File.EXECL.export(fileName, exportItems, list);
}

// 导出永久Excel文件
String fileId = File.EXECL.exportPermanent(fileName, exportItems, list);

// 导出项定义
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

// 使用示例
List<ExportItem> exportItems = List.of(
    new ExportItem("id", "ID", 100),
    new ExportItem("name", "名称", 200),
    new ExportItem("status", "状态", 100)
);
```

### Excel导入

```java
// 导入Excel
@Method(value = "导入新增", status = MethodStatus.COMPLETE)
public void importInsert(@Parameter(value = "文件ID", required = true) String fileId) {
    // 下载文件
    var file = C.FILE.download(fileId);

    // 读取Excel
    var list = File.EXECL.reader(file, Entity.class);

    // 批量插入
    list.forEach(this::insert);
}

// 导入更新
@Method(value = "导入更新", status = MethodStatus.COMPLETE)
public void importUpdate(String fileId) {
    var file = C.FILE.download(fileId);
    var list = File.EXECL.reader(file, Entity.class);
    list.forEach(this::update);
}
```

### Excel操作完整示例

```java
@Service(value = "数据管理", author = "梅思铭", date = "2025-01-08")
public class DataService extends AbstractService {

    @Method(value = "导出", status = MethodStatus.COMPLETE)
    @ReturnData("文件ID")
    public String export(
        @Parameter(value = "查询条件") Search search,
        @Parameter(value = "导出项") List<ExportItem> exportItems,
        @Parameter(value = "文件名") String fileName
    ) {
        // 查询数据
        var list = DB.use()
            .eq(Entity::getStatus, 1)
            .query(EntityV1.class);  // 使用视图类

        // 导出Excel
        return File.EXECL.export(fileName, exportItems, list);
    }

    @Method(value = "导出并发送邮件", status = MethodStatus.COMPLETE)
    public String exportAndSend(
        String email,
        Search search,
        List<ExportItem> exportItems
    ) {
        var list = DB.use().query();

        // 导出为永久文件
        var fileId = File.EXECL.exportPermanent("数据导出", exportItems, list);

        // 下载文件
        var file = C.FILE.download(fileId);

        // 发送邮件
        C.MAIL.sendHtml(email, "数据导出", "您的数据已导出，请查收附件", file);

        return fileId;
    }

    @Method(value = "导入", status = MethodStatus.COMPLETE)
    @ReturnData("导入结果")
    public String importData(
        @Parameter(value = "文件ID", required = true) String fileId
    ) {
        var file = C.FILE.download(fileId);
        var list = File.EXECL.reader(file, Entity.class);

        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        for (Entity entity : list) {
            try {
                dataValidation(entity, false);
                DB.use().insert(entity);
                success++;
            } catch (Exception e) {
                fail++;
                errors.add(entity.getName() + ": " + e.getMessage());
            }
        }

        return String.format("导入完成: 成功%d, 失败%d", success, fail);
    }
}
```

---

## C.TIME - 时间操作

时间处理、转换、验证和计算。

### 获取当前时间

```java
// 获取当前时间戳（毫秒）
long timestamp = C.TIME.localTimestamp();

// 获取当前日期时间
LocalDateTime dateTime = C.TIME.localDateTime();

// 获取当前日期
LocalDate date = C.TIME.localDate();

// 获取当前时间字符串
String dateStr = C.TIME.toDate();  // yyyy-MM-dd
String dateTimeStr = C.TIME.toDateTimeString();  // yyyy-MM-dd HH:mm:ss
```

### 时间格式化

```java
// 格式化时间
String formatted = C.TIME.format(localDateTime, "yyyy-MM-dd HH:mm:ss");

// 常用格式
String yyyyMMdd = C.TIME.format(localDateTime, "yyyy-MM-dd");
String HHmmss = C.TIME.format(localDateTime, "HH:mm:ss");
String chineseDate = C.TIME.format(localDateTime, "yyyy年MM月dd日 HH:mm:ss");
```

### 时间解析

```java
// 解析时间字符串
LocalDateTime dateTime = C.TIME.parse("2025-01-08 12:00:00", "yyyy-MM-dd HH:mm:ss");

// 解析日期
LocalDate date = C.TIME.parseDate("2025-01-08");

// 时间戳转日期时间
LocalDateTime dateTime = C.TIME.getLocalDateTime(timestamp);
```

### 时间验证

```java
// 验证时间范围（最多查询一年）
C.TIME.validYearTimestamp(startTime, endTime);

// 抛出异常如果范围超过一年
// AppRuntimeException: 时间范围不能超过一年
```

### 时间计算

```java
// 计算时间步长
String stepSize = C.TIME.stepSize(startTime, endTime);
// 返回: "年", "季度", "月", "周", "天", "小时", "分钟"

// 根据步长获取时间格式模式
String pattern = C.TIME.getPatternByStep(stepSize);
// stepSize="年" -> pattern="yyyy"
// stepSize="月" -> pattern="yyyy-MM"
// stepSize="天" -> pattern="yyyy-MM-dd"
// stepSize="小时" -> pattern="yyyy-MM-dd HH"
// stepSize="分钟" -> pattern="yyyy-MM-dd HH:mm"
```

### 时间区间生成

```java
// 生成时间区间列表
List<String> times = C.TIME.timeQuantumByStep(startTime, endTime, "天");
// 返回: ["2025-01-01", "2025-01-02", "2025-01-03", ...]

// 按月生成
List<String> months = C.TIME.timeQuantumByStep(startTime, endTime, "月");
// 返回: ["2025-01", "2025-02", "2025-03", ...]

// 按小时生成
List<String> hours = C.TIME.timeQuantumByStep(startTime, endTime, "小时");
// 返回: ["2025-01-08 00", "2025-01-08 01", "2025-01-08 02", ...]
```

### 年度时间

```java
// 获取年度结束时间
long endTime = C.TIME.getYearEndTime(2025);  // 2025年最后一刻的毫秒时间戳
```

### 时间操作完整示例

```java
@Service(value = "数据统计", author = "梅思铭", date = "2025-01-08")
public class StatisticsService extends AbstractService {

    @Method(value = "时间范围统计", status = MethodStatus.COMPLETE)
    @ReturnData("统计数据")
    public List<StatisticsData> timeRangeStatistics(
        @Parameter(value = "开始时间", required = true) long startTime,
        @Parameter(value = "结束时间", required = true) long endTime
    ) {
        // 验证时间范围
        C.TIME.validYearTimestamp(startTime, endTime);

        // 计算时间步长
        var stepSize = C.TIME.stepSize(startTime, endTime);
        var pattern = C.TIME.getPatternByStep(stepSize);

        // 按时间分组统计
        var option = new FunExOption(1, pattern);
        var map = C.DATA.toMap(
            DB.use()
                .count(Data::getId)
                .groupBy(Data::getCreateTime, option)
                .include(Data::getCreateTime, option)
                .query(TimeStatistics.class),
            x -> x.getTime()
        );

        // 生成完整时间区间并填充数据
        var times = C.TIME.timeQuantumByStep(startTime, endTime, stepSize);
        return times.stream()
            .map(t -> new StatisticsData(t, map.getOrDefault(t, 0L)))
            .toList();
    }

    @Method(value = "今日统计", status = MethodStatus.COMPLETE)
    public StatisticsData todayStatistics() {
        // 获取今天开始和结束时间
        var today = C.TIME.toDate();  // yyyy-MM-dd
        var start = C.TIME.toTimestamp(today + " 00:00:00");
        var end = C.TIME.toTimestamp(today + " 23:59:59");

        var count = DB.use()
            .between(Data::getCreateTime, start, end)
            .count();

        return new StatisticsData(today, count);
    }
}
```

### 时间常用工具方法

```java
// 转时间戳
long timestamp = C.TIME.toTimestamp("2025-01-08 12:00:00");

// 时间戳转字符串
String str = C.TIME.toDateTimeString(1704672000000L);

// 获取某天的开始时间
long dayStart = C.TIME.toTimestamp("2025-01-08 00:00:00");

// 获取某天的结束时间
long dayEnd = C.TIME.toTimestamp("2025-01-08 23:59:59");

// 判断是否同一天
boolean sameDay = C.TIME.isSameDay(timestamp1, timestamp2);
```

### 时间处理最佳实践

```java
// 1. 统一使用时间戳存储和传输
@Field(value = "创建时间")
private long createTime;

// 2. 查询时转换为字符串展示
@Data
public class EntityView {
    private String createTimeStr;  // 格式化的时间字符串

    public String getCreateTimeStr() {
        return C.TIME.toDateTimeString(createTime);
    }
}

// 3. 时间范围查询
@Method(value = "查询", status = MethodStatus.COMPLETE)
public DataList<Entity> find(Search search) {
    // 验证时间范围
    if (search.getStartTime() > 0 && search.getEndTime() > 0) {
        C.TIME.validYearTimestamp(search.getStartTime(), search.getEndTime());
    }

    var db = DB.use()
        .between(Entity::getCreateTime,
                 search.getStartTime(),
                 search.getEndTime(),
                 search.getStartTime() == 0 || search.getEndTime() == 0);

    // ...
}

// 4. 时间统计
var list = DB.use()
    .count(Entity::getId)
    .groupBy(Entity::getCreateTime, new FunExOption(1, "yyyy-MM-dd"))
    .query();

// 5. 时间序列填充
var times = C.TIME.timeQuantumByStep(startTime, endTime, "天");
var dataMap = list.stream()
    .collect(Collectors.toMap(
        x -> C.TIME.toDateTimeString(x.getTime()),
        TimeStatistics::getCount
    ));

return times.stream()
    .map(t -> new TimeStatistics(t, dataMap.getOrDefault(t, 0L)))
    .toList();
```

### 时间相关常量

```java
// 常用时间常量
public class TimeConstant {
    public static final long MINUTE = 60 * 1000L;           // 1分钟
    public static final long HOUR = 60 * MINUTE;            // 1小时
    public static final long DAY = 24 * HOUR;               // 1天
    public static final long WEEK = 7 * DAY;                // 1周
    public static final long MONTH = 30 * DAY;              // 1月（近似）
    public static final long YEAR = 365 * DAY;              // 1年（近似）
}

// 使用示例
var fiveMinutesAgo = C.TIME.localTimestamp() - (5 * TimeConstant.MINUTE);
var oneDayAgo = C.TIME.localTimestamp() - TimeConstant.DAY;
```
