package com.techconqueror.initializr.core.nlayer.controller.rest;

import io.swagger.v3.oas.models.Operation;

public record RestControllerOperation(
  String resourceName,
  String httpMethod,
  String path,
  Operation operation
) {}
