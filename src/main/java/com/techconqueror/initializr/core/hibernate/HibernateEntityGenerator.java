package com.techconqueror.initializr.core.hibernate;

import static com.techconqueror.initializr.core.hibernate.HibernateAnnotationGenerator.*;
import static com.techconqueror.initializr.core.ntier.RepositoryGenerator.generateRepository;
import static com.techconqueror.initializr.core.ntier.ServiceGenerator.generateService;
import static org.springframework.util.StringUtils.capitalize;

import com.google.common.base.CaseFormat;
import com.palantir.javapoet.*;
import com.techconqueror.initializr.core.java.ClassSource;
import jakarta.persistence.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * The {@code HibernateEntityGenerator} class provides functionality to generate Java entity classes
 * from database table structures. It connects to a relational database, fetches metadata about the
 * tables, and generates Hibernate-compatible Java classes using the JavaPoet library.
 *
 * <p>The generated entity classes include proper annotations for Hibernate Object-Relational
 * Mapping (ORM), including {@code @Entity}, {@code @Table}, {@code @Id}, {@code @OneToOne},
 * {@code @ManyToOne}, and {@code @Column}.
 *
 * <h3>Key Features:</h3>
 *
 * <ul>
 * <li>Automatically generates entity classes for all tables in a specified database schema.
 * <li>Handles primary keys, foreign keys, and unique constraints with appropriate annotations.
 * <li>Supports various Java types mapped from database column types.
 * <li>Handles relationships between entities, such as {@code @OneToOne} and {@code @ManyToOne}.
 * <li>Uses customizable annotations and metadata through a modular architecture.
 * </ul>
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * Connection connection = ...; // Obtain a JDBC connection
 * String outputPath = "path/to/output/directory";
 * HibernateEntityGenerator.generateEntitiesForAllTables(outputPath, connection);
 * }</pre>
 *
 * @see EntityClassMetadata
 * @see EntityFieldMetadata
 * @see HibernateAnnotationGenerator
 */
public class HibernateEntityGenerator {

  /**
   * Generates entity classes for all tables in the given database connection and writes them to the
   * specified output path.
   *
   * @param packageName the package name to assign to the generated class
   * @param outputPath  the directory path where the generated entity files will be saved.
   * @param connection  the {@link Connection} object used to connect to the database.
   * @throws Exception if an error occurs while generating the entities.
   */
  public static void generateEntitiesForAllTables(
    String packageName,
    String outputPath,
    Connection connection
  ) throws Exception {
    List<String> tableNames = fetchAllTableNames(connection);
    for (String tableName : tableNames) {
      System.out.println("Generating entity for table: " + tableName);
      generateEntityFromTable(packageName, tableName, outputPath, connection);
    }
  }

  /**
   * Generates a Java entity class for a specific table and writes it to the specified output path.
   *
   * @param packageName the package name to assign to the generated class
   * @param tableName   the name of the table for which the entity class is to be generated.
   * @param outputPath  the directory path where the generated entity file will be saved.
   * @param connection  the {@link Connection} object used to connect to the database.
   * @throws Exception if an error occurs while generating the entity.
   */
  private static void generateEntityFromTable(
    String packageName,
    String tableName,
    String outputPath,
    Connection connection
  ) throws Exception {
    EntityClassMetadata metadata = fetchTableMetadata(packageName + ".entity", tableName, connection);
    metadata.getAnnotations().add(createEntityAnnotation());
    metadata.getAnnotations().add(createTableAnnotation(metadata.getTableName()));
    generateEntityFile(metadata, outputPath);
    generateRepository(packageName, metadata, outputPath);
    generateService(packageName, metadata, outputPath);
  }

  /**
   * Generates a Java entity file based on the provided {@link EntityClassMetadata}. The file is
   * written to the specified output path.
   *
   * @param metadata   the metadata containing information about the entity to be generated.
   * @param outputPath the directory path where the generated entity file will be saved.
   * @throws IOException if an error occurs while writing the entity file.
   */
  private static void generateEntityFile(EntityClassMetadata metadata, String outputPath) throws IOException {
    TypeSpec entityClass = ClassSource
      .createClass(metadata.getName())
      .addAnnotations(metadata.getAnnotations())
      .addFields(metadata.getFields())
      .withNoArgConstructor(metadata.getIsNoArgsConstructorNeeded())
      .build();
    JavaFile javaFile = JavaFile.builder(metadata.getPackageName(), entityClass).build();
    javaFile.writeTo(Path.of(outputPath));
  }

  /**
   * Fetches the names of all tables in the database.
   *
   * @param connection the {@link Connection} object used to connect to the database.
   * @return a list of table names in the database.
   * @throws SQLException if an error occurs while fetching the table names.
   */
  private static List<String> fetchAllTableNames(Connection connection) throws SQLException {
    List<String> tableNames = new ArrayList<>();
    String query = """
          SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = 'public'
            AND table_type = 'BASE TABLE';
      """;

    try (PreparedStatement statement = connection.prepareStatement(query)) {
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        tableNames.add(resultSet.getString("table_name"));
      }
    }

    return tableNames;
  }

  /**
   * Fetches the metadata of a specific table, including column details, primary keys, foreign keys,
   * and constraints.
   *
   * @param packageName the package name to assign to the generated class
   * @param tableName   the name of the table for which the metadata is to be fetched.
   * @param connection  the {@link Connection} object used to connect to the database.
   * @return an {@link EntityClassMetadata} object containing the metadata of the table.
   * @throws SQLException if an error occurs while fetching the table metadata.
   */
  private static EntityClassMetadata fetchTableMetadata(
    String packageName,
    String tableName,
    Connection connection
  ) throws SQLException {
    TypeName primaryKeyType = null;
    List<EntityFieldMetadata> fields = new ArrayList<>();
    Map<String, Boolean> childEntities = findChildEntities(tableName, connection);
    String columnQuery = """
          SELECT
              c.column_name,
              c.data_type,
              c.is_nullable,
              c.character_maximum_length,
              tc.constraint_type,
              ccu.table_name AS referenced_table,
              ccu.column_name AS referenced_column,
              EXISTS (
                  SELECT 1
                  FROM pg_catalog.pg_index i
                  JOIN pg_catalog.pg_attribute a
                      ON a.attnum = ANY(i.indkey)
                      AND a.attrelid = i.indrelid
                  WHERE i.indrelid = (quote_ident(c.table_schema) || '.' || quote_ident(c.table_name))::regclass
                  AND i.indisunique
                  AND a.attname = c.column_name
              ) AS is_unique
          FROM information_schema.columns c
          LEFT JOIN information_schema.key_column_usage kcu
              ON c.table_name = kcu.table_name AND c.column_name = kcu.column_name
          LEFT JOIN information_schema.table_constraints tc
              ON kcu.table_name = tc.table_name AND kcu.constraint_name = tc.constraint_name
          LEFT JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
          WHERE c.table_name = ?
          ORDER BY c.ordinal_position;
      """;

    // Fetch columns and metadata
    try (PreparedStatement statement = connection.prepareStatement(columnQuery)) {
      statement.setString(1, tableName);
      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        String columnName = resultSet.getString("column_name");
        String dataType = resultSet.getString("data_type");
        boolean isNullable = resultSet.getString("is_nullable").equalsIgnoreCase("YES");
        String constraintType = resultSet.getString("constraint_type");
        String referencedTable = resultSet.getString("referenced_table");
        String referencedColumn = resultSet.getString("referenced_column");
        Integer columnLength = resultSet.getObject("character_maximum_length", Integer.class);
        boolean isUnique = resultSet.getBoolean("is_unique");

        boolean isPrimaryKey = "PRIMARY KEY".equals(constraintType);
        boolean isForeignKey = "FOREIGN KEY".equals(constraintType);

        TypeName fieldType = isForeignKey ? mapToEntityClass(packageName, referencedTable) : TypeName
          .get(mapToJavaType(dataType));

        if (isPrimaryKey) {
          primaryKeyType = fieldType;
        }

        EntityFieldMetadata entityFieldMetadata = new EntityFieldMetadata(
          isPrimaryKey ? "id" : CaseFormat.LOWER_UNDERSCORE
            .to(
              CaseFormat.LOWER_CAMEL,
              isForeignKey ? referencedTable : columnName
            ),
          fieldType,
          createFieldAnnotations(
            columnName,
            isNullable,
            isPrimaryKey,
            isForeignKey,
            columnLength,
            referencedColumn,
            isUnique
          )
        );

        fields.add(entityFieldMetadata);
      }
    }

    // Add fields for child entities
    for (Map.Entry<String, Boolean> child : childEntities.entrySet()) {
      fields.add(createRelationshipField(packageName, tableName, child.getKey(), child.getValue()));
    }

    return new EntityClassMetadata(packageName, capitalize(tableName), fields, tableName, primaryKeyType);
  }

  /**
   * Retrieves child entities (tables that reference the given table via a foreign key) and
   * determines the uniqueness of each relationship.
   *
   * <p>This method identifies child tables in a database schema that reference the specified table
   * through foreign key constraints. It also checks whether the relationship is unique, indicating
   * a one-to-one relationship instead of a one-to-many relationship.
   *
   * @param parentTable the name of the parent table to find child entities for.
   * @param connection  the {@link Connection} object used to execute the query.
   * @return a map where the key is the child table name and the value is a {@code Boolean}
   *         indicating whether the relationship is unique ({@code true} for one-to-one, {@code false}
   *         for one-to-many).
   * @throws SQLException if an error occurs while querying the database.
   */
  private static Map<String, Boolean> findChildEntities(String parentTable, Connection connection) throws SQLException {
    Map<String, Boolean> childEntities = new HashMap<>();
    String query = """
          SELECT
              tc.table_name AS child_table,
              EXISTS (
                  SELECT 1
                  FROM pg_catalog.pg_index i
                  JOIN pg_catalog.pg_attribute a
                      ON a.attnum = ANY(i.indkey)
                      AND a.attrelid = i.indrelid
                  WHERE i.indrelid = (quote_ident(tc.table_schema) || '.' || quote_ident(tc.table_name))::regclass
                  AND i.indisunique
                  AND a.attname = kcu.column_name
              ) AS is_unique
          FROM information_schema.table_constraints AS tc
          LEFT JOIN information_schema.constraint_column_usage AS ccu
              ON tc.constraint_name = ccu.constraint_name
          LEFT JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_name = kcu.table_name
          WHERE tc.constraint_type = 'FOREIGN KEY' AND ccu.table_name = ?
      """;

    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, parentTable);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          String childTable = resultSet.getString("child_table");
          boolean isUnique = resultSet.getBoolean("is_unique");
          childEntities.put(childTable, isUnique);
        }
      }
    }

    return childEntities;
  }

  /**
   * Creates a list of annotations for the specified column based on its metadata.
   *
   * @param columnName       the name of the column.
   * @param isNullable       whether the column is nullable.
   * @param isPrimaryKey     whether the column is a primary key.
   * @param isForeignKey     whether the column is a foreign key.
   * @param columnLength     the maximum length of the column, if applicable.
   * @param referencedColumn the column in the referenced table, if it is a foreign key.
   * @param isUnique         whether the column has a unique constraint.
   * @return a list of {@link AnnotationSpec} objects for the column.
   */
  private static List<AnnotationSpec> createFieldAnnotations(
    String columnName,
    boolean isNullable,
    boolean isPrimaryKey,
    boolean isForeignKey,
    Integer columnLength,
    String referencedColumn,
    boolean isUnique
  ) {
    List<AnnotationSpec> annotations = new ArrayList<>();

    // Add @Id annotation if the field is a primary key
    if (isPrimaryKey) {
      annotations.add(createIdAnnotation());
      annotations.add(createGeneratedValueAnnotation());
    }

    if (isForeignKey) {
      if (isUnique) {
        // Add @OneToOne annotation if the foreign key is unique
        annotations.add(AnnotationSpec.builder(OneToOne.class).build());
      } else {
        // Add @ManyToOne annotation if the foreign key is not unique
        annotations.add(AnnotationSpec.builder(ManyToOne.class).build());
      }

      annotations
        .add(
          AnnotationSpec
            .builder(JoinColumn.class)
            .addMember("name", "$S", columnName)
            .addMember("referencedColumnName", "$S", referencedColumn)
            .build()
        );
    } else {
      // Add @Column annotation
      annotations.add(createColumnAnnotation(columnName, isNullable, columnLength));
    }

    return annotations;
  }

  /**
   * Creates a metadata field for a bidirectional relationship between two entities.
   *
   * <p>This method generates the field in the parent entity representing the child entity/entities.
   * Depending on whether the relationship is unique, it configures either a {@code @OneToOne} or a
   * {@code @OneToMany} annotation. The {@code mappedBy} attribute specifies the field in the child
   * entity that defines the inverse relationship to the parent entity.
   *
   * @param packageName the package name to assign to the generated class
   * @param parentTable the name of the parent table in the database, in snake_case.
   * @param childTable  the name of the child table in the database, in snake_case.
   * @param isUnique    {@code true} if the relationship is unique (e.g., one-to-one); {@code false} if
   *                    the relationship involves multiple child entities (one-to-many).
   * @return an {@link EntityFieldMetadata} object representing the relationship, including the
   *         field name, its type, and annotations.
   */
  private static EntityFieldMetadata createRelationshipField(
    String packageName,
    String parentTable,
    String childTable,
    boolean isUnique
  ) {
    // Convert table names from snake_case to camelCase
    String formattedParentTable = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, parentTable);
    String formattedChildTable = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, childTable);

    // Define the field name based on relationship type
    String fieldName = formattedChildTable + (isUnique ? "" : "Set");

    // Determine the field type: single entity for @OneToOne or Set for @OneToMany
    var fieldType = isUnique ? mapToEntityClass(packageName, childTable) : ParameterizedTypeName
      .get(ClassName.get(Set.class), mapToEntityClass(packageName, childTable));

    // Select the appropriate annotation
    AnnotationSpec.Builder annotation = isUnique ? AnnotationSpec.builder(OneToOne.class) : AnnotationSpec
      .builder(OneToMany.class)
      .addMember("fetch", "$T.LAZY", FetchType.class);

    annotation.addMember("mappedBy", "$S", formattedParentTable);

    return new EntityFieldMetadata(fieldName, fieldType, List.of(annotation.build()));
  }

  /**
   * Maps a database type to its corresponding Java type.
   *
   * @param dataType the database column type.
   * @return the corresponding Java type.
   */
  private static Class<?> mapToJavaType(String dataType) {
    return switch (dataType) {
      case "boolean" -> Boolean.class;
      case "smallint", "smallserial" -> Short.class;
      case "integer", "serial" -> Integer.class;
      case "bigint", "bigserial" -> Long.class;
      case "text", "character varying" -> String.class;
      case "numeric", "money" -> BigDecimal.class;
      case "real" -> Float.class;
      case "double precision" -> Double.class;
      case "date" -> LocalDate.class;
      case "time" -> LocalTime.class;
      case "timestamp", "time without time zone", "timestamp without time zone" -> Instant.class;
      default -> Object.class;
    };
  }

  /**
   * Maps a referenced table name to its corresponding Java entity class.
   *
   * <p>This method takes the name of a referenced table in the database and converts it to the
   * appropriate Java entity class name by capitalizing the table name and converting it from
   * snake_case to UpperCamelCase. The resulting class name is used to reference the related entity
   * in the generated code.
   *
   * @param packageName     the package name to assign to the generated class.
   * @param referencedTable the name of the referenced table in the database (in snake_case).
   * @return a {@link ClassName} representing the corresponding Java entity class for the referenced
   *         table.
   * @throws IllegalArgumentException if the referenced table name is null or blank.
   */
  private static ClassName mapToEntityClass(String packageName, String referencedTable) {
    if (referencedTable == null || referencedTable.isBlank()) {
      throw new IllegalArgumentException("Referenced table name cannot be null or blank");
    }

    String className = capitalize(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, referencedTable));
    return ClassName.get(packageName, className);
  }
}
