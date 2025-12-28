package com.twilight.pointquestbackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class UploadEvidenceVO {
    private String submissionNo;
    private List<EvidenceResourceVO> resources;
}
