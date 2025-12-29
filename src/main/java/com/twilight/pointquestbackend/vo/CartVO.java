package com.twilight.pointquestbackend.vo;

import java.util.List;
import lombok.Data;

@Data
public class CartVO {
    private List<CartItemVO> items;
    private Long totalPoints;
    private Integer totalQuantity;
}
