package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONException;
import com.google.gson.GsonBuilder;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.Result;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Tuple2;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static com.indix.gocd.utils.utils.Lists.foreach;

@Extension
public class PublishTask implements GoPlugin {

    Logger logger = Logger.getLoggerFor(PublishTask.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if ("configuration".equals(request.requestName())) {
            return handleGetConfigRequest();
        } else if ("validate".equals(request.requestName())) {
            return handleValidation(request);
        } else if ("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        } else if ("view".equals(request.requestName())) {
            return handleTaskView();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map view = new HashMap();
        view.put("displayValue", "Publish To S3");
        try {
            view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        } catch (Exception e) {
            responseCode = DefaultGoPluginApiResponse.INTERNAL_ERROR;
            String errorMessage = "Failed to find template: " + e.getMessage();
            view.put("exception", errorMessage);
            logger.error(errorMessage, e);
        }
        return createResponse(responseCode, view);
    }

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        PublishExecutor executor = new PublishExecutor();
        Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map config = (Map) executionRequest.get("config");
        Map context = (Map) executionRequest.get("context");

        Result result = executor.execute(new Config(config), new Context(context));
        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        final HashMap validationResult = new HashMap();
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        final Config config = new Config(configMap);

        try {
            List<Tuple2<String, String>> sourceDestinations = config.sourceDestinations();
            if(sourceDestinations.isEmpty()) {
                HashMap errorMap = new HashMap();
                errorMap.put(SOURCEDESTINATIONS, "At least one source must be specified");
                validationResult.put("errors", errorMap);
            }
            foreach(sourceDestinations, new Functions.VoidFunction<Tuple2<String, String>>() {
                @Override
                public void execute(Tuple2<String, String> input) {
                    if(StringUtils.isBlank(input._1())) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(SOURCEDESTINATIONS, "Source cannot be empty");
                        validationResult.put("errors", errorMap);
                    }
                }
            });
        } catch (JSONException e) {
            HashMap errorMap = new HashMap();
            errorMap.put(SOURCEDESTINATIONS, "Error while parsing configuration");
            validationResult.put("errors", errorMap);
            responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
        }

        return createResponse(responseCode, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();
        HashMap sourceDestinations = new HashMap();
        sourceDestinations.put("default-value", "");
        sourceDestinations.put("required", true);
        config.put(SOURCEDESTINATIONS, sourceDestinations);

        HashMap destinationPrefix = new HashMap();
        destinationPrefix.put("default-value", "");
        destinationPrefix.put("required", false);
        config.put(DESTINATION_PREFIX, destinationPrefix);

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }

    private GoPluginApiResponse createResponse(int responseCode, Map body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }
}
