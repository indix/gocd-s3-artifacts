package com.indix.gocd.s3fetch;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.indix.gocd.utils.Constants.*;

public class FetchExecutor implements TaskExecutor {
    private static Logger logger = Logger.getLoggerFor(FetchTask.class);

    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        final GoEnvironment env = new GoEnvironment();
        env.putAll(context.environment().asMap());

        if (env.isAbsent(AWS_ACCESS_KEY_ID)) return envNotFound(AWS_ACCESS_KEY_ID);
        if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) return envNotFound(AWS_SECRET_ACCESS_KEY);
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);

        final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
        final S3ArtifactStore store = s3ArtifactStore(env, bucket);

        String artifactPathOnS3 = getS3ArtifactPath(config, env);
        context.console().printLine(String.format("Getting artifacts from %s", store.pathString(artifactPathOnS3)));
        String destination = String.format("%s/%s", context.workingDir(), config.getValue(FetchTask.DESTINATION));
        setupDestinationDirectory(destination);

        try {
            store.getPrefix(artifactPathOnS3, destination);
        } catch (Exception e) {
            String message = String.format("Failure while downloading artifacts - %s", e.getMessage());
            logger.error(message, e);
            return ExecutionResult.failure(message, e);
        }

        return ExecutionResult.success("Fetched all artifacts");
    }

    private String getS3ArtifactPath(TaskConfig config, GoEnvironment env) {
        String repoName = config.getValue(FetchTask.REPO).toUpperCase();
        String packageName = config.getValue(FetchTask.PACKAGE).toUpperCase();

        String materialLabel = env.get(String.format("GO_PACKAGE_%s_%s_LABEL", repoName, packageName));
        String[] counters = materialLabel.split("\\.");
        String pipelineCounter = counters[0];
        String stageCounter = counters[1];
        String pipeline = env.get(String.format("GO_PACKAGE_%s_%s_PIPELINE_NAME", repoName, packageName));
        String stage = env.get(String.format("GO_PACKAGE_%s_%s_STAGE_NAME", repoName, packageName));
        String job = env.get(String.format("GO_PACKAGE_%s_%s_JOB_NAME", repoName, packageName));
        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    private void setupDestinationDirectory(String destination) {
        File destinationDirectory = new File(destination);
        try {
            if(destinationDirectory.exists()) {
                FileUtils.cleanDirectory(destinationDirectory);
                FileUtils.deleteDirectory(destinationDirectory);
            }
            FileUtils.forceMkdir(destinationDirectory);
        } catch (IOException ioe) {
            logger.error(String.format("Error while setting up destination - %s", ioe.getMessage()), ioe);
        }
    }

    public S3ArtifactStore s3ArtifactStore(GoEnvironment env, String bucket) {
        return new S3ArtifactStore(s3Client(env), bucket);
    }

    public AmazonS3Client s3Client(GoEnvironment env) {
        String accessKey = env.get(AWS_ACCESS_KEY_ID);
        String secretKey = env.get(AWS_SECRET_ACCESS_KEY);
        return new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    private ExecutionResult envNotFound(String environmentVariable) {
        return ExecutionResult.failure(String.format("%s environment variable not present", environmentVariable));
    }
}

