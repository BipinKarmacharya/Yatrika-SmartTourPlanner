package com.yatrika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties
public class YatrikaApplication {
	public static void main(String[] args) {
		SpringApplication.run(YatrikaApplication.class, args);
        System.out.println("YATRIKA Backend is running");
        System.out.println("Swagger UI: http://localhost:8080/api/swagger-ui.html");
        System.out.println("API Base URL: http://localhost:8080/api");
	}
}
