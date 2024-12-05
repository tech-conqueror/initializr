package com.techconqueror.initializr.contributor;

import static com.techconqueror.initializr.core.hibernate.HibernateEntityGenerator.generateEntitiesForAllTables;

import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class LayerFileProjectContributor implements ProjectContributor {

    private final ProjectDescription projectDescription;

    public LayerFileProjectContributor(ProjectDescription projectDescription) {
        this.projectDescription = projectDescription;
    }

    @Override
    public void contribute(Path projectRoot) {
        String dbUrl = "jdbc:postgresql://34.87.149.96:32069/postgres";
        String username = "b049090";
        String password = "pwd";

        try (Connection connection = DriverManager.getConnection(dbUrl, username, password)) {
            generateEntitiesForAllTables(
                    projectDescription.getPackageName(),
                    projectRoot.resolve("src/main/java").toString(),
                    connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
