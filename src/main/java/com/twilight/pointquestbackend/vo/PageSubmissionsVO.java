package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PageSubmissionsVO {
    private Long total;
    private Long size;
    private Long current;
    private Long pages;

    private List<TaskSubmissionSummaryVO> records;

    @Data
    public static class TaskSubmissionSummaryVO{
        private String submissionNo;
        private String taskNo;
        private String taskTitle;
        private Integer pointReward;
        private String status;
        private LocalDateTime createdAt;
    }
}
