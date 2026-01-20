package com.yatrika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class YatrikaApplication {
	public static void main(String[] args) {
		SpringApplication.run(YatrikaApplication.class, args);
        System.out.println("YATRIKA Backend is running");
        System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("API Base URL: http://localhost:8080/api");
	}
}
