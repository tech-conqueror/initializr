package com.techconqueror.initializr.customizer;

import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectDescriptionCustomizer;

public class CustomProjectDescriptionCustomizer implements ProjectDescriptionCustomizer {

  @Override
  public void customize(MutableProjectDescription description) {
    description.setApplicationName("Application");
  }
}
