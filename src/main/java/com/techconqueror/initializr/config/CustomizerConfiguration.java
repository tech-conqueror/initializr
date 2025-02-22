package com.techconqueror.initializr.config;

import com.techconqueror.initializr.customizer.CustomProjectDescriptionCustomizer;
import io.spring.initializr.generator.project.ProjectDirectoryFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomizerConfiguration {

  @Bean
  public CustomProjectDescriptionCustomizer customProjectDescriptionCustomizer() {
    return new CustomProjectDescriptionCustomizer();
  }

  @Bean
  public ProjectDirectoryFactory projectDirectoryFactory() {
    return (description) -> {
      Path directoryPath = Paths.get("C:\\Users\\nmman\\Desktop\\dev\\temp");

      // Create the directory if it doesn't exist
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }

      // Return the absolute path as a string
      return Path.of(directoryPath.toAbsolutePath().toString());
    };
  }
}
