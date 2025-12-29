package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PoolDrawRecordVO {
    private String poolNo;
    private Long rewardId;
    private String rewardNo;
    private String rewardName;
    private Long pointCost;
    private LocalDateTime createdAt;
}
