package com.techconqueror.initializr.core.hibernate;

import com.squareup.javapoet.TypeName;
import com.techconqueror.initializr.core.java.ClassMetadata;
import com.techconqueror.initializr.core.java.FieldMetadata;
import java.util.List;

/**
 * Represents metadata for an entity class, extending the {@link ClassMetadata} class to include
 * additional Hibernate-specific information such as the table name.
 * <p>
 * This class is designed to store and manage information required for generating Hibernate entity classes,
 * including the package name, name, fields, whether a no-argument constructor is needed, and the corresponding table name.
 * </p>
 */
public class EntityClassMetadata extends ClassMetadata {

    /**
     * The name of the database table associated with this entity.
     */
    private String tableName;

    /**
     * The Java type of the primary key field for the entity.
     */
    private TypeName primaryKeyType;

    /**
     * Constructs a new {@code EntityClassMetadata} instance with the specified parameters.
     *
     * @param packageName the package name of the entity class.
     * @param name        the name of the entity class.
     * @param fields      a list of {@link FieldMetadata} objects representing the fields of the entity.
     * @param tableName   the name of the database table associated with this entity.
     * @param primaryKeyType the Java type of the primary key field.
     */
    public EntityClassMetadata(
            String packageName,
            String name,
            List<? extends FieldMetadata> fields,
            String tableName,
            TypeName primaryKeyType) {
        super(packageName, name, true, fields);
        this.tableName = tableName;
        this.primaryKeyType = primaryKeyType;
    }

    /**
     * Returns the name of the database table associated with this entity.
     *
     * @return the table name.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the name of the database table associated with this entity.
     *
     * @param tableName the table name to set.
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the Java type of the primary key field for the entity.
     *
     * @return the primary key type.
     */
    public TypeName getPrimaryKeyType() {
        return primaryKeyType;
    }

    /**
     * Sets the Java type of the primary key field for the entity.
     *
     * @param primaryKeyType the primary key type to set.
     */
    public void setPrimaryKeyType(TypeName primaryKeyType) {
        this.primaryKeyType = primaryKeyType;
    }
}
