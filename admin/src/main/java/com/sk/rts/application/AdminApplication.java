package com.sk.rts.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class AdminApplication {

    static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
