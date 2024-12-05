package com.techconqueror.initializr.contributor;

import com.techconqueror.initializr.core.java.ClassMetadata;
import com.techconqueror.initializr.core.ntier.controller.rest.RestControllerGenerator;
import com.techconqueror.initializr.core.ntier.controller.rest.RestControllerRequirement;
import com.techconqueror.initializr.core.ntier.controller.rest.RestOperation;
import com.techconqueror.initializr.core.ntier.controller.rest.RestOperationName;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NTierFileProjectContributor implements ProjectContributor {

  private final ProjectDescription projectDescription;

  public NTierFileProjectContributor(ProjectDescription projectDescription) {
    this.projectDescription = projectDescription;
  }

  @Override
  public void contribute(Path projectRoot) {
    try {
      RestControllerGenerator
        .generateController(
          new RestControllerRequirement(
            projectDescription.getPackageName(),
            "reservation",
            Map
              .ofEntries(
                Map
                  .entry(
                    RestOperationName.GET_ALL,
                    new RestOperation(
                      RestOperationName.GET_ALL,
                      null,
                      new ClassMetadata(
                        projectDescription.getPackageName(),
                        "Reservation",
                        false,
                        List.of()
                      )
                    )
                  )
              ),
            projectRoot.resolve("src/main/java").toString()
          )
        );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
