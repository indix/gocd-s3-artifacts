package com.indix.gocd.s3fetch;

import com.google.gson.GsonBuilder;
import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.TaskExecutionResult;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Extension
public class FetchTask implements GoPlugin {

    Logger logger = Logger.getLoggerFor(FetchTask.class);

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

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map configMap = (Map) executionRequest.get("config");
        Map context = (Map) executionRequest.get("context");
        Config config = new Config(configMap);

        FetchExecutor executor = getFetchExecutor(config);

        TaskExecutionResult result = executor.execute(config, new Context(context));
        return createResponse(result.responseCode(), result.toMap());
    }

    private FetchExecutor getFetchExecutor(Config config) {
        String materialType = config.getMaterialType();
        if (materialType.equals("Package")) {
            return new PackageFetchExecutor();
        } else if (materialType.equals("Pipeline")) {
            return new PipelineFetchExecutor();
        } else if (materialType.equals("Self")) {
            return new SelfFetchExecutor();
        } else {
            throw new IllegalStateException("No such material type: " + materialType);
        }
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map view = new HashMap();
        view.put("displayValue", "Fetch from S3");
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

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Config config = new Config(configMap);
        FetchExecutor executor = getFetchExecutor(config);

        final HashMap validationResult = new HashMap();
        Map<String, String> errors = executor.validate(config);
        if (!errors.isEmpty()) {
            validationResult.put("errors", errors);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();

        HashMap materialType = new HashMap();
        materialType.put("default-value", "Package");
        materialType.put("required", true);
        config.put(Constants.MATERIAL_TYPE, materialType);

        HashMap repo = new HashMap();
        repo.put("default-value", "");
        repo.put("required", false);
        config.put(Constants.REPO, repo);

        HashMap pkg = new HashMap();
        pkg.put("default-value", "");
        pkg.put("required", false);
        config.put(Constants.PACKAGE, pkg);

        HashMap material = new HashMap();
        material.put("default-value", "");
        material.put("required", false);
        config.put(Constants.MATERIAL, material);

        HashMap job = new HashMap();
        job.put("default-value", "");
        job.put("required", false);
        config.put(Constants.JOB, job);

        HashMap stage = new HashMap();
        stage.put("default-value", "");
        stage.put("required", false);
        config.put(Constants.STAGE, stage);

        HashMap source = new HashMap();
        source.put("default-value", "");
        source.put("required", false);
        config.put(Constants.SOURCE, source);

        HashMap sourcePrefix = new HashMap();
        sourcePrefix.put("default-value", "");
        sourcePrefix.put("required", false);
        config.put(Constants.SOURCE_PREFIX, sourcePrefix);

        HashMap destination = new HashMap();
        destination.put("default-value", "");
        destination.put("required", false);
        config.put(Constants.DESTINATION, destination);

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
