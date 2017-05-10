package com.indix.gocd.s3publish.utils;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.indix.gocd.s3publish.Config;
import com.indix.gocd.s3publish.PublishExecutor;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Maps;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;

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

        Context context = getTaskExecutionContext(localFileToUpload);
        List<SourceDestination> sourceDestinations = Lists.of(new SourceDestination(localFileToUpload.getName(), destination));
        Map configMap = Maps.<String, String>builder()
                .with(SOURCEDESTINATIONS, sourceDestinations(sourceDestinations))
                .build();
        Config config = new Config(configMap);
        TaskExecutionResult result = new PublishExecutor().execute(config, context);

        if (!result.isSuccessful()) {
            System.err.println(result.message());
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

    private static Context getTaskExecutionContext(final File localFileToUpload) {
        return new Context(Maps.<String, String>builder().build()) {
            @Override
            public void printMessage(String message) {
            }

            @Override
            public void printEnvironment() {
            }

            @Override
            public Map getEnvironmentVariables() {
                return super.getEnvironmentVariables();
            }

            @Override
            public String getWorkingDir() {
                return localFileToUpload.getParentFile().getAbsolutePath();
            }
        };

    }

}

