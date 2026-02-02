# 内部服务调用

Cloudless-common 提供的通用内部服务，可被各项目直接调用。

## 目录

- [PersonService - 人员管理](#personservice---人员管理)
- [SerialNumberService - 流水号管理](#serialnumberservice---流水号管理)
- [AttachmentService - 附件管理](#attachmentservice---附件管理)
- [ApprovalProcessService - 审批流程](#approvalprocessservice---审批流程)
- [TaskScheduleService - 任务调度](#taskscheduleservice---任务调度)

---

## PersonService - 人员管理

统一人员管理服务，用于管理关联数据的人员列表。

### 依赖实体

```java
@Data
@Entity("人员")
public class Person {
    @Field(value = "ID", primary = true)
    private String id;

    @Field(value = "关联数据ID")
    private String associateId;

    @Field(value = "关联数据类型")
    private String type;

    @Field(value = "用户ID")
    private Integer userId;

    @Field(value = "姓名")
    private String name;
}
```

### 使用示例

```java
// 替换人员数据（存在更新不存在插入）
PersonService.replace(List.of(
    new Person("data001", "task", 1001, "张三"),
    new Person("data001", "task", 1002, "李四")
));

// 删除人员
PersonService.delete("task", "data001");

// 获取人员列表
List<Person> persons = PersonService.get("task", "data001");

// 批量获取
Map<String, List<Person>> personMap = PersonService.get("task", List.of("data001", "data002"));

// 根据用户ID查找关联数据ID
List<String> associateIds = PersonService.findAssociateIds("task", List.of(1001, 1002));
```

---

## SerialNumberService - 流水号管理

统一流水号管理服务，提供线程安全的自增序列号。

### 依赖实体

```java
@Data
@Entity("流水号")
public class SerialNumber {
    @Field(value = "KEY")
    private String key;

    @Field(value = "数据类型")
    private String type;

    @Field(value = "序号")
    private Long number;
}
```

### 使用示例

```java
// 获取自增值（第一次返回1）
Long number1 = SerialNumberService.increment("order", "ORDER_NO");
Long number2 = SerialNumberService.increment("order", "ORDER_NO"); // 返回2

// 不指定类型
Long number = SerialNumberService.increment("BILL_NO");

// 设置值
SerialNumberService.setValue("order", "ORDER_NO", 1000L);
```

### 并发安全

内部使用 `ConcurrentHashMap` 实现分布式锁，确保并发安全。

---

## AttachmentService - 附件管理

统一附件管理服务，用于管理关联数据的附件列表。

### 依赖实体

```java
@Data
@Entity("附件")
public class Attachment {
    @Field(value = "ID", primary = true)
    private String id;

    @Field(value = "关联数据ID")
    private String associateId;

    @Field(value = "关联数据类型")
    private String type;

    @Field(value = "文件名")
    private String name;

    @Field(value = "文件大小")
    private Long size;
}
```

### 使用示例

```java
// 替换附件（先删除后插入）
AttachmentService.replace("task", "task001", List.of(
    new Attachment(fileId1, "文档1.pdf", 1024L),
    new Attachment(fileId2, "文档2.pdf", 2048L)
));

// 获取附件列表
List<Attachment> attachments = AttachmentService.get("task", "task001");

// 批量获取
Map<String, List<Attachment>> attachmentMap = AttachmentService.get(
    "task",
    List.of("task001", "task002")
);

// 删除附件
AttachmentService.delete("task", "task001");
AttachmentService.delete("task", List.of("task001", "task002"));
AttachmentService.delete("task"); // 删除该类型的所有附件
```

---

## ApprovalProcessService - 审批流程

统一审批流程管理服务。

### 依赖实体

```java
@Data
@Entity("审批流程")
public class ApprovalProcess {
    @Field(value = "ID", primary = true)
    private String id;

    @Field(value = "关联数据ID")
    private String associateId;

    @Field(value = "关联数据类型")
    private String type;

    @Field(value = "审批人")
    private UserBase approve;

    @Field(value = "抄送人")
    private List<UserBase> ccPerson;

    @Field(value = "下一审批人")
    private UserBase nextApprove;

    @Field(value = "审批意见")
    private String opinion;

    @Field(value = "状态")
    private Integer status;
}
```

### 使用示例

```java
// 添加审批流程
var process = new ApprovalProcess();
process.setType("purchase");
process.setAssociateId("PO001");
process.setApprove(new UserBase(1001, "张三"));
process.setCcPerson(List.of(
    new UserBase(1002, "李四"),
    new UserBase(1003, "王五")
));
process.setNextApprove(new UserBase(1004, "赵六"));
process.setOpinion("同意");
process.setStatus(1);
ApprovalProcessService.add(process);

// 查询审批流程
List<ApprovalProcess> processes = ApprovalProcessService.findApprovalProcess(
    "purchase",
    "PO001"
);

// 删除审批流程
ApprovalProcessService.delete("purchase", List.of("PO001", "PO002"));
```

---

## TaskScheduleService - 任务调度

任务调度服务，支持集群环境的定时任务和延迟任务。

### 依赖实体

```java
@Data
@Entity("任务调度")
public class TaskSchedule {
    @Field(value = "ID", primary = true)
    private String id;

    @Field(value = "关联数据类型")
    private String type;

    @Field(value = "任务类型")
    private Integer taskType; // 0:周期任务 1:延迟任务

    @Field(value = "Cron表达式")
    private String cron;

    @Field(value = "执行开始时间")
    private Long executeStartTime;

    @Field(value = "状态")
    private Integer status; // 0:未执行 1:执行中 2:执行完成 3:执行失败
}
```

### 使用示例

```java
// 初始化任务调度（服务启动时调用）
TaskScheduleService.init("report", taskSchedule -> {
    // 执行报表生成任务
    generateReport(taskSchedule);
});

// 添加周期任务（Cron表达式）
var cronTask = new TaskSchedule();
cronTask.setId("report001");
cronTask.setType("report");
cronTask.setTaskType(0); // 周期任务
cronTask.setCron("0 0 2 * * ?"); // 每天凌晨2点执行
TaskScheduleService.addSchedule(cronTask, task -> {
    generateReport(task);
});

// 添加延迟任务
var delayTask = new TaskSchedule();
delayTask.setId("report002");
delayTask.setType("report");
delayTask.setTaskType(1); // 延迟任务
delayTask.setExecuteStartTime(C.TIME.toTimestamp("2025-01-09 12:00:00"));
TaskScheduleService.addSchedule(delayTask, task -> {
    generateReport(task);
});

// 删除任务
TaskScheduleService.deleteTask("report", "report001");

// 获取任务
TaskSchedule task = TaskScheduleService.getTask("report", "report001");
List<TaskSchedule> tasks = TaskScheduleService.getTasks("report");
```

### 线程池配置

```java
// 自定义线程池
ThreadPoolExecutor POOL = new ThreadPoolExecutor(
    8,                                    // 核心线程数
    Runtime.getRuntime().availableProcessors() * 20, // 最大线程数
    30L,                                  // 空闲线程存活时间
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000)        // 任务队列
);
```

### 分布式锁

任务调度使用分布式锁确保集群环境下只有一个节点执行任务：

```java
// 自动获取分布式锁
if (C.CACHE.tryLock(lockKey, 0)) {
    try {
        // 执行任务
        executeTask(task);
    } finally {
        C.CACHE.delLock(lockKey);
    }
}
```

---

## 使用场景

### 1. 任务关联人员

```java
@Service(value = "任务管理", author = "梅思铭", date = "2025-01-08")
public class TaskService extends AbstractService {

    @Method(value = "添加任务", status = MethodStatus.COMPLETE)
    public void addTask(Task task, List<Integer> userIds) {
        // 1. 插入任务
        DB.use().insert(task);

        // 2. 保存任务人员
        var persons = userIds.stream()
            .map(userId -> new Person(task.getId(), "task", userId, getName(userId)))
            .toList();
        PersonService.replace(persons);
    }

    @Method(value = "获取任务人员", status = MethodStatus.COMPLETE)
    public List<Person> getTaskPersons(String taskId) {
        return PersonService.get("task", taskId);
    }
}
```

### 2. 生成唯一编号

```java
@Method(value = "生成订单编号", status = MethodStatus.COMPLETE)
public String generateOrderNo() {
    Long number = SerialNumberService.increment("ORDER_NO");
    return "ORD" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%06d", number);
}
```

### 3. 上传附件

```java
@Method(value = "上传附件", status = MethodStatus.COMPLETE)
public void uploadAttachments(String type, String associateId, List<String> fileIds) {
    var attachments = fileIds.stream()
        .map(fileId -> {
            var file = C.FILE.download(fileId);
            return new Attachment(fileId, file.getName(), file.length());
        })
        .toList();
    AttachmentService.replace(type, associateId, attachments);
}
```

### 4. 审批流程

```java
@Method(value = "提交审批", status = MethodStatus.COMPLETE)
public void submitApproval(String type, String associateId, Integer approverId) {
    var process = new ApprovalProcess();
    process.setType(type);
    process.setAssociateId(associateId);
    process.setApprove(getUser(approverId));
    process.setStatus(0); // 待审批
    ApprovalProcessService.add(process);
}

@Method(value = "查询审批历史", status = MethodStatus.COMPLETE)
public List<ApprovalProcess> getApprovalHistory(String type, String associateId) {
    return ApprovalProcessService.findApprovalProcess(type, associateId);
}
```

---

## 服务特点

| 服务 | 特点 | 使用场景 |
|------|------|----------|
| PersonService | 并发安全、批量操作 | 任务人员、会议参与人等 |
| SerialNumberService | 自增序列、线程安全 | 订单编号、流水号等 |
| AttachmentService | 先删后插、类型隔离 | 各类附件管理 |
| ApprovalProcessService | JSON字段自动转换 | 审批流程、意见记录 |
| TaskScheduleService | 分布式锁、Cron支持 | 定时任务、延迟任务 |
