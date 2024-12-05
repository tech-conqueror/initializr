package com.techconqueror.initializr.core.ntier.controller.rest;

import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.java.ClassMetadata;
import com.techconqueror.initializr.core.utility.Try;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.lang.model.element.Modifier;
import org.springframework.web.bind.annotation.*;

public class RestControllerGenerator {

  public static void generateController(RestControllerRequirement requirement) throws IOException {
    Set<RestOperationName> operationNames = requirement.operations().keySet();

    ClassName serviceClassName = ClassName
      .get(
        requirement.rootPackage() + ".service",
        capitalize(requirement.resourceName() + "Service")
      );
    String serviceFieldName = classNameToVariableName(serviceClassName);

    TypeSpec restController = TypeSpec
      .classBuilder(capitalize(requirement.resourceName() + "RestController"))
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(RestController.class)
      .addAnnotation(
        AnnotationSpec
          .builder(RequestMapping.class)
          .addMember("value", "$S", "/" + requirement.resourceName() + "s")
          .build()
      )
      .addField(serviceClassName, serviceFieldName, Modifier.PRIVATE, Modifier.FINAL)
      .addMethod(createConstructor(serviceClassName))
      .addMethods(
        operationNames
          .stream()
          .flatMap(operationName -> Try.of(() -> switch (operationName) {
            case RestOperationName.GET_ALL -> createGetAllOperation(
              serviceFieldName,
              requirement
                .operations()
                .get(RestOperationName.GET_ALL)
                .response(),
              requirement.outputPath()
            );
            case RestOperationName.GET_BY_ID -> createGetByIdOperation(
              serviceFieldName,
              requirement
                .operations()
                .get(RestOperationName.GET_BY_ID)
                .response(),
              requirement.outputPath()
            );
            case RestOperationName.CREATE -> createPostOperation(
              serviceFieldName,
              requirement.operations().get(RestOperationName.CREATE),
              requirement.outputPath()
            );
            case RestOperationName.REPLACE -> createPutOperation(
              serviceFieldName,
              requirement
                .operations()
                .get(RestOperationName.REPLACE)
                .request(),
              requirement.outputPath()
            );
            case RestOperationName.PARTIAL_UPDATE -> createPatchOperation(serviceFieldName);
            case RestOperationName.DELETE -> createDeleteOperation(serviceFieldName);
          })
            .valueStream())
          .toList()
      )
      .build();

    JavaFile javaFile = JavaFile
      .builder(requirement.rootPackage() + ".controller.rest", restController)
      .build();
    javaFile.writeTo(Path.of(requirement.outputPath()));
  }

  private static MethodSpec createConstructor(ClassName serviceName) {
    String serviceVariableName = classNameToVariableName(serviceName);
    return MethodSpec
      .constructorBuilder()
      .addParameter(serviceName, serviceVariableName)
      .addStatement("this.%s = %s".formatted(serviceVariableName, serviceVariableName))
      .build();
  }

  private static MethodSpec createGetAllOperation(
    String serviceFieldName,
    ClassMetadata response,
    String outputPath
  ) throws IOException {
    generateRecord(response, outputPath);

    return MethodSpec
      .methodBuilder("getAll")
      .addAnnotation(GetMapping.class)
      .addModifiers(Modifier.PUBLIC)
      .returns(
        ParameterizedTypeName
          .get(
            ClassName.get(List.class),
            ClassName.get(response.getPackageName(), response.getName())
          )
      )
      .addStatement("return %s.findAll()".formatted(serviceFieldName))
      .build();
  }

  private static MethodSpec createGetByIdOperation(
    String serviceFieldName,
    ClassMetadata response,
    String outputPath
  ) throws IOException {
    generateRecord(response, outputPath);

    return MethodSpec
      .methodBuilder("getById")
      .addAnnotation(
        AnnotationSpec
          .builder(GetMapping.class)
          .addMember("value", "$S", "/{id}")
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .addParameter(
        ParameterSpec
          .builder(Long.class, "id")
          .addAnnotation(
            AnnotationSpec.builder(PathVariable.class).build()
          )
          .build()
      )
      .returns(ClassName.get(response.getPackageName(), response.getName()))
      .addStatement("return %s.findById(id)".formatted(serviceFieldName))
      .build();
  }

  private static MethodSpec createPostOperation(
    String serviceFieldName,
    RestOperation restOperation,
    String outputPath
  ) throws IOException {
    generateRecord(restOperation.request(), outputPath);
    generateRecord(restOperation.response(), outputPath);

    return MethodSpec
      .methodBuilder("create")
      .addAnnotation(PostMapping.class)
      .addAnnotation(
        AnnotationSpec
          .builder(ResponseStatus.class)
          .addMember("value", "$T.CREATED", ClassName.get("org.springframework.http", "HttpStatus"))
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .addParameter(
        ParameterSpec
          .builder(
            ClassName
              .get(
                restOperation.request().getPackageName(),
                restOperation.request().getName()
              ),
            "request"
          )
          .addAnnotation(RequestBody.class)
          .build()
      )
      .returns(
        ClassName
          .get(
            restOperation.response().getPackageName(),
            restOperation.response().getName()
          )
      )
      .addStatement("return %s.create(request)".formatted(serviceFieldName))
      .build();
  }

  private static MethodSpec createPutOperation(
    String serviceFieldName,
    ClassMetadata request,
    String outputPath
  ) throws IOException {
    generateRecord(request, outputPath);

    return MethodSpec
      .methodBuilder("replace")
      .addAnnotation(
        AnnotationSpec
          .builder(PutMapping.class)
          .addMember("value", "$S", "/{id}")
          .build()
      )
      .addAnnotation(
        AnnotationSpec
          .builder(ResponseStatus.class)
          .addMember("value", "$T.NO_CONTENT", ClassName.get("org.springframework.http", "HttpStatus"))
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .addParameter(
        ParameterSpec
          .builder(Long.class, "id")
          .addAnnotation(
            AnnotationSpec.builder(PathVariable.class).build()
          )
          .build()
      )
      .addParameter(
        ParameterSpec
          .builder(ClassName.get(request.getPackageName(), request.getName()), "request")
          .addAnnotation(RequestBody.class)
          .build()
      )
      .returns(void.class)
      .addStatement("%s.update(id, request)".formatted(serviceFieldName))
      .build();
  }

  private static MethodSpec createPatchOperation(String serviceFieldName) {
    return MethodSpec
      .methodBuilder("patch")
      .addAnnotation(
        AnnotationSpec
          .builder(PatchMapping.class)
          .addMember("value", "$S", "/{id}")
          .build()
      )
      .addAnnotation(
        AnnotationSpec
          .builder(ResponseStatus.class)
          .addMember("value", "$T.NO_CONTENT", ClassName.get("org.springframework.http", "HttpStatus"))
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .addParameter(
        ParameterSpec
          .builder(Long.class, "id")
          .addAnnotation(
            AnnotationSpec.builder(PathVariable.class).build()
          )
          .build()
      )
      .addParameter(
        ParameterSpec
          .builder(
            ParameterizedTypeName
              .get(
                ClassName.get(List.class),
                ClassName.get("com.github.fge.jsonpatch", "JsonPatchOperation")
              ),
            "request"
          )
          .addAnnotation(RequestBody.class)
          .build()
      )
      .returns(void.class)
      .addStatement("%s.partialUpdate(id, request)".formatted(serviceFieldName))
      .build();
  }

  private static MethodSpec createDeleteOperation(String serviceFieldName) {
    return MethodSpec
      .methodBuilder("delete")
      .addAnnotation(
        AnnotationSpec
          .builder(DeleteMapping.class)
          .addMember("value", "$S", "/{id}")
          .build()
      )
      .addAnnotation(
        AnnotationSpec
          .builder(ResponseStatus.class)
          .addMember("value", "$T.NO_CONTENT", ClassName.get("org.springframework.http", "HttpStatus"))
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .addParameter(
        ParameterSpec
          .builder(Long.class, "id")
          .addAnnotation(
            AnnotationSpec.builder(PathVariable.class).build()
          )
          .build()
      )
      .returns(void.class)
      .addStatement("%s.deleteById(id)".formatted(serviceFieldName))
      .build();
  }

  private static void generateRecord(ClassMetadata classMetadata, String outputPath) throws IOException {
    TypeSpec record = TypeSpec
      .recordBuilder(classMetadata.getName())
      .addModifiers(Modifier.PUBLIC)
      .addFields(
        classMetadata
          .getFields()
          .stream()
          .map(
            fieldMetadata -> FieldSpec
              .builder(
                fieldMetadata.getTypeName(),
                fieldMetadata.getName(),
                Modifier.PRIVATE
              )
              .addAnnotations(fieldMetadata.getAnnotations())
              .build()
          )
          .toList()
      )
      .build();

    JavaFile javaFile = JavaFile.builder(classMetadata.getPackageName(), record).build();
    javaFile.writeTo(Path.of(outputPath));
  }

  private static String capitalize(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  private static String decapitalize(String input) {
    if (input == null || input.isEmpty()) {
      return input; // Return input as-is if it's null or empty
    }

    return input.substring(0, 1).toLowerCase() + input.substring(1);
  }

  private static String classNameToVariableName(ClassName className) {
    return decapitalize(className.simpleName());
  }
}
