package com.szigc.xxx.entity.view;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import com.szigc.xxx.entity.table.Your;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity("示例视图")
public class YourView extends Your {

    @Field("状态名称")
    private String statusName;
}
