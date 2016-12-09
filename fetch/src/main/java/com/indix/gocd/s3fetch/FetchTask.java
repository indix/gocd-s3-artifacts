package com.indix.gocd.s3fetch;

import io.jmnarloch.cd.go.plugin.api.config.AnnotatedEnumConfigurationProvider;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcher;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcherBuilder;
import io.jmnarloch.cd.go.plugin.api.task.AbstractDispatchingTask;

public class FetchTask extends AbstractDispatchingTask {
    @Override
    protected ApiRequestDispatcher buildDispatcher() {
        return ApiRequestDispatcherBuilder.dispatch()
                .toConfiguration(new AnnotatedEnumConfigurationProvider<>(FetchConfigEnum.class))
                .toValidator(new FetchTaskValidator())
                .toView(new FetchTaskView("", ""))
                .toExecutor(new FetchExecutor())
                .build();
    }
}