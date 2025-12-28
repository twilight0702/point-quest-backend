package com.twilight.pointquestbackend.common;

public enum TaskSubmissionStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String name;

    TaskSubmissionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
