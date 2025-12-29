package com.twilight.pointquestbackend.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PointLedgerVO {
    private Long delta;
    private String refType;
    private Long refId;
    private String remark;
    private LocalDateTime createdAt;
}
