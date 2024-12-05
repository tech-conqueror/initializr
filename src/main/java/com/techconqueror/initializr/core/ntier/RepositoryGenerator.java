package com.techconqueror.initializr.core.ntier;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.techconqueror.initializr.core.hibernate.EntityClassMetadata;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

/**
 * A utility class for generating repository interfaces for specified entities.
 *
 * <p>The generated repository interface adheres to the conventions of Spring Data JPA and extends
 * {@code JpaRepository}, enabling basic CRUD operations and custom query capabilities.
 *
 * <p>Example usage:
 *
 * <pre>
 * EntityClassMetadata metadata = new EntityClassMetadata("User", ...);
 * String outputPath = "/path/to/output";
 * RepositoryGenerator.generateRepository(metadata, outputPath);
 * </pre>
 */
public class RepositoryGenerator {

  /**
   * Generates a repository interface for the given entity and writes it to the specified output
   * path.
   *
   * <p>The generated repository interface includes:
   *
   * <ul>
   * <li>Extension of {@code JpaRepository} from Spring Data JPA.
   * <li>Automatic support for standard CRUD operations.
   * </ul>
   *
   * <p>Example of the generated repository interface:
   *
   * <pre>
   * package com.techconqueror.app.repository;
   *
   * import org.springframework.data.jpa.repository.JpaRepository;
   * import com.techconqueror.app.entity.User;
   *
   * public interface UserRepository extends JpaRepository&lt;User, Long&gt; {
   * }
   * </pre>
   *
   * @param packageName         the package name to assign to the generated class
   * @param entityClassMetadata the metadata of the entity for which the repository is to be
   *                            generated. This includes the entity name and package structure.
   * @param outputPath          the directory path where the generated repository file will be saved.
   * @throws IOException if an error occurs while writing the repository file.
   */
  public static void generateRepository(
    String packageName,
    EntityClassMetadata entityClassMetadata,
    String outputPath
  ) throws IOException {
    // Define the repository name
    String repositoryName = entityClassMetadata.getName() + "Repository";

    // Create a JavaPoet TypeSpec for the repository interface
    TypeSpec repositoryInterface = TypeSpec
      .interfaceBuilder(repositoryName)
      .addModifiers(Modifier.PUBLIC)
      .addSuperinterface(
        ParameterizedTypeName
          .get(
            ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
            ClassName.get(entityClassMetadata.getPackageName(), entityClassMetadata.getName()),
            entityClassMetadata.getPrimaryKeyType()
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
}
