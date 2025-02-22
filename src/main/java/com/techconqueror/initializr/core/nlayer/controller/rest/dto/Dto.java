package com.techconqueror.initializr.core.nlayer.controller.rest.dto;

import com.techconqueror.initializr.core.utility.generator.JavaField;

import java.util.List;

public record Dto(
  DtoType dtoType,
  String javaClassName,
  List<JavaField> fields
) {}
