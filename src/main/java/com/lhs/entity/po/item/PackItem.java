package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
@TableName
public class PackItem {
    @Id
    @TableId
    private String id;
    private Integer zoneIndex;
    private String name;
    private Double value;


}
