package com.techconqueror.initializr.core.nlayer.repository.entity;

import com.palantir.javapoet.TypeName;
import com.techconqueror.initializr.core.utility.generator.JavaClass;

public record EntityClass(
  JavaClass javaClass,
  String tableName,
  TypeName primaryKeyType
) {}
