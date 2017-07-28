package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.logging.Logger;

public class PackageFetchExecutor extends FetchExecutor {

    private static Logger logger = Logger.getLoggerFor(PackageFetchExecutor.class);

    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {
        String repoName = config.getRepo();
        String packageName = config.getPkg();
        logger.debug(String.format("S3 fetch config uses repoName=%s and packageName=%s", repoName, packageName));

        String materialLabel = env.get(String.format("GO_PACKAGE_%s_%s_LABEL", repoName, packageName));
        if(materialLabel == null) {
            throw new RuntimeException("Please check Repository name or Package name configuration. Also, ensure that the appropriate S3 material is configured for the pipeline.");
        }

        String[] counters = materialLabel.split("\\.");
        String pipelineCounter = counters[0];
        String stageCounter = counters[1];
        String pipeline = env.get(String.format("GO_PACKAGE_%s_%s_PIPELINE_NAME", repoName, packageName));
        String stage = env.get(String.format("GO_PACKAGE_%s_%s_STAGE_NAME", repoName, packageName));
        String job = env.get(String.format("GO_PACKAGE_%s_%s_JOB_NAME", repoName, packageName));
        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    @Override
    protected String getBucket(Config config, GoEnvironment env) {
        String repoName = config.getRepo();
        String packageName = config.getPkg();
        String bucketFromMaterial = env.get(String.format("GO_REPO_%s_%s_S3_BUCKET", repoName, packageName));
        if(bucketFromMaterial != null) {
            return bucketFromMaterial;
        }

        return super.getBucket(config, env);
    }
}
