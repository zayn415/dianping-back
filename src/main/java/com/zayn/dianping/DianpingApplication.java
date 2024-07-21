package com.zayn.dianping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zayn.dianping.mapper")
public class DianpingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DianpingApplication.class, args);
    }
    
}
