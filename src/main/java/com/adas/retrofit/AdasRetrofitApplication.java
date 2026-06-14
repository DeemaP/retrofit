package com.adas.retrofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа приложения. Встроенный движок Camunda 7 поднимается стартером
 * camunda-bpm-spring-boot-starter-webapp и автоматически разворачивает BPMN из classpath.
 */
@SpringBootApplication
public class AdasRetrofitApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdasRetrofitApplication.class, args);
    }
}