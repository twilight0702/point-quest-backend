package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminSubmissionDetailVO extends AdminSubmissionVO {
    private Long userId;
    private List<String> evidencePreviewUrls;
    private Long pointsAwarded;
    private String reviewComment;
}
