package com.lhs.entity.po.stage;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName("stage_result_sample")
public class StageResultSample {
    @TableId
    private Long id;
    private String stageId;
    private String stageCode;
    private Integer stageType;
    private String itemType;
    private String itemTypeId;
    private String mainItemId;
    private String secondaryItemId;
    private Double stageEfficiency;
    @TableField(value = "le_t5_efficiency")
    private Double leT5Efficiency;
    @TableField(value = "le_t4_efficiency")
    private Double leT4Efficiency;
    @TableField(value = "le_t3_efficiency")
    private Double leT3Efficiency;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private String version;

}
