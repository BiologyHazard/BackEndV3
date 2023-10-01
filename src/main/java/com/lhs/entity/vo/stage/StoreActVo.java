package com.lhs.entity.vo.stage;


import com.lhs.entity.dto.stage.ActTagArea;
import lombok.Data;

import java.util.List;

@Data
public class StoreActVo {
    private String actEndDate;

    private List<ActTagArea> actTagArea;

    private String actImgUrl;

    private Double actPPRBase;

    private Double actPPRStair;

    private String actName;

    private List<StoreItem> actStore;
}