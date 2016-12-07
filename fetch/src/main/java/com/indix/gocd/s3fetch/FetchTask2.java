package com.indix.gocd.s3fetch;

import io.jmnarloch.cd.go.plugin.api.config.AnnotatedEnumConfigurationProvider;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcher;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcherBuilder;
import io.jmnarloch.cd.go.plugin.api.task.AbstractDispatchingTask;
import io.jmnarloch.cd.go.plugin.api.validation.AbstractTaskValidator;
import io.jmnarloch.cd.go.plugin.api.validation.ValidationErrors;

import java.util.Map;

public class FetchTask2 extends AbstractDispatchingTask {
    @Override
    protected ApiRequestDispatcher buildDispatcher() {
        return ApiRequestDispatcherBuilder.dispatch()
                .toConfiguration(new AnnotatedEnumConfigurationProvider<>(FetchTaskConfig.class))
                .toValidator(new FetchTaskValidator())
                .toView(new FetchTaskView())
                .toExecutor(new FetchTaskExecutor())
                .build();
    }
}

class FetchTaskConfig {

}

class FetchTaskValidator extends AbstractTaskValidator {

    @Override
    public void validate(Map<String, Object> properties, ValidationErrors errors) {

    }

}

class FetchTaskView {

    public FetchTaskView() {

    }

}

class FetchTaskExecutor {

}