package com.techconqueror.initializr.contributor;

import io.spring.initializr.generator.project.contributor.ProjectContributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CodeStyleFileProjectContributor implements ProjectContributor {

  private static final String CODE_STYLE_FILE = "code-style.xml";

  @Override
  public void contribute(Path projectRoot) throws IOException {
    // Define the source file path (relative to where this code runs)
    Path sourceFile = Path.of(CODE_STYLE_FILE);

    // Define the destination file path in the project root
    Path destinationFile = projectRoot.resolve(CODE_STYLE_FILE);

    // Copy the file to the project root
    Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
  }
}
