package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Tuple2;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.utils.Lists.foreach;
import static org.apache.commons.lang3.StringUtils.trim;

@Extension
public class PublishTask implements Task {

    @Override
    public TaskConfig config() {
        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty(SOURCEDESTINATIONS);
        taskConfig.addProperty(DESTINATION_PREFIX);
        return taskConfig;
    }

    @Override
    public TaskExecutor executor() {
        return new PublishExecutor();
    }

    @Override
    public TaskView view() {
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
    public ValidationResult validate(TaskConfig taskConfig) {
        final ValidationResult validationResult = new ValidationResult();
        List<Tuple2<String, String>> sourceDestinations;
        try {
            sourceDestinations = getSourceDestinations(taskConfig.getValue(SOURCEDESTINATIONS));
        } catch (JSONException e) {
            validationResult.addError(new ValidationError(SOURCEDESTINATIONS, "Error while parsing configuration"));
            return validationResult;
        }

        foreach(sourceDestinations, new Functions.VoidFunction<Tuple2<String, String>>() {
            @Override
            public void execute(Tuple2<String, String> input) {
                if(StringUtils.isBlank(input._1())) {
                    validationResult.addError(new ValidationError(SOURCEDESTINATIONS, "Source cannot be empty"));
                }
            }
        });

        return validationResult;
    }

    public static List<Tuple2<String, String>> getSourceDestinations(String sourceDestinationsString) throws JSONException {
        JSONArray sourceDestinations = new JSONArray(sourceDestinationsString);
        List<Tuple2<String, String>> result = new ArrayList<Tuple2<String, String>>();
        for(int i =0; i < sourceDestinations.length(); i++) {
            JSONObject sourceDestination = (JSONObject)sourceDestinations.get(i);
            String source = trim(sourceDestination.getString("source"));
            String destination = trim(sourceDestination.getString("destination"));
            result.add(new Tuple2<String, String>(source, destination));
        }

        return result;
    }
}
