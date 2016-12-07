package com.indix.gocd.s3fetch;

import io.jmnarloch.cd.go.plugin.api.config.AnnotatedEnumConfigurationProvider;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcher;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcherBuilder;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionConfiguration;
import io.jmnarloch.cd.go.plugin.api.task.AbstractDispatchingTask;
import io.jmnarloch.cd.go.plugin.api.validation.AbstractTaskValidator;
import io.jmnarloch.cd.go.plugin.api.validation.ValidationErrors;
import org.apache.commons.lang3.StringUtils;
import io.jmnarloch.cd.go.plugin.api.view.

import java.util.Map;

public class FetchTask2 extends AbstractDispatchingTask {
    @Override
    protected ApiRequestDispatcher buildDispatcher() {
        return ApiRequestDispatcherBuilder.dispatch()
                .toConfiguration(new AnnotatedEnumConfigurationProvider<>(FetchTaskConfig.class))
                .toValidator(new FetchTaskValidator())
                .toView(new FetchTaskView())
                .toExecutor(new FetchExecutor())
                .build();
    }
}

class FetchTaskConfig {

}

class FetchTaskValidator extends AbstractTaskValidator {

    public static final String REPO = "Repo";
    public static final String PACKAGE = "Package";
    public static final String DESTINATION = "Destination";

    @Override
    public void validate(Map<String, Object> properties, ValidationErrors errors) {
        if (StringUtils.isBlank((String)properties.get(REPO))) {
            errors.addError(REPO, "S3 repository must be specified");
        }

        if (StringUtils.isBlank((String)properties.get(PACKAGE))) {
            errors.addError(PACKAGE, "S3 package must be specified");
        }

        if (StringUtils.isBlank((String)properties.get(DESTINATION))) {
            errors.addError(DESTINATION, "Destination directory must be specified");
        }
    }

}

class FetchTaskView extends AbstractTaskView{

    public FetchTaskView() {

    }

}