package com.techconqueror.initializr.core.nlayer.repository.entity;

import static com.techconqueror.initializr.core.nlayer.repository.RepositoryGenerator.generateRepository;

import com.google.common.base.CaseFormat;
import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.nlayer.controller.rest.dto.Dto;
import com.techconqueror.initializr.core.utility.generator.EntityGeneratorUtils;
import com.techconqueror.initializr.core.utility.generator.JavaClass;
import com.techconqueror.initializr.core.utility.generator.JavaField;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class EntityGenerator {

  public static void generate(
    String packageName,
    Dto dto,
    String outputPath
  ) throws IOException {
    List<JavaField> fields = new ArrayList<>();
    fields
      .add(
        new JavaField(
          "id", TypeName.get(Long.class), List
            .of(EntityGeneratorUtils.createIdAnnotation(), EntityGeneratorUtils.createGeneratedValueAnnotation()), List
              .of()
        )
      );
    fields.addAll(dto.fields());

    EntityClass metadata = new EntityClass(
      new JavaClass(
        packageName + ".repository.entity", dto.javaClassName() + "Entity", List
          .of(
            EntityGeneratorUtils.createEntityAnnotation(), EntityGeneratorUtils
              .createTableAnnotation(CaseFormat.UPPER_CAMEL
                .to(CaseFormat.LOWER_UNDERSCORE, dto.javaClassName()))
          ), false, false, fields, List.of()
      ), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, dto.javaClassName()), TypeName.get(Long.class)
    );
    generateEntityFile(metadata.javaClass(), outputPath);
    generateRepository(packageName, metadata, outputPath);
  }

  private static void generateEntityFile(JavaClass entityClass, String outputPath) throws IOException {
    JavaFile javaFile = JavaFile.builder(entityClass.packageName(), entityClass.toTypeSpec()).build();
    javaFile.writeTo(Path.of(outputPath));
  }
}
