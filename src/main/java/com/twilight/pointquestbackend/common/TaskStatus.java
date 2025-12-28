package com.twilight.pointquestbackend.common;

import lombok.Getter;

@Getter
public enum TaskStatus {
    OPEN("OPEN"),
    CLOSED("CLOSE"),
    CANCELED("CANCELED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String name;

    TaskStatus(String name) {
        this.name = name;
    }

    public static TaskStatus fromString(String status) {
        for (TaskStatus value : values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }
}
