package com.lhs.entity.vo.item;

import lombok.Data;

import java.util.List;

@Data
public class RecommendedStageVO {
    private String itemType;
    private String itemTypeId;
    private String itemSeries;
    private String itemSeriesId;
    private String version;
    private List<StageResultVOV2> stageResultList;
}


