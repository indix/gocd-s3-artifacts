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
import io.jmnarloch.cd.go.plugin.api.config.AnnotatedEnumConfigurationProvider;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcher;
import io.jmnarloch.cd.go.plugin.api.dispatcher.ApiRequestDispatcherBuilder;
import io.jmnarloch.cd.go.plugin.api.task.AbstractDispatchingTask;
import io.jmnarloch.cd.go.plugin.api.validation.AbstractTaskValidator;
import io.jmnarloch.cd.go.plugin.api.validation.ValidationErrors;
import io.jmnarloch.cd.go.plugin.api.view.AbstractTaskView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static com.indix.gocd.utils.utils.Lists.foreach;
import static org.apache.commons.lang3.StringUtils.trim;

@Extension
class PublishTask extends AbstractDispatchingTask {

    @Override
    protected ApiRequestDispatcher buildDispatcher() {
        return ApiRequestDispatcherBuilder.dispatch()
                .toValidator(new PublishTaskValidator())
                .toView(new PublishTaskView("s3publish", "/views/task.template.html"))
                .toExecutor(new PublishExecutor())
                .build();
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


class PublishTaskView extends AbstractTaskView {

    public PublishTaskView(String displayValue, String templatePath) {
        super(displayValue, templatePath);
    }

}

class PublishTaskValidator extends AbstractTaskValidator {

    @Override
    public void validate(Map<String, Object> properties, final ValidationErrors errors) {

        List<Tuple2<String, String>> sourceDestinations;
        try {
            sourceDestinations = PublishTask.getSourceDestinations(properties.get(SOURCEDESTINATIONS).toString());
            foreach(sourceDestinations, new Functions.VoidFunction<Tuple2<String, String>>() {
                @Override
                public void execute(Tuple2<String, String> input) {
                    if(StringUtils.isBlank(input._1())) {
                        errors.addError(SOURCEDESTINATIONS, "Source cannot be empty");
                    }
                }
            });
        } catch (JSONException e) {
            errors.addError(SOURCEDESTINATIONS, "Error while parsing configuration");
        }
    }

}
