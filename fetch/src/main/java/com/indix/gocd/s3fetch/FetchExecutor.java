package com.indix.gocd.s3fetch;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class FetchExecutor implements TaskExecutor {
    private static Logger logger = Logger.getLoggerFor(FetchTask.class);

    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        final FetchConfig fetchConfig = new FetchConfig(config, context);

        ValidationResult validationResult = fetchConfig.validate();
        if(!validationResult.isSuccessful()) {
            return ExecutionResult.failure(validationResult.getMessages().toString());
        }

        final S3ArtifactStore store = s3ArtifactStore(fetchConfig);

        String artifactPathOnS3 = fetchConfig.getArtifactsLocationTemplate();
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

    public S3ArtifactStore s3ArtifactStore(FetchConfig config) {
        return new S3ArtifactStore(s3Client(config), config.getS3Bucket());
    }

    public AmazonS3Client s3Client(FetchConfig config) {
        return new AmazonS3Client(new BasicAWSCredentials(config.getAWSAccessKeyId(), config.getAWSSecretAccessKey()));
    }

}

