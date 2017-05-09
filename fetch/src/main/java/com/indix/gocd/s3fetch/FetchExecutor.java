package com.indix.gocd.s3fetch;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.indix.gocd.utils.Constants.*;

public class FetchExecutor {
    private static Logger logger = Logger.getLoggerFor(FetchTask.class);

    public TaskExecutionResult execute(Config config, final Context context) {
        final GoEnvironment env = new GoEnvironment(context.getEnvironmentVariables());
        if (!env.hasAWSUseIamRole()) {
            if (env.isAbsent(AWS_ACCESS_KEY_ID)) return envNotFound(AWS_ACCESS_KEY_ID);
            if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) return envNotFound(AWS_SECRET_ACCESS_KEY);
        }
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);

        try {
            final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
            final S3ArtifactStore store = getS3ArtifactStore(env, bucket);

            String artifactPathOnS3 = getArtifactsLocationTemplate(config, env);
            context.printMessage(String.format("Getting artifacts from %s", store.pathString(artifactPathOnS3)));
            String destination = String.format("%s/%s", context.getWorkingDir(), config.getDestination());
            setupDestinationDirectory(destination);
            store.getPrefix(artifactPathOnS3, destination);
            return new TaskExecutionResult(true, "Fetched all artifacts");
        } catch (Exception e) {
            String message = String.format("Failure while downloading artifacts - %s", e.getMessage());
            logger.error(message, e);
            return new TaskExecutionResult(false, message, e);
        }
    }

    public S3ArtifactStore getS3ArtifactStore(GoEnvironment env, String bucket) {
        return new S3ArtifactStore(s3Client(env), bucket);
    }

    private void setupDestinationDirectory(String destination) {
        File destinationDirectory = new File(destination);
        try {
            if(!destinationDirectory.exists()) {
                FileUtils.forceMkdir(destinationDirectory);
            }
        } catch (IOException ioe) {
            logger.error(String.format("Error while setting up destination - %s", ioe.getMessage()), ioe);
        }
    }

    public AmazonS3Client s3Client(GoEnvironment env) {
        AmazonS3Client client;
        if (env.hasAWSUseIamRole()) {
            client = new AmazonS3Client(new InstanceProfileCredentialsProvider());
        } else {
            client = new AmazonS3Client(new BasicAWSCredentials(env.get(AWS_ACCESS_KEY_ID), env.get(AWS_SECRET_ACCESS_KEY)));
        }
        return client;
    }


    public String getArtifactsLocationTemplate(Config config, GoEnvironment env) {
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

    private TaskExecutionResult envNotFound(String environmentVariable) {
        String message = String.format("%s environment variable not present", environmentVariable);
        logger.error(message);
        return new TaskExecutionResult(false, message);
    }
}
