package com.twilight.pointquestbackend.vo;

import lombok.Data;

@Data
public class OrderItemVO {
    private Long rewardId;
    private String rewardName;
    private Long pointCost;
    private Integer quantity;
}
