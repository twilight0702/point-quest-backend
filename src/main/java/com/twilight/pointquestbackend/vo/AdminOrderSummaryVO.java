package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminOrderSummaryVO {
    private String orderNo;
    private Long userId;
    private Long totalPoints;
    private String status;
    private LocalDateTime createdAt;
    private Integer itemCount;
}
