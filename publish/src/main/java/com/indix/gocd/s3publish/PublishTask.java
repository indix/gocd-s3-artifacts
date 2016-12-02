package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.GsonBuilder;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Tuple2;
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
public class PublishTask implements  GoPlugin {



    public GoPluginApiResponse config() {
        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty(SOURCEDESTINATIONS);
        taskConfig.addProperty(DESTINATION_PREFIX);
        return taskConfig;
    }


    public GoPluginApiResponse executor() {
        return new PublishExecutor();
    }


    public GoPluginApiResponse view() {
        return new TaskView() {
            @Override
            public String displayValue() {
                return "Publish To S3";
            }

            @Override
            public String template() {
                try {
                    return IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Error happened during rendering - " + e.getMessage();
                }
            }
        };
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) {
        Map config = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        List<Tuple2<String, String>> sourceDestinations;
        try {
            sourceDestinations = getSourceDestinations(config.get(SOURCEDESTINATIONS).toString());
        } catch (JSONException e) {
            List<JSONException> errors = Arrays.asList(e);
            return DefaultGoPluginApiResponse.error(new Gson().toJson(errors));
        }

        final HashMap errors = new HashMap();

        foreach(sourceDestinations, new Functions.VoidFunction<Tuple2<String, String>>() {
            @Override
            public void execute(Tuple2<String, String> input) {
                if (StringUtils.isBlank(input._1())) {
                    errors.put(DefaultGoPluginApiResponse.VALIDATION_FAILED, "Source cannot be empty");
                }
            }
        });

        if (errors.containsKey(DefaultGoPluginApiResponse.VALIDATION_FAILED)) {
            return DefaultGoPluginApiResponse.badRequest(new Gson().toJson(errors));
        } else {
            return DefaultGoPluginApiResponse.success(new Gson().toJson(errors));
        }
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
