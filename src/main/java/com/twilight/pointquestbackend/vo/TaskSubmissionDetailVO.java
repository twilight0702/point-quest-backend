package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskSubmissionDetailVO {
    private String submissionNo;
    private String status;
    private List<String> evidencePreviewUrls;
    private String taskNo;
}
