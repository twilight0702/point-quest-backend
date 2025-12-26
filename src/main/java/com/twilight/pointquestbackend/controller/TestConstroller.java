package com.twilight.pointquestbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestConstroller {

    @GetMapping("/test")
    public String test(){
        return "test";
    }
}
