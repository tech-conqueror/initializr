package com.techconqueror.initializr.core.nlayer.controller.rest;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;

public record RestControllerRequirement(
  String rootPackage,
  String outputPath,
  String resourceName,
  String basePath,
  OpenAPI openAPI,
  List<RestControllerOperation> operations
) {}
