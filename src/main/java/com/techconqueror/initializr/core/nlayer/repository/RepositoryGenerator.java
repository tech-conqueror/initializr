package com.techconqueror.initializr.core.nlayer.repository;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.techconqueror.initializr.core.nlayer.repository.entity.EntityClass;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;

public class RepositoryGenerator {

  public static void generateRepository(
    String packageName,
    EntityClass entityClass,
    String outputPath
  ) throws IOException {
    // Define the repository name
    String repositoryName = extractEntityName(entityClass.javaClass().className()) + "Repository";

    // Create a JavaPoet TypeSpec for the repository interface
    TypeSpec repositoryInterface = TypeSpec
      .interfaceBuilder(repositoryName)
      .addModifiers(Modifier.PUBLIC)
      .addSuperinterface(
        ParameterizedTypeName
          .get(
            ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"), ClassName
              .get(entityClass.javaClass().packageName(), entityClass.javaClass().className()), entityClass
                .primaryKeyType()
          )
      )
      .build();

    // Build the Java file
    JavaFile javaFile = JavaFile
      .builder(packageName + ".repository", repositoryInterface)
      .build();

    // Write the file to the output directory
    javaFile.writeTo(Path.of(outputPath));
  }

  public static String extractEntityName(String input) {
    Pattern pattern = Pattern.compile("^(.*?)Entity$");
    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }
}
