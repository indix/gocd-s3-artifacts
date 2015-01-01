package com.indix.gocd.s3publish;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.amazonaws.util.json.JSONException;
import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Tuple2;

import static com.indix.gocd.utils.Constants.*;
import static com.indix.gocd.utils.utils.Functions.VoidFunction;
import static com.indix.gocd.utils.utils.Lists.flatMap;
import static com.indix.gocd.utils.utils.Lists.foreach;


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

        try {
            List<Tuple2<String, String>> sourceDestinations = PublishTask.getSourceDestinations(config.getValue(SOURCEDESTINATIONS));
            foreach(sourceDestinations, new VoidFunction<Tuple2<String, String>>() {
                @Override
                public void execute(Tuple2<String, String> input) {
                    String source = input._1();
                    String destination = input._2();
                    File localFileToUpload = new File(String.format("%s/%s", context.workingDir(), source));
                    pushToS3(context, env, store, localFileToUpload, destination);
                }
            });
        } catch (JSONException e) {
            return ExecutionResult.failure("Failed while parsing configuration", e);
        }
        setMetadata(env, bucket, store);

        return ExecutionResult.success("Published all artifacts to S3");
    }

    /*
        Made public only for tests
     */
    public AmazonS3Client s3Client(GoEnvironment env) {
        String accessKey = env.get(AWS_ACCESS_KEY_ID);
        String secretKey = env.get(AWS_SECRET_ACCESS_KEY);
        return new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    private void pushToS3(final TaskExecutionContext context, final GoEnvironment env, final S3ArtifactStore store, File localFileToUpload, String destination) {
        String templateSoFar = env.artifactsLocationTemplate();
        if(!org.apache.commons.lang3.StringUtils.isBlank(destination)) {
            templateSoFar += "/" + destination;
        }
        List<FilePathToTemplate> filesToUpload = generateFilesToUpload(templateSoFar, localFileToUpload);
        foreach(filesToUpload, new VoidFunction<FilePathToTemplate>() {
            @Override
            public void execute(FilePathToTemplate filePathToTemplate) {
                String localFile = filePathToTemplate._1();
                String destinationOnS3 = filePathToTemplate._2();
                context.console().printLine(String.format("Pushing %s to %s", localFile, store.pathString(destinationOnS3)));
                store.put(localFile, destinationOnS3);
                context.console().printLine(String.format("Pushed %s to %s", localFile, store.pathString(destinationOnS3)));
            }
        });
    }

    private ObjectMetadata metadata(GoEnvironment env) {
        String tracebackUrl = env.traceBackUrl();
        String user = env.triggeredUser();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata(METADATA_USER, user);
        objectMetadata.addUserMetadata(METADATA_TRACEBACK_URL, tracebackUrl);
        objectMetadata.addUserMetadata(COMPLETED, COMPLETED);
        return objectMetadata;
    }

    private List<FilePathToTemplate> generateFilesToUpload(final String templateSoFar, final File fileToUpload) {
        final String templateWithFolder = String.format("%s/%s", templateSoFar, fileToUpload.getName());
        if (fileToUpload.isDirectory()) {
            return flatMap(fileToUpload.listFiles(), new Function<File, List<FilePathToTemplate>>() {
                @Override
                public List<FilePathToTemplate> apply(File file) {
                    return generateFilesToUpload(templateWithFolder, file);
                }
            });
        } else {
            return Lists.of(new FilePathToTemplate(fileToUpload.getAbsolutePath(), templateWithFolder));
        }
    }

    private ExecutionResult envNotFound(String environmentVariable) {
        return ExecutionResult.failure(String.format("%s environment variable not present", environmentVariable));
    }

    private void setMetadata(GoEnvironment env, String bucket, S3ArtifactStore store) {
        ObjectMetadata metadata = metadata(env);
        metadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket,
                env.artifactsLocationTemplate() + "/",
                emptyContent,
                metadata);

        store.put(putObjectRequest);
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
