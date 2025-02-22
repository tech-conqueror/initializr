package com.techconqueror.initializr.core.utility.generator;

import com.palantir.javapoet.AnnotationSpec;
import jakarta.persistence.*;

public class EntityGeneratorUtils {

  public static AnnotationSpec createEntityAnnotation() {
    return AnnotationSpec.builder(Entity.class).build();
  }

  public static AnnotationSpec createTableAnnotation(String tableName) {
    return AnnotationSpec
      .builder(Table.class)
      .addMember("name", "$S", tableName)
      .build();
  }

  public static AnnotationSpec createIdAnnotation() {
    return AnnotationSpec.builder(Id.class).build();
  }

  public static AnnotationSpec createGeneratedValueAnnotation() {
    return AnnotationSpec
      .builder(GeneratedValue.class)
      .addMember("strategy", "$T.IDENTITY", GenerationType.class)
      .build();
  }
}
