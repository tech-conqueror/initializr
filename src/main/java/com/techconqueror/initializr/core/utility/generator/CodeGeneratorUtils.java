package com.techconqueror.initializr.core.utility.generator;

import com.palantir.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;

public class CodeGeneratorUtils {

  public static void generateJavaClass(JavaClass javaClass, String outputPath) throws IOException {
    JavaFile
      .builder(
        javaClass.packageName(), javaClass.toTypeSpec()
      )
      .build()
      .writeTo(Path.of(outputPath));
  }
}
