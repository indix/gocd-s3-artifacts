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
import java.util.List;

import static com.indix.gocd.s3publish.Constants.*;
import static com.indix.gocd.s3publish.utils.Lists.flatMap;
import static com.indix.gocd.s3publish.utils.Lists.foreach;
import static org.apache.commons.lang3.StringUtils.*;

public class PublishExecutor implements TaskExecutor {

    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        final GoEnvironment env = new GoEnvironment();
        env.putAll(context.environment().asMap());
        if (env.isAbsent(AWS_ACCESS_KEY_ID)) return envNotFound(AWS_ACCESS_KEY_ID);
        if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) return envNotFound(AWS_SECRET_ACCESS_KEY);
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);

        final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
        final S3ArtifactStore store = new S3ArtifactStore(s3Client(env), bucket);
        String[] sources = split(config.getValue(SOURCE), "\n");
        foreach(sources, new Function<String, Void>() {
            @Override
            public Void apply(String source) {
                pushToS3(context, env, store, bucket, trim(source));
                return null;
            }
        });

        return ExecutionResult.success("Published all artifacts to S3");
    }

    private void pushToS3(final TaskExecutionContext context, final GoEnvironment env, final S3ArtifactStore store, final String bucket, String source) {
        File localFileToUpload = new File(String.format("%s/%s", context.workingDir(), source));
        List<FilePathToTemplate> filesToUpload = destinationOnS3(env, localFileToUpload);
        foreach(filesToUpload, new Function<FilePathToTemplate, Void>() {
            @Override
            public Void apply(FilePathToTemplate filePathToTemplate) {
                String localFile = filePathToTemplate._1();
                String destinationOnS3 = filePathToTemplate._2();
                context.console().printLine(String.format("Pushing %s to s3://%s/%s", localFile, bucket, destinationOnS3));
                store.put(localFile, destinationOnS3, metadata(env));
                context.console().printLine(String.format("Pushed %s to s3://%s/%s", localFile, bucket, destinationOnS3));

                return null; // ugly ugly really ugly :(
            }
        });
    }

    public AmazonS3Client s3Client(GoEnvironment env) {
        String accessKey = env.get(AWS_ACCESS_KEY_ID);
        String secretKey = env.get(AWS_SECRET_ACCESS_KEY);
        return new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    private ObjectMetadata metadata(GoEnvironment env) {
        String tracebackUrl = env.traceBackUrl();
        String user = env.triggeredUser();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata(METADATA_USER, user);
        objectMetadata.addUserMetadata(METADATA_TRACEBACK_URL, tracebackUrl);
        return objectMetadata;
    }

    private List<FilePathToTemplate> destinationOnS3(GoEnvironment env, File localFileToUpload) {
        return filesToKeys(env.artifactsLocationTemplate(), localFileToUpload);
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

    private ExecutionResult envNotFound(String environmentVariable) {
        return ExecutionResult.failure(environmentVariable + " environment variable not present");
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

