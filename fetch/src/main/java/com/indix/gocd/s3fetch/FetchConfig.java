package com.indix.gocd.s3fetch;

import com.amazonaws.util.StringUtils;
import com.indix.gocd.utils.AWSCredentialsFactory;
import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class FetchConfig {
    private final String materialLabel;
    private final String pipeline;
    private final String stage;
    private final String job;
    private GoEnvironment env;
    private AWSCredentialsFactory awsCredentialsFactory;

    public FetchConfig(TaskConfig config, TaskExecutionContext context) {
        this.env = new GoEnvironment();
        env.putAll(context.environment().asMap());
        this.awsCredentialsFactory = new AWSCredentialsFactory(this.env.asMap());

        String repoName = config.getValue(FetchTask.REPO).toUpperCase().replaceAll("-", "_");
        String packageName = config.getValue(FetchTask.PACKAGE).toUpperCase().replaceAll("-", "_");
        this.materialLabel = env.get(String.format("GO_PACKAGE_%s_%s_LABEL", repoName, packageName));
        this.pipeline = env.get(String.format("GO_PACKAGE_%s_%s_PIPELINE_NAME", repoName, packageName));
        this.stage = env.get(String.format("GO_PACKAGE_%s_%s_STAGE_NAME", repoName, packageName));
        this.job = env.get(String.format("GO_PACKAGE_%s_%s_JOB_NAME", repoName, packageName));
    }

    public ValidationResult validate() {
        ValidationResult validationResult = new ValidationResult();
        for (String error : getAwsCredentialsValidationErrors()) {
            validationResult.addError(new ValidationError(error));
        }
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) validationResult.addError(envNotFound(GO_ARTIFACTS_S3_BUCKET));
        if (StringUtils.isNullOrEmpty(materialLabel))
            validationResult.addError(new ValidationError("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline."));

        return validationResult;
    }

    public String getArtifactsLocationTemplate() {
        String[] counters = materialLabel.split("\\.");
        String pipelineCounter = counters[0];
        String stageCounter = counters[1];
        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    public String getS3Bucket() {
        return env.get(GO_ARTIFACTS_S3_BUCKET);
    }

    public Map<String,String> asMap() { return env.asMap(); }

    public AWSCredentialsFactory getAWSCredentialsFactory() { return this.awsCredentialsFactory; }

    public List<String> getAwsCredentialsValidationErrors() {
        return awsCredentialsFactory.validationErrors();
    }

    private ValidationError envNotFound(String environmentVariable) {
        return new ValidationError(environmentVariable, String.format("%s environment variable not present", environmentVariable));
    }
}
