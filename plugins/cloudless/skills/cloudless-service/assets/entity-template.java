package com.szigc.xxx.entity.table;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import lombok.Data;

@Data
@Entity(value = "示例实体", logicDelete = true)
public class Your {

    @Field(value = "ID", primaryKey = true, sample = "1950000000000000000")
    private String id;

    @Field(value = "名称", required = true, length = 100, sample = "示例名称")
    private String name;

    @Field(value = "状态", defaultValue = "1", check = "0|1")
    private int status = 1;

    @Field(value = "备注", length = 500)
    private String remark;

    @Field("创建时间")
    private long createTime;

    @Field("更新时间")
    private long updateTime;
}
