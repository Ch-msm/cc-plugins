# 文件与时间

## `C.FILE`

真实常用方法：

```java
FileInfo tempFile = C.FILE.upload(file);
FileInfo permanentFile = C.FILE.upload(file, false);
List<FileInfo> files = C.FILE.upload(List.of(file1, file2), true, "report");

java.io.File localFile = C.FILE.download(fileId);
String url = C.FILE.getFileUrl(fileId);
boolean exist = C.FILE.exist(localFile);
```

说明：

- `upload(file)` 等价于 `upload(file, true)`，默认上传临时文件
- `upload(file, false)` 才是永久文件
- 返回值是 `FileInfo`，不是字符串 ID

取文件 ID：

```java
String fileId = C.FILE.upload(file).getId();
```

## 本地文件处理

`C.FILE` 不提供 `read/write/append/delete/exists` 这类文本文件快捷方法。

如果要读写本地文件，请使用：

- Java NIO
- Hutool `FileUtil`
- Apache Commons IO

## Excel

入口在 `com.szigc.common.util.File.EXECL`：

```java
String tempFileId = com.szigc.common.util.File.EXECL.export(fileName, exportItems, list);
String permanentFileId = com.szigc.common.util.File.EXECL.exportPermanent(fileName, exportItems, list);

var file = C.FILE.download(fileId);
List<ImportRow> rows = com.szigc.common.util.File.EXECL.reader(file, ImportRow.class);
```

导出示例：

```java
@Method(value = "导出", status = MethodStatus.COMPLETE)
@ReturnData("文件ID")
public String export(
    @Parameter(value = "查询条件") UserSearch search,
    @Parameter(value = "导出项", required = true) List<ExportItem> exportItems,
    @Parameter(value = "文件名", required = true) String fileName
) {
    var list = DB.use().query();
    return com.szigc.common.util.File.EXECL.export(fileName, exportItems, list);
}
```

导入示例：

```java
@Method(value = "导入新增", status = MethodStatus.COMPLETE)
public void importInsert(@Parameter(value = "文件ID", required = true) String fileId) {
    var file = C.FILE.download(fileId);
    var rows = com.szigc.common.util.File.EXECL.reader(file, UserImportRow.class);
    for (UserImportRow row : rows) {
        var param = C.OBJECT.convert(SaveUserParam.class, row);
        insert(param);
    }
}
```

## 其他文件能力

`com.szigc.common.util.File` 还提供：

- `CSV`
- `ZIP`
- `XML`
- `WORD`

如果需要这些能力，优先查对应源码实现，再补充业务封装。

## `C.TIME`

常用方法：

```java
long now = C.TIME.localTimestamp();
String today = C.TIME.toDate();
String nowText = C.TIME.toDateTime();
String date = C.TIME.toDateString(now);
String dateTime = C.TIME.toDateTimeString(now);

long ts1 = C.TIME.toTimestamp("2026-03-09 08:00:00");
long ts2 = C.TIME.toTimestamp(LocalDateTime.now());
```

范围计算：

```java
var range = C.TIME.getRange("本月", 0, 0);
long begin = range.begin();
long over = range.over();
```

合法性校验：

```java
C.TIME.validYearTimestamp(startTime, endTime);
```

统计步长：

```java
String step = C.TIME.stepSize(startTime, endTime);
List<Long> buckets = C.TIME.timeQuantumByStep(startTime, endTime, step);
```

## 推荐做法

1. 文件上传后立即取 `FileInfo.getId()`
2. 导出接口返回文件 ID，不直接返回二进制
3. 时间范围查询先用 `validYearTimestamp`
4. 本地文本文件读写不要假设 `C.FILE` 有快捷方法
