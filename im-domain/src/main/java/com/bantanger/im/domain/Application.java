package com.bantanger.im.domain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = {"com.bantanger.im.service", "com.bantanger.im.domain"})
@MapperScan("com.bantanger.im.domain.*.dao.mapper")
@ComponentScan(
    basePackages = "com.bantanger.im",
    excludeFilters = {
        @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.bantanger\\.im\\.service\\..*\\.extensionpost\\.impl\\..*"
        )
    }
)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}