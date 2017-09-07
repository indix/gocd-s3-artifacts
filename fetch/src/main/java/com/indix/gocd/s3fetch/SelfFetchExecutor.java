package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.GoEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class SelfFetchExecutor extends FetchExecutor {

    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {

        String prefix = config.getSourcePrefix();
        String source = config.getSource();
        if (StringUtils.isBlank(prefix)) {
            String pipeline = env.get("GO_PIPELINE_NAME");
            String pipelineCounter = env.get("GO_PIPELINE_COUNTER");
            String stage = config.getStage();
            // TODO : Find out stage latest counter
            String stageCounter = "2";
            String job = config.getJob();

            prefix = env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
        }

        return prefix + "/" + source;
    }

    @Override
    public Map<String, String> validate(Config config) {
        Map<String, String> errors = new HashMap<>();

        if (StringUtils.isBlank(config.getSourcePrefix())) {

            if (StringUtils.isBlank(config.getStage())) {
                errors.put(Constants.STAGE, Constants.REQUIRED_FIELD_MESSAGE);
            }
            if (StringUtils.isBlank(config.getJob())) {
                errors.put(Constants.JOB, Constants.REQUIRED_FIELD_MESSAGE);
            }
        }

        if (StringUtils.isBlank(config.getSource())) {
            errors.put(Constants.SOURCE, Constants.REQUIRED_FIELD_MESSAGE);
        }

        return errors;
    }
}
