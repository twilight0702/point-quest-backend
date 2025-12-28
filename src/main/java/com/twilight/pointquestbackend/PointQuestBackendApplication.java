package com.twilight.pointquestbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.twilight.pointquestbackend.mapper")
public class PointQuestBackendApplication {
    // JVM 时区配置
    static {
        System.setProperty("user.timezone", "Asia/Shanghai");
        System.setProperty("timezone", "Asia/Shanghai");
        System.setProperty("java.util.timezone", "Asia/Shanghai");
        System.setProperty("user.country", "CN");
    }

    public static void main(String[] args) {
        SpringApplication.run(PointQuestBackendApplication.class, args);
    }

}
