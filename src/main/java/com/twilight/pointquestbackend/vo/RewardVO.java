package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Reward view object for admin endpoints.
 */
@Data
public class RewardVO {
    private Long id;
    private String name;
    private String rewardNo;
    private String description;
    private Long pointCost;
    private String status;
    private Integer stock;
    private List<Long> categoryIds;
    private List<String> categoryNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
