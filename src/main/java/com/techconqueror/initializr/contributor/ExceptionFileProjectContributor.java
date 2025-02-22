package com.techconqueror.initializr.contributor;

import static com.techconqueror.initializr.core.common.ExceptionGenerator.generateResourceNotFoundException;

import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import java.io.IOException;
import java.nio.file.Path;

public class ExceptionFileProjectContributor implements ProjectContributor {

  private final ProjectDescription projectDescription;

  public ExceptionFileProjectContributor(ProjectDescription projectDescription) {
    this.projectDescription = projectDescription;
  }

  @Override
  public void contribute(Path projectRoot) throws IOException {
    generateResourceNotFoundException(
      projectDescription.getPackageName() + ".common.exception", projectRoot.resolve("src/main/java").toString()
    );
  }
}
