package com.twilight.pointquestbackend.vo;

import lombok.Data;

@Data
public class PoolDrawResultVO {
    private String poolNo;
    private Long rewardId;
    private String rewardNo;
    private String rewardName;
    private Long pointCost;
    private Long remainingPoints;
}
