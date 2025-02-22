package com.techconqueror.initializr.core.common;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

/**
 * Utility class for generating custom exception classes programmatically.
 *
 * <p>This class leverages the {@code JavaPoet} library to create exception classes with standard
 * constructors, package declarations, and inheritance. The generated classes are written to the
 * specified output directory with a user-defined package name.
 *
 * <p>Example use case:
 *
 * <pre>
 * String outputPath = "/path/to/output";
 * String rootPackage = "com.techconqueror.common.exception";
 * ExceptionGenerator.generateResourceNotFoundException(outputPath, rootPackage);
 * </pre>
 *
 * <p>The above example creates a {@code ResourceNotFoundException} class in the given package and
 * writes it to the specified output directory. The generated class extends {@code RuntimeException}
 * and includes constructors for specifying an error message and an error cause.
 *
 * <p>All methods in this utility class are static, and no instantiation of the class is necessary.
 */
public class ExceptionGenerator {

  /**
   * Generates a custom exception class named {@code ResourceNotFoundException} and writes it to the
   * specified output path.
   *
   * <p>The generated exception class is structured as follows:
   *
   * <ul>
   * <li>It belongs to the specified {@code rootPackage}.
   * <li>It extends {@code RuntimeException}.
   * <li>Includes the following constructors:
   * <ul>
   * <li>A public constructor accepting a {@code String message}, which calls {@code
   *             super(message)}.
   * <li>A public constructor accepting a {@code String message} and a {@code Throwable
   *             cause}, which calls {@code super(message, cause)}.
   * </ul>
   * </ul>
   *
   * <p>Example of the generated class:
   *
   * <pre>
   * package com.techconqueror.common.exception;
   *
   * public class ResourceNotFoundException extends RuntimeException {
   * public ResourceNotFoundException(String message) {
   * super(message);
   * }
   *
   * public ResourceNotFoundException(String message, Throwable cause) {
   * super(message, cause);
   * }
   * }
   * </pre>
   *
   * <p>Example usage:
   *
   * <pre>
   * String outputPath = "/path/to/output";
   * String rootPackage = "com.techconqueror.common.exception";
   * ExceptionGenerator.generateResourceNotFoundException(outputPath, rootPackage);
   * </pre>
   *
   * @param packageName the package name to assign to the generated class
   * @param outputPath  the directory where the {@code ResourceNotFoundException} class will be
   *                    written
   * @throws IOException if an error occurs while writing the class file to the specified path
   */
  public static void generateResourceNotFoundException(String packageName, String outputPath) throws IOException {
    // Define the class name
    String className = "ResourceNotFoundException";

    // Create the exception class
    TypeSpec exceptionClass = TypeSpec
      .classBuilder(className)
      .addModifiers(Modifier.PUBLIC)
      .superclass(RuntimeException.class)
      .addMethod(
        MethodSpec
          .constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .build()
      )
      .addMethod(
        MethodSpec
          .constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(String.class, "message")
          .addStatement("super(message)")
          .build()
      )
      .addMethod(
        MethodSpec
          .constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(String.class, "message")
          .addParameter(Throwable.class, "cause")
          .addStatement("super(message, cause)")
          .build()
      )
      .build();

    // Build the Java file
    JavaFile javaFile = JavaFile.builder(packageName, exceptionClass).build();

    // Write to the output path
    javaFile.writeTo(Path.of(outputPath));
  }
}
