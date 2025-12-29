package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Prize pool view object.
 */
@Data
public class PoolVO {
    private Long id;
    private String poolNo;
    private String title;
    private String description;
    private Long pointCost;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PoolItemVO> items;

    @Data
    public static class PoolItemVO {
        private Long rewardId;
        private String rewardName;
        private String rewardNo;
        private Long pointCost;
        private String rewardStatus;
        private Integer sortNo;
        private Long weight;
    }
}
