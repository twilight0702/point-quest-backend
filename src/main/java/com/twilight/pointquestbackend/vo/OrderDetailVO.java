package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class OrderDetailVO {
    private String orderNo;
    private Long totalPoints;
    private String status;
    private LocalDateTime createdAt;
    private String address;
    private List<OrderItemVO> items;
}
