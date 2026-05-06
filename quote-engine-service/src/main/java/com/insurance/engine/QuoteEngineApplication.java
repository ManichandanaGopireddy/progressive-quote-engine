package com.insurance.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = "com.insurance")
@PropertySource("classpath:clue-rules.properties")
public class QuoteEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuoteEngineApplication.class, args);
    }
}