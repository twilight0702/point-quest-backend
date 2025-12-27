package com.twilight.pointquestbackend.controller;

import com.twilight.pointquestbackend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestConstroller {

    @GetMapping("/test")
    public ApiResponse<String> test(){
        return ApiResponse.success("test");
    }
}
