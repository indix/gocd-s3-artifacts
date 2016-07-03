package com.indix.gocd.s3fetch;

import org.apache.commons.lang3.StringUtils;
import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.logging.Logger;
import static com.indix.gocd.utils.Constants.*;

public class FetchConfig {
    private final String materialLabel;
    private final String pipeline;
    private final String stage;
    private final String job;
    private GoEnvironment env;
    private static Logger logger = Logger.getLoggerFor(FetchConfig.class);

    public FetchConfig(TaskConfig config, TaskExecutionContext context) {
        this(config, context, new GoEnvironment());
    }

    public FetchConfig(TaskConfig config, TaskExecutionContext context, GoEnvironment goEnvironment) {
        this.env = goEnvironment;
        env.putAll(context.environment().asMap());

        String repoName = escapeEnvironmentVariable(config.getValue(FetchTask.REPO));
        String packageName = escapeEnvironmentVariable(config.getValue(FetchTask.PACKAGE));
        logger.debug(String.format("s3 fetch config uses repoName=%s and packageName=%s", repoName, packageName));
        this.materialLabel = env.get(String.format("GO_PACKAGE_%s_%s_LABEL", repoName, packageName));
        this.pipeline = env.get(String.format("GO_PACKAGE_%s_%s_PIPELINE_NAME", repoName, packageName));
        this.stage = env.get(String.format("GO_PACKAGE_%s_%s_STAGE_NAME", repoName, packageName));
        this.job = env.get(String.format("GO_PACKAGE_%s_%s_JOB_NAME", repoName, packageName));
    }

    private static String escapeEnvironmentVariable(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
    }

    public ValidationResult validate() {
        ValidationResult validationResult = new ValidationResult();
        if (!env.hasAWSUseIamRole()) {
            if (env.isAbsent(AWS_ACCESS_KEY_ID)) validationResult.addError(envNotFound(AWS_ACCESS_KEY_ID));
            if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) validationResult.addError(envNotFound(AWS_SECRET_ACCESS_KEY));
        }
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) validationResult.addError(envNotFound(GO_ARTIFACTS_S3_BUCKET));
        if (StringUtils.isBlank(materialLabel))
            validationResult.addError(new ValidationError("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline."));

        return validationResult;
    }

    public String getArtifactsLocationTemplate() {
        String[] counters = materialLabel.split("\\.");
        String pipelineCounter = counters[0];
        String stageCounter = counters[1];
        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    public boolean hasAWSUseIamRole() {
        return env.hasAWSUseIamRole();
    }

    public String getAWSAccessKeyId() {
        return env.get(AWS_ACCESS_KEY_ID);
    }

    public String getAWSSecretAccessKey() {
        return env.get(AWS_SECRET_ACCESS_KEY);
    }

    public String getS3Bucket() {
        return env.get(GO_ARTIFACTS_S3_BUCKET);
    }

    private ValidationError envNotFound(String environmentVariable) {
        return new ValidationError(environmentVariable, String.format("%s environment variable not present", environmentVariable));
    }

}
