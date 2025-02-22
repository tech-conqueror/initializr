package com.techconqueror.initializr.core.utility;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class OpenAPIUtils {

  public static String getSimpleRef(String ref) {
    if (ref == null) {
      return null;
    } else if (ref.startsWith("#/components/")) {
      ref = ref.substring(ref.lastIndexOf("/") + 1);
    } else if (ref.startsWith("#/definitions/")) {
      ref = ref.substring(ref.lastIndexOf("/") + 1);
    } else {
      return null;
    }

    ref = URLDecoder.decode(ref, StandardCharsets.UTF_8);

    // see https://tools.ietf.org/html/rfc6901#section-3
    // Because the characters '~' (%x7E) and '/' (%x2F) have special meanings in
    // JSON Pointer, '~' needs to be encoded as '~0' and '/' needs to be encoded
    // as '~1' when these characters appear in a reference token.
    // This reverses that encoding.
    ref = ref.replace("~1", "/").replace("~0", "~");

    return ref;
  }

  private static Schema<?> getSchema(OpenAPI openAPI, String schemaName) {
    return openAPI.getComponents().getSchemas().get(schemaName);
  }

  public static TypeName mapSchemaToJavaType(String packageName, OpenAPI openAPI, Schema<?> schema) {
    String ref = getSimpleRef(schema.get$ref());
    String type = (schema.get$ref() != null ? getSchema(openAPI, ref) : schema)
      .getTypes()
      .stream()
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No types available in schema"));

    return switch (type) {
      case "string" -> TypeName.get(String.class);
      case "integer" -> TypeName.get(Integer.class);
      case "number" -> TypeName.get(Double.class);
      case "boolean" -> TypeName.get(Boolean.class);
      case "array" -> {
        Schema<?> itemsSchema = schema.getItems();
        TypeName itemType = itemsSchema != null ? mapSchemaToJavaType(packageName, openAPI, itemsSchema) : TypeName
          .get(Object.class);
        yield ParameterizedTypeName.get(ClassName.get(ArrayList.class), itemType);
      }
      case "object" -> ClassName.get(packageName + ".controller.dto", ref);
      default -> TypeName.get(Object.class);
    };
  }

  public static TypeName mapSchemaToJavaType2(String packageName, OpenAPI openAPI, Schema<?> schema) {
    String ref = getSimpleRef(schema.get$ref());
    String type = (schema.get$ref() != null ? getSchema(openAPI, ref) : schema)
      .getTypes()
      .stream()
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No types available in schema"));

    return switch (type) {
      case "string" -> TypeName.get(String.class);
      case "integer" -> TypeName.get(Integer.class);
      case "number" -> TypeName.get(Double.class);
      case "boolean" -> TypeName.get(Boolean.class);
      case "array", "object" -> ClassName.get(packageName + ".controller.dto", ref);
      default -> TypeName.get(Object.class);
    };
  }
}
