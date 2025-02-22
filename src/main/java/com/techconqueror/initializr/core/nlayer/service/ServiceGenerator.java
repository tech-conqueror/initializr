package com.techconqueror.initializr.core.nlayer.service;

import com.google.common.base.CaseFormat;
import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.nlayer.controller.rest.RestControllerRequirement;
import com.techconqueror.initializr.core.utility.generator.JavaClass;
import com.techconqueror.initializr.core.utility.generator.JavaField;
import com.techconqueror.initializr.core.utility.generator.CodeGeneratorUtils;
import com.techconqueror.initializr.core.utility.StringUtils;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceGenerator {

  public static void generate(RestControllerRequirement requirement, JavaClass serviceClass) throws IOException {
    JavaField repositoryField = createRepositoryField(requirement);
    JavaField mapperField = createMapperField(requirement);

    serviceClass.fields().addAll(List.of(repositoryField, mapperField));
    serviceClass.annotations().add(AnnotationSpec.builder(Service.class).build());
    List<MethodSpec> methods = serviceClass
      .methods()
      .stream()
      .map(
        method -> method
          .toBuilder()
          .addStatement(
            getMethodBody(requirement, method.name(), repositoryField, mapperField)
          )
          .build()
      )
      .toList();
    serviceClass.methods().clear();
    serviceClass.methods().addAll(methods);

    CodeGeneratorUtils
      .generateJavaClass(
        serviceClass, requirement.outputPath()
      );
  }

  private static JavaField createRepositoryField(RestControllerRequirement requirement) {
    ClassName typeName = ClassName
      .get(requirement.rootPackage() + ".repository", requirement.resourceName() + "Repository");
    return new JavaField(StringUtils.decapitalize(typeName.simpleName()), typeName, List.of(), List.of(Modifier.FINAL));
  }

  private static JavaField createMapperField(RestControllerRequirement requirement) {
    ClassName typeName = ClassName
      .get(requirement.rootPackage() + ".service.mapper", requirement.resourceName() + "Mapper");
    return new JavaField(StringUtils.decapitalize(typeName.simpleName()), typeName, List.of(), List.of(Modifier.FINAL));
  }

  private static CodeBlock getMethodBody(
    RestControllerRequirement requirement,
    String methodName,
    JavaField repositoryField,
    JavaField mapperField
  ) {
    final Pattern OPERATION_PATTERN = Pattern.compile("^(create|getMany|get|replace|patch|delete)$");

    Matcher matcher = OPERATION_PATTERN.matcher(methodName);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid method name format: " + methodName);
    }

    CodeBlock.Builder builder = CodeBlock.builder();
    final Map<String, Object> fieldParams = new HashMap<>(
      Map
        .of(
          "repository", repositoryField.name(), "mapper", mapperField.name()
        )
    );
    final List<Object> typeParams = new ArrayList<>();

    String action = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, matcher.group(1));
    String template = switch (action) {
      case "CREATE" -> "return ${mapper}.toDto(${repository}.save(${mapper}.toEntity(requestBody)))";
      case "GET_MANY" -> "return ${mapper}.toDtos(${repository}.findAll())";
      case "GET" -> {
        typeParams.add(ClassName.get(requirement.rootPackage() + ".common.exception", "ResourceNotFoundException"));
        yield "return ${repository}.findById(Long.valueOf(id)).map(${mapper}::toDto).orElseThrow($T::new)";
      }
      case "REPLACE" -> {
        String entityVarName = requirement.resourceName() + "Entity";
        fieldParams.put("entity", entityVarName);
        typeParams
          .add(ClassName.get(requirement.rootPackage() + ".repository.entity", StringUtils.capitalize(entityVarName)));
        typeParams.add(ClassName.get(requirement.rootPackage() + ".common.exception", "ResourceNotFoundException"));

        yield """
          $T ${entity} = ${mapper}.toEntity(requestBody);

          if (!${repository}.existsById(Long.valueOf(id))) {
              throw new $T();
          }

          ${repository}.save(${entity})
          """;
      }
      case "DELETE" -> "${repository}.deleteById(Long.valueOf(id))";
      default -> "throw new UnsupportedOperationException(\"Not implemented yet\")";
    };

    builder.add(StringUtils.format(template, fieldParams), typeParams.toArray());
    return builder.build();
  }
}