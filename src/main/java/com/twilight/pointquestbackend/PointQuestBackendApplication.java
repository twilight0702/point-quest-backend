package com.twilight.pointquestbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.twilight.pointquestbackend.mapper")
public class PointQuestBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointQuestBackendApplication.class, args);
    }

}
