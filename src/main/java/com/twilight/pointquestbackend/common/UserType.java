package com.twilight.pointquestbackend.common;

import lombok.Getter;

/**
 * 用户身份类型枚举
 */
@Getter
public enum UserType {
    USER("USER"),
    ADMIN("ADMIN");

    private final String name;

    UserType(String name) {
        this.name = name;
    }

    public static UserType fromString(String name) {
        for (UserType value : UserType.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid AuthType: " + name);
    }
}
