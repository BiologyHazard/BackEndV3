package com.lhs.entity.stage;



import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Data
@TableName("item")   //用于存储最终的等效理智/绿票价值
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Item {

    @TableId
    @Id

    private Long id;


    private String itemId;  //物品id


    private String itemName; //物品名称


    private Double itemValue; //物品价值 单位：绿票


    private Double itemValueAp; //物品价值 单位：理智


    private String  type; //物品稀有度


    private Integer rarity; //物品稀有度


    private Integer cardNum;  //前端排序的用索引


    private String version;  //经验书系数


    private Double weight;   //加工站爆率

}
