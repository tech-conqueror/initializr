package com.techconqueror.initializr.contributor;

import com.techconqueror.initializr.core.nlayer.repository.entity.EntityGenerator;
import com.techconqueror.initializr.core.nlayer.controller.rest.dto.Dto;
import com.techconqueror.initializr.core.nlayer.controller.rest.dto.DtoType;
import com.techconqueror.initializr.core.nlayer.controller.rest.*;
import com.techconqueror.initializr.core.nlayer.controller.rest.dto.DtoGenerator;
import com.techconqueror.initializr.core.nlayer.service.ServiceGenerator;
import com.techconqueror.initializr.core.nlayer.service.mapper.MapperGenerator;
import com.techconqueror.initializr.core.utility.generator.JavaClass;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NLayerFileProjectContributor implements ProjectContributor {

  private final ProjectDescription projectDescription;

  public NLayerFileProjectContributor(ProjectDescription projectDescription) {
    this.projectDescription = projectDescription;
  }

  @Override
  public void contribute(Path projectRoot) {
    OpenAPI openAPI = new OpenAPIV3Parser().read("./oapi.yaml");
    Map<String, List<RestControllerOperation>> operationsByTag = groupOperationsByTag(openAPI.getPaths());

    generateDtos(
      projectDescription.getPackageName(), openAPI, projectRoot.resolve("src/main/java").toString()
    )
      .stream()
      .filter(dto -> dto.dtoType() == DtoType.PERSIST)
      .forEach(dto -> {
        try {
          EntityGenerator
            .generate(projectDescription.getPackageName(), dto, projectRoot.resolve("src/main/java").toString());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    operationsByTag.forEach((tag, operations) -> {
      try {
        RestControllerRequirement requirement = new RestControllerRequirement(
          projectDescription.getPackageName(), projectRoot
            .resolve("src/main/java")
            .toString(), tag, getBasePath(operations), openAPI, operations
        );

        JavaClass serviceClass = RestControllerGenerator
          .generate(requirement);
        MapperGenerator
          .generate(tag, projectDescription.getPackageName(), projectRoot.resolve("src/main/java").toString());
        ServiceGenerator.generate(requirement, serviceClass);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static Set<String> extractUniqueTags(Paths paths) {
    return paths
      .values()
      .stream()
      .flatMap(pathItem -> pathItem.readOperationsMap().values().stream())
      .filter(operation -> operation.getTags() != null)
      .flatMap(operation -> operation.getTags().stream())
      .collect(Collectors.toSet());
  }

  private static Map<String, List<RestControllerOperation>> groupOperationsByTag(Paths paths) {
    return paths
      .entrySet()
      .stream()
      .flatMap(
        entry -> entry
          .getValue()
          .readOperationsMap()
          .entrySet()
          .stream()
          .flatMap(methodEntry -> {
            String httpMethod = methodEntry.getKey().name();
            Operation operation = methodEntry.getValue();
            return (operation.getTags() != null ? operation.getTags().stream() : Stream.of("DefaultController"))
              .map(tag -> Map.entry(tag, new RestControllerOperation(tag, httpMethod, entry.getKey(), operation)));
          })
      )
      .collect(
        Collectors
          .groupingBy(
            Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())
          )
      );
  }

  private static String getBasePath(List<RestControllerOperation> RestControllerOperations) {
    // Find the longest common prefix of all paths
    return RestControllerOperations
      .stream()
      .map(
        RestControllerOperation -> RestControllerOperation
          .path()
          .substring(0, RestControllerOperation.path().lastIndexOf('/'))
      )
      .reduce((o1, o2) -> o1.length() > o2.length() ? o1 : o2)
      .orElse("/");
  }

  private static List<Dto> generateDtos(String packageName, OpenAPI openAPI, String outputPath) {
    Components components = openAPI.getComponents();
    if (components == null || components.getSchemas() == null) {
      return List.of();
    }

    return components
      .getSchemas()
      .entrySet()
      .stream()
      .map(entry -> {
        try {
          return DtoGenerator.generate(packageName, openAPI, outputPath, entry.getKey(), entry.getValue());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .toList();
  }
}
