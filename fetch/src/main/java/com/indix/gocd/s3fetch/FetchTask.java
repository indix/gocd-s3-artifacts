package com.indix.gocd.s3fetch;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;


@Extension
public class FetchTask implements Task {
    public static final String REPO = "Repo";
    public static final String PACKAGE = "Package";
    public static final String DESTINATION = "Destination";

    @Override
    public TaskConfig config() {
        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty(REPO);
        taskConfig.addProperty(PACKAGE);
        taskConfig.addProperty(DESTINATION);
        return taskConfig;
    }

    @Override
    public TaskExecutor executor() {
        return new FetchExecutor();
    }

    @Override
    public TaskView view() {
        return new TaskView() {
            @Override
            public String displayValue() {
                return "Fetch S3 package";
            }

            @Override
            public String template() {
                try {
                    return IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Error happened during rendering - " + e.getMessage();
                }
            }
        };
    }

    @Override
    public ValidationResult validate(TaskConfig taskConfig) {
        ValidationResult validationResult = new ValidationResult();
        if (StringUtils.isBlank(taskConfig.getValue(REPO))) {
            validationResult.addError(new ValidationError(REPO, "S3 repository must be specified"));
        }

        if (StringUtils.isBlank(taskConfig.getValue(PACKAGE))) {
            validationResult.addError(new ValidationError(PACKAGE, "S3 package must be specified"));
        }

        if (StringUtils.isBlank(taskConfig.getValue(DESTINATION))) {
            validationResult.addError(new ValidationError(DESTINATION, "Destination directory must be specified"));
        }

        return validationResult;
    }
}
