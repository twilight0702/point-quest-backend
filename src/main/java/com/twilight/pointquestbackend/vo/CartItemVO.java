package com.twilight.pointquestbackend.vo;

import lombok.Data;

@Data
public class CartItemVO {
    private Long rewardId;
    private String rewardNo;
    private String name;
    private Long pointCost;
    private Integer quantity;
    private Integer stock;
    private String status;
    private Boolean available;
    private Long linePoints;
}
