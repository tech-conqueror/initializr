package com.techconqueror.initializr.core.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

  public static String capitalize(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  public static String decapitalize(String input) {
    if (input == null || input.isEmpty()) {
      return input; //Return input as-is if it's null or empty
    }

    return input.substring(0, 1).toLowerCase() + input.substring(1);
  }

  public static String singularize(String plural) {
    if (plural.endsWith("ies")) {
      return plural.substring(0, plural.length() - 3) + "y"; // e.g., "categories" → "category"
    } else if (plural.endsWith("oes") || plural.endsWith("ses") || plural.endsWith("xes")) {
      return plural.substring(0, plural.length() - 2); // e.g., "heroes" → "hero"
    } else if (plural.endsWith("s") && !plural.endsWith("ss")) {
      return plural.substring(0, plural.length() - 1); // e.g., "users" → "user"
    }

    return plural; //If no change needed, return as is
  }

  public static String format(String template, Map<String, Object> parameters) {
    StringBuilder newTemplate = new StringBuilder(template);
    List<Object> valueList = new ArrayList<>();

    Matcher matcher = Pattern.compile("[$][{](\\w+)}").matcher(template);

    while (matcher.find()) {
      String key = matcher.group(1);

      String paramName = "${" + key + "}";
      int index = newTemplate.indexOf(paramName);
      if (index != -1) {
        newTemplate.replace(index, index + paramName.length(), "%s");
        valueList.add(parameters.get(key));
      }
    }

    return String.format(newTemplate.toString(), valueList.toArray());
  }
}
