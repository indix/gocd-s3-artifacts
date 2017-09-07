package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.GoEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class StageFetchExecutor extends FetchExecutor {

    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {

        String prefix = config.getStageSourcePrefix();
        String source = config.getStageSource();
        if (StringUtils.isBlank(prefix)) {
            String pipeline = env.get("GO_PIPELINE_NAME");
            String pipelineCounter = env.get("GO_PIPELINE_COUNTER");
            String stage = config.getStageName();
            // TODO : Find out stage latest counter
            String stageCounter = "2";
            String job = config.getStageJob();

            prefix = env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
        }

        return prefix + "/" + source;
    }

    @Override
    public Map<String, String> validate(Config config) {
        Map<String, String> errors = new HashMap<>();

        if (StringUtils.isBlank(config.getStageSourcePrefix())) {

            if (StringUtils.isBlank(config.getStageName())) {
                errors.put(Constants.STAGE_NAME, Constants.REQUIRED_FIELD_MESSAGE);
            }
            if (StringUtils.isBlank(config.getStageJob())) {
                errors.put(Constants.STAGE_JOB, Constants.REQUIRED_FIELD_MESSAGE);
            }
        }

        if (StringUtils.isBlank(config.getStageSource())) {
            errors.put(Constants.STAGE_SOURCE, Constants.REQUIRED_FIELD_MESSAGE);
        }

        return errors;
    }
}
