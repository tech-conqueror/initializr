package com.techconqueror.initializr.core.utility;

import com.palantir.javapoet.ClassName;

public class ClassUtils {

  public static String classNameToVariableName(ClassName className) {
    return classNameToVariableName(className.simpleName());
  }

  public static String classNameToVariableName(String className) {
    return StringUtils.decapitalize(className);
  }
}
