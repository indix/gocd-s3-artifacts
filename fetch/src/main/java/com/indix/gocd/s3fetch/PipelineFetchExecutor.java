package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.GoEnvironment;

public class PipelineFetchExecutor extends FetchExecutor {
    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {
        String materialName = config.getMaterial();
        String materialLocator = env.get(String.format("GO_DEPENDENCY_LOCATOR_%s", materialName.toUpperCase()));

        String[] locatorParts = materialLocator.split("/");

        String pipeline = locatorParts[0];
        String pipelineCounter = locatorParts[1];
        String stage = locatorParts[2];
        String stageCounter = locatorParts[3];
        String job = config.getJob();

        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }
}
