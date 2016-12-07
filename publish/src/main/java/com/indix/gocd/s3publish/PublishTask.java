package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.GsonBuilder;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Tuple2;
import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.utils.Lists.foreach;
import static org.apache.commons.lang3.StringUtils.trim;

import com.google.gson.Gson;

@Extension
public class PublishTask implements GoPlugin {


    public GoPluginApiResponse handleGetConfigRequest(GoPluginApiRequest request) {
        Map config = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        return DefaultGoPluginApiResponse.success(new Gson().toJson(config));
    }


    public GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        return new PublishExecutor();
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
    public GoPluginApiResponse handle(GoPluginApiRequest request) {
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
                errors.put("Empty source" , "Source cannot be empty")
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


    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return null;
    }
}
