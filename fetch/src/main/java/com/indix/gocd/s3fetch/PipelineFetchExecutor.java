package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.GoEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class PipelineFetchExecutor extends FetchExecutor {
    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {
        String materialLocator = env.get(String.format("GO_DEPENDENCY_LOCATOR_%s", config.getMaterial()));
        if (materialLocator == null) {
            throw new RuntimeException("Please check Material name configuration.");
        }

        String[] locatorParts = materialLocator.split("/");

        String pipeline = locatorParts[0];
        String pipelineCounter = locatorParts[1];
        String stage = locatorParts[2];
        String stageCounter = locatorParts[3];
        String job = config.getJob();

        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    @Override
    public Map<String, String> validate(Config config) {
        Map<String, String> errors = new HashMap<>();
        if (StringUtils.isBlank(config.getMaterial())) {
            errors.put(Constants.MATERIAL, Constants.REQUIRED_FIELD_MESSAGE);
        }
        if (StringUtils.isBlank(config.getJob())) {
            errors.put(Constants.JOB, Constants.REQUIRED_FIELD_MESSAGE);
        }
        return errors;
    }
}
