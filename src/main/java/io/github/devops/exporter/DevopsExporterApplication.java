package io.github.devops.exporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class DevopsExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevopsExporterApplication.class, args);
    }

}
