package com.twilight.pointquestbackend.vo.auth;

import lombok.Data;

@Data
public class AuthVO {
    private String token;

    public AuthVO() {
    }

    public AuthVO(String token) {
        this.token = token;
    }

}
