package com.szigc.xxx.entity.param;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import lombok.Data;

@Data
@Entity("示例查询条件")
public class YourSearch extends BaseSearch {

    @Field("ID")
    private String id;

    @Field("关键字")
    private String keyword;

    @Field("状态")
    private Integer status;
}
