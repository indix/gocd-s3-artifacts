package com.indix.gocd.utils;


import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import java.nio.file.Paths;
import java.util.Map;

public class Context {
    private final Map environmentVariables;
    private final String workingDir;
    private final JobConsoleLogger console;

    public Context(Map context) {
        environmentVariables = (Map) context.get("environmentVariables");
        workingDir = (String) context.get("workingDirectory");
        console = new JobConsoleLogger() {};
    }

    public void printMessage(String message) {
        console.printLine(message);
    }

    public void printEnvironment() {
        console.printEnvironment(environmentVariables);
    }

    public Map getEnvironmentVariables() {
        return environmentVariables;
    }

    public String getPipelineLabel() {
        return environmentVariables.get("GO_PIPELINE_LABEL").toString();
    }

    public String getPipelineName() {
        return environmentVariables.get("GO_PIPELINE_NAME").toString();
    }

    public String getStageName() {
        return environmentVariables.get("GO_STAGE_NAME").toString();
    }

    public String getJobName() {
        return environmentVariables.get("GO_JOB_NAME").toString();
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getAbsoluteWorkingDir() {
        return Paths.get("").toAbsolutePath().resolve(workingDir).toString();
    }
}
