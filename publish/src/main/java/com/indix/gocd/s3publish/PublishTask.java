package com.indix.gocd.s3publish;

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
public class PublishTask implements Task {
    public static final String GO_ARTIFACTS_S3_BUCKET = "GO_ARTIFACTS_S3_BUCKET";
    public static final String SOURCE = "source";
    public static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
    public static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";

    @Override
    public TaskConfig config() {
        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty(SOURCE);
        return taskConfig;
    }

    @Override
    public TaskExecutor executor() {
        return new PublishExecutor();
    }

    @Override
    public TaskView view() {
        return new TaskView() {
            @Override
            public String displayValue() {
                return "Publish To S3";
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
        if (StringUtils.isEmpty(taskConfig.getValue(SOURCE))) {
            validationResult.addError(new ValidationError(SOURCE, "Source files to publish not present"));
        }

        return validationResult;
    }
}
