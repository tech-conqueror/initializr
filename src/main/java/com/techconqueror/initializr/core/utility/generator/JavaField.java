package com.techconqueror.initializr.core.utility.generator;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.List;

public record JavaField(
  String name,
  TypeName typeName,
  List<AnnotationSpec> annotations,
  List<Modifier> modifiers
) {

  public FieldSpec toFieldSpec() {
    FieldSpec.Builder builder = FieldSpec
      .builder(typeName, name, Modifier.PRIVATE)
      .addAnnotations(annotations);
    modifiers.forEach(builder::addModifiers);
    return builder.build();
  }
}
