package com.szigc.xxx.entity.param;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import lombok.Data;

@Data
@Entity("基础查询条件")
public class BaseSearch {

    @Field(value = "页码", defaultValue = "1")
    private int pageNo = 1;

    @Field(value = "每页条数", defaultValue = "20")
    private int pageSize = 20;
}
