package com.techconqueror.initializr.core.utility.generator;

import com.palantir.javapoet.*;
import javax.lang.model.element.Modifier;
import java.util.List;

public record JavaClass(
  String packageName,
  String className,
  List<AnnotationSpec> annotations,
  boolean hasNoArgConstructor,
  boolean hasAllArgConstructor,
  List<JavaField> fields,
  List<MethodSpec> methods
) {

  public TypeSpec toTypeSpec() {
    TypeSpec.Builder typeBuilder = TypeSpec
      .classBuilder(className)
      .addModifiers(Modifier.PUBLIC)
      .addFields(fields.stream().map(JavaField::toFieldSpec).toList())
      .addMethods(methods);

    if (hasAllArgConstructor) {
      MethodSpec.Builder constructorBuilder = MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameters(
          fields
            .stream()
            .map(field -> ParameterSpec.builder(field.typeName(), field.name()).build())
            .toList()
        );

      fields.forEach(field -> constructorBuilder.addStatement("this.$N = $N", field.name(), field.name()));
      typeBuilder.addMethod(constructorBuilder.build());
    }

    annotations.forEach(typeBuilder::addAnnotation);
    return typeBuilder.build();
  }

  public TypeName getTypeName() {
    return ClassName.get(packageName, className);
  }
}
