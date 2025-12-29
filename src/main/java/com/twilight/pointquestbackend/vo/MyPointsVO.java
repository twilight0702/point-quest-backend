package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MyPointsVO {
    private Long balance;
    private LocalDateTime updatedAt;
}
