package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.indix.gocd.utils.utils.Tuple2;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static org.apache.commons.lang3.StringUtils.trim;

@Extension
public class PublishTask implements GoPlugin {


    public GoPluginApiResponse handleGetConfigRequest(GoPluginApiRequest request) {
        Map configuration = new HashMap();
        Map config = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        configuration.put(SOURCEDESTINATIONS,config.get(SOURCEDESTINATIONS));
        configuration.put(DESTINATION_PREFIX,config.get(DESTINATION_PREFIX));
        return DefaultGoPluginApiResponse.success(new Gson().toJson(configuration));
    }


    public GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        PublishExecutor executor =  new PublishExecutor();
        Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map config = (Map) executionRequest.get("config");
        Map context = (Map) executionRequest.get("context");

        return executor.execute(request);
    }


    public GoPluginApiResponse view() {
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map view = new HashMap();
        view.put("displayValue", "Publish To S3");
        try {
            view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        } catch (IOException e) {
            responseCode = DefaultGoPluginApiResponse.INTERNAL_ERROR;
            view.put("exception", "Error happened during rendering" + e.getMessage());
        }

        return createResponse(responseCode, view);
    }

    private GoPluginApiResponse createResponse(int responseCode, Map message) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(message));
        return response;
    }


    @Override
    public GoPluginApiResponse handle (GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if("configuration".equals(request.requestName())) {
            return handleGetConfigRequest(request);
        }
        else if("validate".equals(request.requestName())) {
            return handleValidation(request);
        }
        else if("view".equals(request.requestName())) {
            return view();
        }
        else if("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        }

        throw new UnhandledRequestTypeException(request.requestName());
    }

    public GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        Map config = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map errors = new HashMap();
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        List<Tuple2<String, String>> sourceDestinations = null;
        try {
            sourceDestinations = getSourceDestinations(config.get(SOURCEDESTINATIONS).toString());
        } catch (JSONException e) {
            responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
            errors.put("Error reading source destinations", e);
            createResponse(responseCode, errors);
        }

        int i = 0 ;
        while(i < sourceDestinations.size()) {
            String source = sourceDestinations.get(i)._1();
            if(StringUtils.isBlank(source)) {
                responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
                errors.put("Empty source" , "Source cannot be empty");
            }
        }

        return createResponse(responseCode, errors);
    }

    public static List<Tuple2<String, String>> getSourceDestinations(String sourceDestinationsString) throws JSONException {
        JSONArray sourceDestinations = new JSONArray(sourceDestinationsString);
        List<Tuple2<String, String>> result = new ArrayList<Tuple2<String, String>>();
        for (int i = 0; i < sourceDestinations.length(); i++) {
            JSONObject sourceDestination = (JSONObject) sourceDestinations.get(i);
            String source = trim(sourceDestination.getString("source"));
            String destination = trim(sourceDestination.getString("destination"));
            result.add(new Tuple2<String, String>(source, destination));
        }
        return result;
    }


    private GoApplicationAccessor accessor;

    // this method is executed once at startup
    public void initializeGoApplicationAccessor(GoApplicationAccessor accessor) {
        this.accessor = accessor;
    }

    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }
}
