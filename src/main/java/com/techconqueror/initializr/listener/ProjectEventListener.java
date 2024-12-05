package com.techconqueror.initializr.listener;

import io.spring.initializr.generator.project.ProjectDirectoryFactory;
import io.spring.initializr.web.project.ProjectGeneratedEvent;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEventListener.class);

    private final ApplicationContext applicationContext;

    public ProjectEventListener(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void handleProjectGeneratedEvent(ProjectGeneratedEvent event) {
        var projectDirectoryFactory = applicationContext.getBean(ProjectDirectoryFactory.class);
        LOGGER.info("Project generated: {}", projectDirectoryFactory);

        var cmd = new CommandLine("./mvnw.cmd");
        cmd.addArgument("spotless:apply");

        var executor = new DefaultExecutor();
        executor.setWorkingDirectory(new java.io.File("C:\\Users\\nmman\\Desktop\\dev\\project"));

        try {
            var exitCode = executor.execute(cmd);
            LOGGER.info("Process exited with code: {}", exitCode);
        } catch (Exception e) {
            LOGGER.error("Error occurred while executing the command", e);
        }
    }
}
