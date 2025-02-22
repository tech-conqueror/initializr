package com.techconqueror.initializr.core.nlayer.service.mapper;

import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MapperGenerator {

  public static void generate(
    String baseName,
    String packageName,
    String outputPath
  ) throws IOException {
    String entityPackage = packageName + ".repository.entity";
    String dtoPackage = packageName + ".controller.dto";
    String mapperPackage = packageName + ".service.mapper";

    // Define class names dynamically
    ClassName entityClass = ClassName.get(entityPackage, baseName + "Entity");
    ClassName persistDtoClass = ClassName.get(dtoPackage, baseName + "PersistRequest");
    ClassName readDtoClass = ClassName.get(dtoPackage, baseName);
    ClassName listDtoClass = ClassName.get(dtoPackage, baseName + "s");

    // Create @Mapper annotation
    AnnotationSpec mapperAnnotation = AnnotationSpec
      .builder(ClassName.get("org.mapstruct", "Mapper"))
      .addMember("componentModel", "$S", "spring")
      .build();

    // Define the Mapper interface
    TypeSpec mapperInterface = TypeSpec
      .interfaceBuilder(baseName + "Mapper")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(mapperAnnotation)
      .addMethod(
        MethodSpec
          .methodBuilder("toEntity")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .returns(entityClass)
          .addParameter(persistDtoClass, "dto")
          .build()
      )
      .addMethod(
        MethodSpec
          .methodBuilder("toDto")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .returns(readDtoClass)
          .addParameter(entityClass, "entity")
          .build()
      )
      .addMethod(
        MethodSpec
          .methodBuilder("toDtos")
          .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
          .returns(listDtoClass)
          .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), entityClass), "entities")
          .addStatement("List<$T> dtoList = entities.stream().map(this::toDto).toList()", readDtoClass)
          .addStatement("return new $T(dtoList)", listDtoClass)
          .build()
      )
      .build();

    // Create Java file
    JavaFile
      .builder(mapperPackage, mapperInterface)
      .build()
      .writeTo(Path.of(outputPath));
  }
}
