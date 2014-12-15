package com.indix.gocd.s3publish;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around Go's Environment variables
 */
public class GoEnvironment {
    private Map<String, String> environment = new HashMap<String, String>();

    public GoEnvironment() {
        this.environment.putAll(System.getenv());
    }

    public GoEnvironment putAll(Map<String, String> existing) {
        environment.putAll(existing);
        return this;
    }

    public String get(String name) {
        return environment.get(name);
    }

    public String traceBackUrl() {
        String serverUrl = get("GO_SERVER_URL");
        String pipelineName = get("GO_PIPELINE_NAME");
        String pipelineCounter = get("GO_PIPELINE_COUNTER");
        String stageName = get("GO_STAGE_NAME");
        String stageCounter = get("GO_STAGE_COUNTER");
        String jobName = get("GO_JOB_NAME");
        return String.format("%s/tab/build/detail/%s/%s/%s/%s/%s", serverUrl, pipelineName, pipelineCounter, stageName, stageCounter, jobName);
    }

    public String triggeredUser() {
        return get("GO_TRIGGER_USER");
    }

    /**
     * Version Format on S3 is <code>pipeline/stage/job/pipeline_counter.stage_counter</code>
     */
    public String artifactsLocationTemplate() {
        String pipeline = get("GO_PIPELINE_NAME");
        String stageName = get("GO_STAGE_NAME");
        String jobName = get("GO_JOB_NAME");

        String pipelineCounter = get("GO_PIPELINE_COUNTER");
        String stageCounter = get("GO_STAGE_COUNTER");
        return String.format("%s/%s/%s/%s.%s", pipeline, stageName, jobName, pipelineCounter, stageCounter);
    }

}
