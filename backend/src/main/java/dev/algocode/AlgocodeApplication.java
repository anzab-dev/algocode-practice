package dev.algocode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlgocodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgocodeApplication.class, args);
    }
}
