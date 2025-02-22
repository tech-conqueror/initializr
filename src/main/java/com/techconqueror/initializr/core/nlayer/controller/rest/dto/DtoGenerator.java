package com.techconqueror.initializr.core.nlayer.controller.rest.dto;

import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.utility.generator.JavaField;
import com.techconqueror.initializr.core.utility.OpenAPIUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Modifier;

public class DtoGenerator {

  public static Dto generate(
    String packageName,
    OpenAPI openAPI,
    String outputPath,
    String dtoName,
    Schema<?> schema
  ) throws IOException {
    String type = schema
      .getTypes()
      .stream()
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No types available in schema"));
    Path directory = Path.of(outputPath);

    if ("array".equals(type)) {
      JavaFile
        .builder(
          packageName + ".controller.dto", TypeSpec
            .classBuilder(dtoName)
            .addModifiers(Modifier.PUBLIC)
            .superclass(
              OpenAPIUtils.mapSchemaToJavaType(packageName, openAPI, schema)
            )
            .addMethod(
              MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                  ParameterizedTypeName
                    .get(
                      ClassName.get(Collection.class), WildcardTypeName
                        .subtypeOf(OpenAPIUtils.mapSchemaToJavaType(packageName, openAPI, schema.getItems()))
                    ), "c"
                )
                .addStatement("super(c)")
                .build()
            )
            .build()
        )
        .build()
        .writeTo(directory);

      return new Dto(DtoType.RETRIEVE, null, null);
    }

    List<JavaField> fields = new ArrayList<>();
    if (schema.getProperties() != null) {
      fields = schema
        .getProperties()
        .entrySet()
        .stream()
        .map(
          entry -> new JavaField(
            entry.getKey(), OpenAPIUtils.mapSchemaToJavaType(packageName, openAPI, entry.getValue()), List.of(), List
              .of()
          )
        )
        .toList();
    }

    JavaFile
      .builder(
        packageName + ".controller.dto", TypeSpec
          .recordBuilder(dtoName)
          .addModifiers(Modifier.PUBLIC)
          .recordConstructor(
            MethodSpec
              .constructorBuilder()
              .addModifiers(Modifier.PUBLIC)
              .addParameters(
                fields
                  .stream()
                  .map(field -> ParameterSpec.builder(field.typeName(), field.name()).build())
                  .toList()
              )
              .build()
          )
          .build()
      )
      .build()
      .writeTo(directory);

    return new Dto(resolveDtoType(dtoName), extractPrefix(dtoName), fields);
  }

  private static String extractPrefix(String dtoName) {
    return dtoName.replaceFirst("PersistRequest$", "");
  }

  private static DtoType resolveDtoType(String dtoName) {
    return dtoName.matches(".*PersistRequest$") ? DtoType.PERSIST : DtoType.RETRIEVE;
  }
}
