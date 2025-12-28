package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class TaskSubmissionVO {
    private String submissionNo;
    private String status;
}
