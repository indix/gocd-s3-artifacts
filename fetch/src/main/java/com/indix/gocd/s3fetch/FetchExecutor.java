package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public abstract class FetchExecutor {
    private static Logger logger = Logger.getLoggerFor(FetchExecutor.class);

    public TaskExecutionResult execute(Config config, final Context context) {

        try {
            final GoEnvironment env = new GoEnvironment(context.getEnvironmentVariables());
            String artifactPathOnS3 = getArtifactsLocationTemplate(config, env);
            final String bucket = getBucket(config, env);
            final S3ArtifactStore store = getS3ArtifactStore(env, bucket);

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

    protected S3ArtifactStore getS3ArtifactStore(GoEnvironment env, String bucket) {
        return new S3ArtifactStore(env, bucket);
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

    protected String getBucket(Config config, GoEnvironment env) {
        if(env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) {
            throw new RuntimeException("S3 bucket to fetch from should be from material plugin or GO_ARTIFACTS_S3_BUCKET env var.");
        }

        return env.get(GO_ARTIFACTS_S3_BUCKET);
    }

    protected abstract String getArtifactsLocationTemplate(Config config, GoEnvironment env);

    public abstract Map<String,String> validate(Config config);
}
