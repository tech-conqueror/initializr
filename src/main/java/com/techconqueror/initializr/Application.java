package com.techconqueror.initializr;

import io.spring.initializr.generator.project.ProjectGenerationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
  excludeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION, value = ProjectGenerationConfiguration.class),
  })
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
