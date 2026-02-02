package com.szigc.xxx.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.szigc.common.annotation.Method;
import com.szigc.common.annotation.Parameter;
import com.szigc.common.annotation.ReturnData;
import com.szigc.common.annotation.Service;
import com.szigc.common.abs.AbstractService;
import com.szigc.common.db.MainDB;
import com.szigc.common.db.Index;
import com.szigc.common.entity.base.DataList;
import com.szigc.common.enumerate.MethodNature;
import com.szigc.common.enumerate.MethodStatus;
import com.szigc.common.exception.AppRuntimeException;
import com.szigc.common.util.C;
import com.szigc.xxx.entity.table.YourEntity;
import com.szigc.xxx.entity.param.Search;

/**
 * 服务模板
 * 使用此模板创建新的服务类
 *
 * @author 梅思铭
 */
@Service(value = "服务名称", author = "梅思铭", date = "2025-01-08")
public class ServiceTemplate extends AbstractService {

    // 默认数据库
    private static final MainDB<YourEntity> DB = new MainDB<>(YourEntity.class);

    // ClickHouse时序数据库（如需要）
    // private static final MainDB<TimeEntity> TIME_DB = new MainDB<>(TimeEntity.class, "Clickhouse");

    @Override
    public void init() {
        var db = DB.use();
        // 创建表
        db.createTable();
        // 创建索引
        db.createIndex(Index.BTREE, null, List.of(YourEntity::getField));
        // db.createIndex(Index.BRIN, null, List.of(YourEntity::getCreateTime));

        // 初始化数据（如需要）
        // if (!db.exist()) {
        //     db.insert(List.of(
        //         new YourEntity("初始数据1"),
        //         new YourEntity("初始数据2")
        //     ));
        // }
    }

    /**
     * 公开接口示例 - 无需认证
     */
    @Method(value = "方法说明", nature = MethodNature.PUBLIC, status = MethodStatus.COMPLETE)
    @ReturnData("返回值说明")
    public String publicMethod() {
        C.LOG.info("执行公开方法");
        return "result";
    }

    /**
     * 受控接口示例 - 需要用户认证
     */
    @Method(value = "获取数据", status = MethodStatus.COMPLETE, nature = MethodNature.CONTROLLED)
    @ReturnData(type = YourEntity.class)
    public DataList<YourEntity> find(Search search) {
        var dataList = new DataList<YourEntity>();
        var db = getSearchDB(search);

        if (search.getPageNo() != -1) {
            dataList.setTotal(db.count());
        }

        dataList.setList(
            db.paging(search.getPageNo(), search.getPageSize())
                .orderByDesc(YourEntity::getId)
                .query()
        );
        return dataList;
    }

    /**
     * 新增
     */
    @Method(value = "新增", status = MethodStatus.COMPLETE)
    public void insert(YourEntity entity) {
        if (C.OBJECT.isEmpty(entity.getId())) {
            entity.setId(C.TEXT.longId());
        }
        dataValidation(entity, false);
        DB.use().insert(entity);
    }

    /**
     * 更新
     */
    @Method(value = "更新", status = MethodStatus.COMPLETE)
    public void update(YourEntity entity) {
        dataValidation(entity, true);
        DB.use().update(entity);
    }

    /**
     * 删除
     */
    @Method(value = "删除", status = MethodStatus.COMPLETE)
    public void delete(@Parameter(value = "ID", required = true) String id) {
        // 检查关联
        // if (otherDB.use().eq(OtherEntity::getEntityId, id).exist()) {
        //     throw new AppRuntimeException("数据已被使用，不能删除");
        // }
        DB.use().eq(YourEntity::getId, id).delete();
        // 清除缓存
        // C.CACHE.del("entity:" + id);
    }

    /**
     * 受保护接口示例 - 内部系统调用
     */
    @Method(value = "系统状态检查", status = MethodStatus.COMPLETE, nature = MethodNature.PROTECTED)
    @ReturnData("系统状态")
    public String checkSystem() {
        return C.TIME.localDateTime() + " 系统正常";
    }

    /**
     * 导出Excel
     */
    @Method(value = "导出", status = MethodStatus.COMPLETE)
    @ReturnData("文件ID")
    public String export(
        @Parameter(value = "查询条件") Search search,
        @Parameter(value = "导出项", required = true) List<ExportItem> exportItems,
        @Parameter(value = "文件名", required = true) String fileName
    ) {
        var list = getSearchDB(search).query();
        return com.szigc.common.util.File.EXECL.export(fileName, exportItems, list);
    }

    /**
     * 导入Excel
     */
    @Method(value = "导入新增", status = MethodStatus.COMPLETE)
    public void importInsert(
        @Parameter(value = "文件ID", required = true) String fileId
    ) {
        var file = C.FILE.download(fileId);
        var list = com.szigc.common.util.File.EXECL.reader(file, YourEntity.class);
        list.forEach(this::insert);
    }

    /**
     * 查询条件构建
     */
    private DB<YourEntity> getSearchDB(Search search) {
        return DB.use()
            .eq(YourEntity::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
            .in(YourEntity::getStatus, search.getStatus(), C.OBJECT.isEmpty(search.getStatus()))
            .between(YourEntity::getCreateTime, search.getStartTime(), search.getEndTime(),
                     C.OBJECT.isEmpty(search.getStartTime()) || C.OBJECT.isEmpty(search.getEndTime()))
            .iLike(List.of(YourEntity::getName, YourEntity::getCode),
                   "%" + search.getKeyword() + "%",
                   C.OBJECT.isEmpty(search.getKeyword()));
    }

    /**
     * 数据校验
     */
    private void dataValidation(YourEntity entity, boolean update) {
        if (update && C.OBJECT.isEmpty(entity.getId())) {
            throw new AppRuntimeException("ID必填");
        }

        // 名称重复校验
        if (DB.use()
            .notEq(YourEntity::getId, entity.getId(), !update)
            .eq(YourEntity::getName, entity.getName())
            .exist()) {
            throw new AppRuntimeException("名称重复");
        }

        // 其他业务校验...
    }

    /**
     * 使用工具类示例
     */
    @Method(value = "数据处理示例", status = MethodStatus.COMPLETE, nature = MethodNature.PUBLIC)
    @ReturnData("处理结果")
    public Map<String, Object> processData(@Parameter(value = "输入数据") String input) {

        // JSON操作
        var jsonObj = C.JSON.toJson(Map.of("input", input));
        var parsed = C.JSON.fromJson(jsonObj, Map.class);

        // 时间操作
        var now = C.TIME.localDateTime();
        var timestamp = C.TIME.localTimestamp();
        var dateStr = C.TIME.toDate();

        // 缓存操作
        C.CACHE.set("key", "value", 3600);
        var cached = C.CACHE.get("key");

        // 文件操作
        // File file = C.FILE.download(fileId);
        // String content = C.FILE.read(path);

        // 异步操作
        C.ASYNC.run(() -> {
            C.LOG.info("异步执行任务");
        });

        // 消息推送
        // C.PUSH.push("channel", "data", userId);

        // HTTP请求
        // var response = C.HTTP.get(url);

        return Map.of(
            "json", jsonObj,
            "time", now,
            "timestamp", timestamp,
            "date", dateStr,
            "cached", cached
        );
    }
}
