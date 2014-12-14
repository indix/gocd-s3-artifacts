package com.indix.gocd.s3publish;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.indix.gocd.s3publish.store.S3ArtifactStore;
import com.indix.gocd.s3publish.utils.Function;
import com.indix.gocd.s3publish.utils.Lists;
import com.indix.gocd.s3publish.utils.Tuple2;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.s3publish.PublishTask.*;
import static com.indix.gocd.s3publish.utils.Lists.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class PublishExecutor implements TaskExecutor {
    private Map<String, String> environment = new HashMap<String, String>();
    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        environment.putAll(context.environment().asMap());
        environment.putAll(System.getenv());
        if (isEmpty(env(AWS_ACCESS_KEY_ID)))
            return ExecutionResult.failure(envNotFound(AWS_ACCESS_KEY_ID));
        if (isEmpty(env(AWS_SECRET_ACCESS_KEY)))
            return ExecutionResult.failure(envNotFound(AWS_SECRET_ACCESS_KEY));
        if (isEmpty(env(GO_ARTIFACTS_S3_BUCKET)))
            return ExecutionResult.failure(envNotFound(GO_ARTIFACTS_S3_BUCKET));

        final String bucket = env(GO_ARTIFACTS_S3_BUCKET);
        String source = config.getValue(SOURCE);

        final S3ArtifactStore store = new S3ArtifactStore(s3Client(), bucket);
        File localFileToUpload = new File(String.format("%s/%s", context.workingDir(), source));
        List<FilePathToTemplate> filesToUpload = destinationOnS3(localFileToUpload);
        foreach(filesToUpload, new Function<FilePathToTemplate, Void>() {
            @Override
            public Void apply(FilePathToTemplate filePathToTemplate) {
                String localFile = filePathToTemplate._1();
                String destinationOnS3 = filePathToTemplate._2();
                context.console().printLine(String.format("Pushing %s to s3://%s/%s", localFile, bucket, destinationOnS3));
                store.put(localFile, destinationOnS3, metadata());
                context.console().printLine(String.format("Pushed %s to s3://%s/%s", localFile, bucket, destinationOnS3));

                return null; // ugly ugly really ugly :(
            }
        });

        return ExecutionResult.success("Published all artifacts to S3");
    }

    public AmazonS3Client s3Client() {
        String accessKey = System.getenv(AWS_ACCESS_KEY_ID);
        String secretKey = System.getenv(AWS_SECRET_ACCESS_KEY);
        return new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    private ObjectMetadata metadata() {
        // TODO - Add metadata information with every object
        // - Callback URL (re-usable go pipeline url)
        // - User who caused the pipeline build
        return new ObjectMetadata();
    }

    private List<FilePathToTemplate> destinationOnS3(File localFileToUpload) {
        String pipeline = env("GO_PIPELINE_NAME");
        String stageName = env("GO_STAGE_NAME");
        String jobName = env("GO_JOB_NAME");

        String pipelineCounter = env("GO_PIPELINE_COUNTER");
        String stageCounter = env("GO_STAGE_COUNTER");
        // pipeline/stage/job/pipeline_counter.stage_counter
        String template = String.format("%s/%s/%s/%s.%s", pipeline, stageName, jobName, pipelineCounter, stageCounter);

        return filesToKeys(template, localFileToUpload);
    }

    private List<FilePathToTemplate> filesToKeys(final String templateSoFar, final File fileToUpload) {
        final String templateWithFolder = templateSoFar + "/" + fileToUpload.getName();
        if (fileToUpload.isDirectory()) {
            return flatMap(fileToUpload.listFiles(), new Function<File, List<FilePathToTemplate>>() {
                @Override
                public List<FilePathToTemplate> apply(File file) {
                    return filesToKeys(templateWithFolder, file);
                }
            });
        } else {
            return Lists.of(new FilePathToTemplate(fileToUpload.getAbsolutePath(), templateWithFolder));
        }
    }

    private String env(String name) {
        return environment.get(name);
    }

    private String envNotFound(String environmentVariable) {
        return environmentVariable + " environment variable not present";
    }
}

/**
 * Represents the (AbsoluteFilePath -> S3KeyTemplate)
 */
class FilePathToTemplate extends Tuple2<String, String> {

    public FilePathToTemplate(String filePath, String template) {
        super(filePath, template);
    }
}

