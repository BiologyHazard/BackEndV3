package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyScore {
    private Long id;

    private Long uid;

    private String  charId;

    private Integer rarity;

    private Integer daily;

    private Integer rogue;

    private Integer securityService;

    private Integer hard;

    private Integer universal;

    private Integer counter;

    private Integer building;

    private Integer comprehensive;
}
