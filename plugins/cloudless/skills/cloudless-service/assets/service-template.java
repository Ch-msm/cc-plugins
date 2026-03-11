package com.szigc.xxx.service;

import java.util.List;
import java.util.Map;

import com.szigc.common.abs.AbstractService;
import com.szigc.common.annotation.Method;
import com.szigc.common.annotation.Parameter;
import com.szigc.common.annotation.ReturnData;
import com.szigc.common.annotation.Service;
import com.szigc.common.db.Index;
import com.szigc.common.db.MainDB;
import com.szigc.common.db.impl.DB;
import com.szigc.common.entity.base.DataList;
import com.szigc.common.entity.base.ExportItem;
import com.szigc.common.enumerate.MethodNature;
import com.szigc.common.enumerate.MethodStatus;
import com.szigc.common.exception.AppRuntimeException;
import com.szigc.common.util.C;
import com.szigc.xxx.entity.param.SaveYourParam;
import com.szigc.xxx.entity.param.YourSearch;
import com.szigc.xxx.entity.table.Your;
import com.szigc.xxx.entity.view.YourView;

/**
 * 服务模板。
 *
 * @author 梅思铭
 */
@Service(value = "示例服务", author = "梅思铭", date = "2026-03-09", module = "基础资料")
public class YourService extends AbstractService {

    private static final MainDB<Your> DB = new MainDB<>(Your.class);

    @Override
    public void init() {
        var db = DB.use();
        db.createTable();
        db.createIndex(Index.BTREE, null, List.of(Your::getId));
    }

    @Method(value = "分页查询", status = MethodStatus.COMPLETE)
    @ReturnData(type = YourView.class)
    public DataList<YourView> find(YourSearch search) {
        var dataList = new DataList<YourView>();
        var db = getSearchDB(search);
        if (search.getPageNo() != -1) {
            dataList.setTotal(db.count());
        }
        dataList.setList(
            db.paging(search.getPageNo(), search.getPageSize())
                .orderByDesc(Your::getId)
                .query()
                .stream()
                .map(this::toView)
                .toList()
        );
        return dataList;
    }

    @Method(value = "详情", status = MethodStatus.COMPLETE)
    public YourView get(@Parameter(value = "ID", required = true) String id) {
        return toView(DB.use().eq(Your::getId, id).get());
    }

    @Method(value = "新增", status = MethodStatus.COMPLETE)
    public void insert(SaveYourParam param) {
        var entity = toEntity(param);
        validate(entity, false);
        DB.use().insert(entity);
    }

    @Method(value = "更新", status = MethodStatus.COMPLETE)
    public void update(SaveYourParam param) {
        var entity = toEntity(param);
        validate(entity, true);
        DB.use().update(entity);
    }

    @Method(value = "删除", status = MethodStatus.COMPLETE)
    public void delete(@Parameter(value = "ID", required = true) String id) {
        DB.use().eq(Your::getId, id).delete();
    }

    @Method(value = "导出Excel", status = MethodStatus.COMPLETE)
    @ReturnData("文件ID")
    public String export(
        @Parameter(value = "查询条件") YourSearch search,
        @Parameter(value = "导出项", required = true) List<ExportItem> exportItems,
        @Parameter(value = "文件名", required = true) String fileName
    ) {
        var list = getSearchDB(search).query().stream().map(this::toView).toList();
        return com.szigc.common.util.File.EXECL.export(fileName, exportItems, list);
    }

    @Method(value = "导入新增", status = MethodStatus.COMPLETE)
    public void importInsert(@Parameter(value = "文件ID", required = true) String fileId) {
        var file = C.FILE.download(fileId);
        var rows = com.szigc.common.util.File.EXECL.reader(file, SaveYourParam.class);
        for (SaveYourParam row : rows) {
            insert(row);
        }
    }

    @Method(value = "公开健康检查", nature = MethodNature.PUBLIC, status = MethodStatus.COMPLETE, printLog = false)
    @ReturnData("结果")
    public String health() {
        return "ok";
    }

    @Method(value = "当前用户ID", nature = MethodNature.PROTECTED, status = MethodStatus.COMPLETE)
    @ReturnData("用户ID")
    public int currentUserId() {
        return context().getUserId();
    }

    @Method(value = "工具示例", status = MethodStatus.COMPLETE)
    @ReturnData("处理结果")
    public Map<String, Object> process(@Parameter(value = "关键字") String keyword) {
        var json = C.JSON.toJson(Map.of("keyword", keyword));
        return Map.of(
            "traceId", context().getTraceId(),
            "json", json,
            "now", C.TIME.toDateTime()
        );
    }

    private DB<Your> getSearchDB(YourSearch search) {
        return DB.use()
            .eq(Your::getId, search.getId(), C.OBJECT.isEmpty(search.getId()))
            .eq(Your::getStatus, search.getStatus(), search.getStatus() == null)
            .iLike(
                List.of(Your::getName),
                "%" + search.getKeyword() + "%",
                C.OBJECT.isEmpty(search.getKeyword())
            );
    }

    private Your toEntity(SaveYourParam param) {
        var entity = C.OBJECT.convert(Your.class, param);
        if (C.OBJECT.isEmpty(entity.getId())) {
            entity.setId(C.TEXT.longId());
        }
        if (param.getStatus() == null) {
            entity.setStatus(1);
        }
        return entity;
    }

    private YourView toView(Your entity) {
        if (entity == null) {
            return null;
        }
        var view = C.OBJECT.convert(YourView.class, entity);
        view.setStatusName(entity.getStatus() == 1 ? "启用" : "停用");
        return view;
    }

    private void validate(Your entity, boolean update) {
        if (update && C.OBJECT.isEmpty(entity.getId())) {
            throw new AppRuntimeException("ID必填");
        }

        // 这里按实际业务补充唯一性、状态、关联关系等校验。
    }
}
