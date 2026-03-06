package com.krushikranti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KrushiKrantiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KrushiKrantiApplication.class, args);
    }
}
