# 内部服务

`common` 提供了一组静态内部服务，适合直接在业务服务中调用。

## `PersonService`

适合维护“关联数据 -> 人员列表”。

替换人员：

```java
List<Person> persons = List.of(
    new Person(1001, "张三", 10L, "研发部", taskId, "task"),
    new Person(1002, "李四", 10L, "研发部", taskId, "task")
);
PersonService.replace(persons);
```

查询和删除：

```java
List<Person> list = PersonService.get("task", taskId);
Map<String, List<Person>> map = PersonService.get("task", List.of(taskId1, taskId2));
List<String> associateIds = PersonService.findAssociateIds("task", List.of(1001, 1002));

PersonService.delete("task", taskId);
```

## `SerialNumberService`

线程安全自增序号：

```java
Long no1 = SerialNumberService.increment("order", "ORDER_NO");
Long no2 = SerialNumberService.increment("order", "ORDER_NO");

Long billNo = SerialNumberService.increment("BILL_NO");

SerialNumberService.setValue("order", "ORDER_NO", 1000L);
```

## `AttachmentService`

统一管理关联附件。

```java
AttachmentService.replace("task", taskId, List.of(
    new Attachment(fileId1, "方案.pdf", 1024L, "方案附件", null, null),
    new Attachment(fileId2, "清单.xlsx", 2048L, "明细附件", null, null)
));

List<Attachment> attachments = AttachmentService.get("task", taskId);
Map<String, List<Attachment>> map = AttachmentService.get("task", List.of(taskId1, taskId2));

AttachmentService.delete("task", taskId);
AttachmentService.delete("task", List.of(taskId1, taskId2));
AttachmentService.delete("task");
```

说明：

- `replace` 会先删后插
- 调用前要准备好附件的文件 ID、名称、大小

## `ApprovalProcessService`

维护审批记录。

新增：

```java
ApprovalProcess process = new ApprovalProcess();
process.setType("purchase");
process.setAssociateId(orderId);
process.setApprovalType("提交");
process.setOpinion("提交审批");
process.setApprove(new UserBase(1001, "张三", "", 0, ""));
process.setCcPerson(List.of(new UserBase(1002, "李四", "", 0, "")));

ApprovalProcessService.add(process);
```

查询和删除：

```java
List<ApprovalProcess> list = ApprovalProcessService.findApprovalProcess("purchase", orderId);
ApprovalProcessService.delete("purchase", List.of(orderId1, orderId2));
```

## `TaskScheduleService`

集群任务调度，支持周期任务和延迟任务。

初始化：

```java
TaskScheduleService.init("report", task -> {
    C.LOG.info("执行任务: {}", task.getId());
    return null;
});
```

新增任务：

```java
TaskSchedule cronTask = new TaskSchedule();
cronTask.setId("report-daily");
cronTask.setType("report");
cronTask.setTaskType(0);
cronTask.setCron("0 0 1 * * *");
cronTask.setExecuteStartTime(C.TIME.localTimestamp());

TaskScheduleService.addSchedule(cronTask, task -> {
    generateReport(task.getId());
    return null;
});
```

延迟任务：

```java
TaskSchedule delayTask = new TaskSchedule();
delayTask.setId("report-once");
delayTask.setType("report");
delayTask.setTaskType(1);
delayTask.setExecuteStartTime(C.TIME.localTimestamp() + 60_000);

TaskScheduleService.addSchedule(delayTask, task -> {
    generateReport(task.getId());
    return null;
});
```

查询和删除：

```java
TaskSchedule task = TaskScheduleService.getTask("report", "report-daily");
List<TaskSchedule> tasks = TaskScheduleService.getTasks("report");
TaskScheduleService.deleteTask("report", "report-daily");
```

## 推荐做法

1. 关联人员用 `PersonService`
2. 附件关系用 `AttachmentService`
3. 可并发自增编号用 `SerialNumberService`
4. 审批轨迹用 `ApprovalProcessService`
5. 延迟任务和周期任务用 `TaskScheduleService`
