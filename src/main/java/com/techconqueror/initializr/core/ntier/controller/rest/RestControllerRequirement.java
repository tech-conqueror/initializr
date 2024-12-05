package com.techconqueror.initializr.core.ntier.controller.rest;

import java.util.Map;

public record RestControllerRequirement(
  String rootPackage,
  String resourceName,
  Map<RestOperationName, RestOperation> operations,
  String outputPath
) {}
