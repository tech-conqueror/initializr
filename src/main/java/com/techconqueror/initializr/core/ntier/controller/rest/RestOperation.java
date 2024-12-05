package com.techconqueror.initializr.core.ntier.controller.rest;

import com.techconqueror.initializr.core.java.ClassMetadata;

public record RestOperation(
  RestOperationName name,
  ClassMetadata request,
  ClassMetadata response
) {}
