package com.indix.gocd.utils.mocks;

import io.jmnarloch.cd.go.plugin.api.executor.ExecutionContext;

import java.util.Map;

public class MockTaskExecutionContext extends ExecutionContext {
    private Map<String, String> mockEnvironmentVariables;

    public MockTaskExecutionContext(Map mockEnvironmentVariables) {
        super(mockEnvironmentVariables);
        this.mockEnvironmentVariables = mockEnvironmentVariables;

    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return mockEnvironmentVariables;
    }

    @Override
    public String getWorkingDirectory() {
        return ".";
    }
}
