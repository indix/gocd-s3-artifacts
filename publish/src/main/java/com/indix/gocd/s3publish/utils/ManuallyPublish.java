package com.indix.gocd.s3publish.utils;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.indix.gocd.s3publish.PublishExecutor;
import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionConfiguration;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionContext;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionResult;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Utility to manually publish an artifact (if required) without being in Go.

Run this Task with the following ENV variables
- AWS_USE_IAM_ROLE OR
- AWS_ACCESS_KEY_ID AND
- AWS_SECRET_ACCESS_KEY
- GO_ARTIFACTS_S3_BUCKET
- GO_SERVER_URL
- GO_PIPELINE_NAME
- GO_PIPELINE_COUNTER
- GO_STAGE_NAME
- GO_STAGE_COUNTER
- GO_JOB_NAME

 */
public class ManuallyPublish {
    public static void main(String[] args) throws JSONException {
        if (args.length < 2) {
            String usage = "ManuallyPublish [localFileToUpload] [destinationOnS3]";
            System.out.println(usage);
            System.exit(1);
        }
        final File localFileToUpload = new File(args[0]);
        String destination = args[1];

        ExecutionContext taskExecutionContext = getTaskExecutionContext(localFileToUpload);
        List<SourceDestination> sourceDestinations = Lists.of(new SourceDestination(localFileToUpload.getName(), destination));
        Map<String, Object> config = new HashMap<>();
        config.put(Constants.SOURCEDESTINATIONS, sourceDestinations(sourceDestinations));
        ExecutionConfiguration taskConfig = new ExecutionConfiguration(config);

        ExecutionResult executionResult = new PublishExecutor().execute(taskExecutionContext, taskConfig, getConsole());

        if (!executionResult.isSuccess()) {
            System.err.println(executionResult.getMessage());
            System.exit(1);
        }
    }

    public static String sourceDestinations(List<SourceDestination> sourceDestinations) throws JSONException {
        JSONObject jsonObject = new JSONObject(sourceDestinations.get(0));
        System.out.println(jsonObject.toString());
        JSONArray jsonArray = new JSONArray(sourceDestinations);
        String s = jsonArray.toString();
        System.out.println(s);
        return s;
    }

    private static ExecutionContext getTaskExecutionContext(final File localFileToUpload) {
        return new ExecutionContext(Maps.<String, String>builder().build()) {
            @Override
            public Map<String, String> getEnvironmentVariables() {
                return Maps.<String, String>builder().build();
            }

            @Override
            public String getWorkingDirectory() {
                return localFileToUpload.getParentFile().getAbsolutePath();
            }
        };
    }

    private static JobConsoleLogger getConsole() {
        return new JobConsoleLogger() {
            @Override
            public void printLine(String s) {
            }

            @Override
            public void readErrorOf(InputStream inputStream) {
            }

            @Override
            public void readOutputOf(InputStream inputStream) {
            }

            @Override
            public void printEnvironment(Map<String, String> map) {
                context.console().printEnvironment(map, defaultSecureEnvSpecifier());
            }
        };
    }

    private static Console.SecureEnvVarSpecifier defaultSecureEnvSpecifier() {
        return new Console.SecureEnvVarSpecifier() {
            @Override
            public boolean isSecure(String s) {
                return false;
            }
        };
    }
}

