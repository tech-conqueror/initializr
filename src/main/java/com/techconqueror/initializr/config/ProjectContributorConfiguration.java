package com.techconqueror.initializr.config;

import com.techconqueror.initializr.contributor.CodeStyleFileProjectContributor;
import com.techconqueror.initializr.contributor.ExceptionFileProjectContributor;
import com.techconqueror.initializr.contributor.NLayerFileProjectContributor;
import io.spring.initializr.generator.buildsystem.maven.MavenBuild;
import io.spring.initializr.generator.buildsystem.maven.MavenBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnPackaging;
import io.spring.initializr.generator.packaging.jar.JarPackaging;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerationConfiguration;
import io.spring.initializr.generator.spring.build.BuildCustomizer;
import org.springframework.context.annotation.Bean;

@ProjectGenerationConfiguration
public class ProjectContributorConfiguration {

  @Bean
  public CodeStyleFileProjectContributor codeStyleFileProjectContributor() {
    return new CodeStyleFileProjectContributor();
  }

  @Bean
  public ExceptionFileProjectContributor exceptionFileProjectContributor(ProjectDescription projectDescription) {
    return new ExceptionFileProjectContributor(projectDescription);
  }

  @Bean
  public NLayerFileProjectContributor layerFileProjectContributor(ProjectDescription projectDescription) {
    return new NLayerFileProjectContributor(projectDescription);
  }

  @Bean
  @ConditionalOnBuildSystem(MavenBuildSystem.ID)
  @ConditionalOnPackaging(JarPackaging.ID)
  public BuildCustomizer<MavenBuild> mavenBuildCustomizer() {
    return (build) -> build.plugins().add("com.diffplug.spotless", "spotless-maven-plugin", (plugin) -> {
      plugin.version("2.43.0"); // Specify the Spotless plugin version
      plugin.configuration((configuration) -> {
        // Add configuration for the <pom> element
        configuration.add("pom", (pom) -> {
          pom.add("sortPom", (sortPom) -> {
            sortPom.add("expandEmptyElements", "false");
            sortPom.add("spaceBeforeCloseEmptyElement", "true");
            sortPom.add("sortDependencies", "scope");
          });
        });
        // Add configuration for the <java> element
        configuration.add("java", (java) -> {
          java.add("eclipse", (eclipse) -> {
            eclipse.add("file", "${project.basedir}/code-style.xml");
          });
//          java.add("palantirJavaFormat", (palantirJavaFormat) -> {
//            palantirJavaFormat.add("version", "2.47.0");
//          });
//          java.add("importOrder", (importOrder) -> {
//            importOrder.add("order", "#,");
//            importOrder.add("wildcardsLast", "true");
//            importOrder.add("semanticSort", "true");
//          });
        });
      });
    });
  }
}
