package com.szigc.xxx.entity.param;

import java.util.List;

import com.szigc.common.annotation.Entity;
import com.szigc.common.annotation.Field;
import com.szigc.xxx.entity.table.Your;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity("保存示例实体参数")
public class SaveYourParam extends Your {

    @Field("附件ID列表")
    private List<String> attachmentIds;
}
