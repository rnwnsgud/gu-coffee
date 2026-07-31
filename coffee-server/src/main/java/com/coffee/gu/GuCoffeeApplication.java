package com.coffee.gu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.resilience.annotation.EnableResilientMethods;

@EnableResilientMethods
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = "com.coffee.gu")
public class GuCoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuCoffeeApplication.class, args);
    }
}
