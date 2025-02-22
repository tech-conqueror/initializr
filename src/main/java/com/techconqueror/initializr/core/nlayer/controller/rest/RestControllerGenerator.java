package com.techconqueror.initializr.core.nlayer.controller.rest;

import com.google.common.base.CaseFormat;
import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.utility.generator.CodeGeneratorUtils;
import com.techconqueror.initializr.core.utility.generator.JavaClass;
import com.techconqueror.initializr.core.utility.generator.JavaField;
import com.techconqueror.initializr.core.utility.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RestControllerGenerator {

  public static JavaClass generate(RestControllerRequirement requirement) throws IOException {
    JavaClass serviceClass = createServiceClass(requirement);

    JavaClass restControllerClass = createRestControllerClass(requirement, serviceClass);
    CodeGeneratorUtils.generateJavaClass(restControllerClass, requirement.outputPath());

    serviceClass
      .methods()
      .addAll(
        restControllerClass
          .methods()
          .stream()
          .map(RestControllerGenerator::createServiceMethod)
          .toList()
      );

    return serviceClass;
  }

  private static JavaClass createServiceClass(RestControllerRequirement requirement) {
    return new JavaClass(
      requirement.rootPackage() + ".service", StringUtils
        .capitalize(requirement
          .resourceName() + "Service"), new ArrayList<>(), false, true, new ArrayList<>(), new ArrayList<>()
    );
  }

  private static MethodSpec createServiceMethod(MethodSpec controllerMethod) {
    return MethodSpec
      .methodBuilder(controllerMethod.name())
      .addModifiers(Modifier.PUBLIC)
      .returns(controllerMethod.returnType())
      .addParameters(
        controllerMethod
          .parameters()
          .stream()
          .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
          .toList()
      )
      .build();
  }

  private static JavaClass createRestControllerClass(
    RestControllerRequirement requirement,
    JavaClass serviceClass
  ) {
    JavaField serviceField = new JavaField(
      StringUtils.decapitalize(serviceClass.className()), serviceClass.getTypeName(), List.of(), List.of(Modifier.FINAL)
    );
    return new JavaClass(
      requirement.rootPackage() + ".controller", requirement.resourceName() + "RestController", List
        .of(
          AnnotationSpec.builder(RestController.class).build(), AnnotationSpec
            .builder(RequestMapping.class)
            .addMember("value", "$S", requirement.basePath())
            .build()
        ), false, true, List.of(serviceField), createMethods(requirement, serviceField)
    );
  }

  private static List<MethodSpec> createMethods(RestControllerRequirement requirement, JavaField serviceField) {
    return requirement
      .operations()
      .stream()
      .map(operation -> createMethod(requirement, operation, serviceField))
      .toList();
  }

  private static MethodSpec createMethod(
    RestControllerRequirement requirement,
    RestControllerOperation operation,
    JavaField serviceField
  ) {
    TypeName returnType = resolveReturnType(requirement.rootPackage(), requirement.openAPI(), operation.operation());

    List<String> pathVars = extractPathVariables(operation.path());
    TypeName requestBodyType = resolveRequestBodyType(
      requirement.rootPackage(), requirement.openAPI(), operation.operation()
    );

    MethodSpec.Builder methodBuilder = createMethodBuilder(operation, requirement.basePath(), returnType);
    addMethodParameters(methodBuilder, pathVars, requestBodyType, requirement, operation.operation());
    addResponseStatus(methodBuilder, operation.operation());

    methodBuilder
      .addStatement(
        createMethodBody(
          operation, serviceField
        )
      );

    return methodBuilder.build();
  }

  private static MethodSpec.Builder createMethodBuilder(
    RestControllerOperation operation,
    String basePath,
    TypeName returnType
  ) {
    MethodSpec.Builder builder = MethodSpec
      .methodBuilder(
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, extractAction(operation.operation().getOperationId()))
      )
      .addModifiers(Modifier.PUBLIC)
      .returns(returnType);

    AnnotationSpec.Builder mappingAnnotation = AnnotationSpec.builder(switch (operation.httpMethod().toUpperCase()) {
      case "GET" -> GetMapping.class;
      case "POST" -> PostMapping.class;
      case "PUT" -> PutMapping.class;
      case "DELETE" -> DeleteMapping.class;
      default -> RequestMapping.class;
    });

    String subPath = getSubPath(operation.path(), basePath);

    if (!subPath.isEmpty()) {
      mappingAnnotation.addMember("value", "$S", subPath);
    }

    builder.addAnnotation(mappingAnnotation.build());

    return builder;
  }

  private static String getSubPath(String fullPath, String basePath) {
    return fullPath.replaceFirst(basePath, "").replaceAll("^/", "");
  }

  private static void addResponseStatus(MethodSpec.Builder builder, Operation operation) {
    int statusCode = getResponseStatus(operation);

    if (statusCode != 200) {
      builder
        .addAnnotation(
          AnnotationSpec
            .builder(ResponseStatus.class)
            .addMember("value", "$T.$L", HttpStatus.class, switch (statusCode) {
              case 201 -> "CREATED";
              case 204 -> "NO_CONTENT";
              case 400 -> "BAD_REQUEST";
              case 401 -> "UNAUTHORIZED";
              case 403 -> "FORBIDDEN";
              case 404 -> "NOT_FOUND";
              case 409 -> "CONFLICT";
              case 500 -> "INTERNAL_SERVER_ERROR";
              default -> "OK";
            })
            .build()
        );
    }
  }

  private static int getResponseStatus(Operation operation) {
    if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
      return 200;
    }

    return operation
      .getResponses()
      .keySet()
      .stream()
      .filter(key -> key.matches("\\d{3}"))
      .mapToInt(Integer::parseInt)
      .findFirst()
      .orElse(200);
  }

  private static TypeName resolveReturnType(String packageName, OpenAPI openAPI, Operation operation) {
    if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
      return TypeName.VOID;
    }

    ApiResponse response = operation
      .getResponses()
      .entrySet()
      .stream()
      .filter(entry -> entry.getKey().startsWith("2"))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);

    if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
      return TypeName.VOID;
    }

    MediaType mediaType = response.getContent().values().iterator().next();
    Schema<?> schema = mediaType.getSchema();
    return schema != null ? OpenAPIUtils.mapSchemaToJavaType2(packageName, openAPI, schema) : TypeName.VOID;
  }

  private static void addMethodParameters(
    MethodSpec.Builder builder,
    List<String> pathVars,
    TypeName requestBodyType,
    RestControllerRequirement requirement,
    Operation operation
  ) {
    // Add path variables
    pathVars
      .forEach(
        var -> builder
          .addParameter(
            ParameterSpec
              .builder(String.class, var)
              .addAnnotation(PathVariable.class)
              .build()
          )
      );

    // Add query parameters
    getRequestParams(requirement.rootPackage(), requirement.openAPI(), operation)
      .forEach(builder::addParameter);

    // Add request body if present
    if (requestBodyType != null) {
      builder
        .addParameter(
          ParameterSpec
            .builder(requestBodyType, "requestBody")
            .addAnnotation(RequestBody.class)
            .build()
        );
    }
  }

  private static List<String> extractPathVariables(String path) {
    final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");
    List<String> variables = new ArrayList<>();
    Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
    while (matcher.find()) {
      variables.add(matcher.group(1));
    }
    return variables;
  }

  private static List<ParameterSpec> getRequestParams(String packageName, OpenAPI openAPI, Operation operation) {
    if (operation.getParameters() == null) {
      return Collections.emptyList();
    }

    return operation
      .getParameters()
      .stream()
      .filter(param -> "query".equals(param.getIn()))
      .map(param -> {
        TypeName paramType = OpenAPIUtils.mapSchemaToJavaType(packageName, openAPI, param.getSchema());
        return ParameterSpec
          .builder(paramType, param.getName())
          .addAnnotation(
            AnnotationSpec
              .builder(RequestParam.class)
              .addMember("value", "$S", param.getName())
              .addMember("required", "$L", param.getRequired() != null && param.getRequired())
              .build()
          )
          .build();
      })
      .collect(Collectors.toList());
  }

  private static TypeName resolveRequestBodyType(String packageName, OpenAPI openAPI, Operation operation) {
    if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
      return null;
    }

    MediaType mediaType = operation.getRequestBody().getContent().values().iterator().next();
    Schema<?> schema = mediaType.getSchema();
    return schema != null ? OpenAPIUtils.mapSchemaToJavaType(packageName, openAPI, schema) : null;
  }

  private static String extractAction(String operationId) {
    final Pattern OPERATION_PATTERN = Pattern.compile("^(create|get|replace|patch|delete)([A-Z].*)$");

    if (operationId == null) {
      throw new IllegalStateException("Operation ID is required");
    }

    Matcher matcher = OPERATION_PATTERN.matcher(operationId);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid operation ID format: " + operationId);
    }

    String action = matcher.group(1).toUpperCase();
    String entity = matcher.group(2);
    boolean isPlural = entity.endsWith("s");

    if ("GET".equals(action)) {
      return isPlural ? "GET_MANY" : "GET";
    }

    return action;
  }

  private static String createMethodBody(
    RestControllerOperation operation,
    JavaField serviceField
  ) {
    String action = extractAction(operation.operation().getOperationId());

    String template = switch (action) {
      case "CREATE" -> "return ${service}.create(requestBody)";
      case "GET_MANY" -> "return ${service}.getMany()";
      case "GET" -> "return ${service}.get(id)";
      case "REPLACE" -> "${service}.replace(id, requestBody)";
      case "DELETE" -> "${service}.delete(id)";
      default -> throw new IllegalArgumentException("Unsupported operation: " + action);
    };

    return StringUtils.format(template, Map.of("service", serviceField.name()));
  }
}
