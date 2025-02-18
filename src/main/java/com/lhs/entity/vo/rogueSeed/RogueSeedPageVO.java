package com.lhs.entity.vo.rogueSeed;

import lombok.Data;

import java.util.List;

@Data
public class RogueSeedPageVO {
    private Long seedId;
    private String seed;
    private String rogueVersion;
    private Double rating;
    private Integer ratingPerson;
    private Integer difficulty;
    private String rogueTheme;
    private String squad;
    private List<String> operatorTeam;
    private String description;
    private List<String> tags;
    private String summaryImageLink;
    private Long createTime;
}
