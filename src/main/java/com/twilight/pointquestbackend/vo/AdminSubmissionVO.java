package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSubmissionVO {
    private String submissionNo;
    private String taskNo;
    private String taskTitle;
    private String username;
    private String userEmail;
    private Long pointReward;
    private String status;
    private String evidenceText;
    private LocalDateTime createdAt;
}
