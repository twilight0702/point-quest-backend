package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderSummaryVO {
    private String orderNo;
    private Long totalPoints;
    private String status;
    private LocalDateTime createdAt;
    private Integer itemCount;
}
