package com.twilight.pointquestbackend.common;

public enum OrderStatus {
    CREATED,
    PROCESSING,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public static OrderStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        for (OrderStatus value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
