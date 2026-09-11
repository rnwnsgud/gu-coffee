package com.coffee.gu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods

@EnableResilientMethods
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["com.coffee.gu"])
class GuCoffeeApplication

fun main(args: Array<String>) {
    runApplication<GuCoffeeApplication>(*args)
}
