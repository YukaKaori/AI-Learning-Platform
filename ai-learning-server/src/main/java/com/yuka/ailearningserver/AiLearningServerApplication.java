package com.yuka.ailearningserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiLearningServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearningServerApplication.class, args);
    }

}
