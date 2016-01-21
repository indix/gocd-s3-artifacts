package com.indix.gocd.s3publish.utils;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.indix.gocd.s3publish.PublishExecutor;
import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/*
Utility to manually publish an artifact (if required) without being in Go.

Run this Task with the following ENV variables
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
(or)
-- AWS_USE_IAM_ROLE
and
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

        TaskExecutionContext taskExecutionContext = getTaskExecutionContext(localFileToUpload);
        TaskConfig taskConfig = new TaskConfig();
        List<SourceDestination> sourceDestinations = Lists.of(new SourceDestination(localFileToUpload.getName(), destination));
        taskConfig.add(new Property(Constants.SOURCEDESTINATIONS, sourceDestinations(sourceDestinations), "null"));

        ExecutionResult executionResult = new PublishExecutor().execute(taskConfig, taskExecutionContext);

        if (!executionResult.isSuccessful()) {
            System.err.println(executionResult.getMessagesForDisplay());
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

    private static TaskExecutionContext getTaskExecutionContext(final File localFileToUpload) {
        return new TaskExecutionContext() {
            @Override
            public EnvironmentVariables environment() {
                return defaultEnvironmentVariables();
            }

            @Override
            public Console console() {
                return defaultConsole();
            }

            @Override
            public String workingDir() {
                return localFileToUpload.getParentFile().getAbsolutePath();
            }
        };
    }

    private static Console defaultConsole() {
        return new Console() {
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
            public void printEnvironment(Map<String, String> map, SecureEnvVarSpecifier secureEnvVarSpecifier) {
            }
        };
    }

    private static EnvironmentVariables defaultEnvironmentVariables() {
        return new EnvironmentVariables() {
            @Override
            public Map<String, String> asMap() {
                return Maps.<String, String>builder().build();
            }

            @Override
            public void writeTo(Console console) {
            }

            @Override
            public Console.SecureEnvVarSpecifier secureEnvSpecifier() {
                return defaultSecureEnvSpecifier();
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

