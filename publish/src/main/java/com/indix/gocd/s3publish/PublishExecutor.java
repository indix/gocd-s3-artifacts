package com.indix.gocd.s3publish;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.amazonaws.util.json.JSONException;
import com.indix.gocd.utils.AWSCredentialsFactory;
import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.logging.Logger;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tools.ant.DirectoryScanner;

import static com.indix.gocd.utils.Constants.*;
import static com.indix.gocd.utils.utils.Functions.VoidFunction;
import static com.indix.gocd.utils.utils.Lists.flatMap;
import static com.indix.gocd.utils.utils.Lists.foreach;


public class PublishExecutor implements TaskExecutor {
    private Logger log = Logger.getLoggerFor(PublishTask.class);

    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        final GoEnvironment env = new GoEnvironment();
        env.putAll(context.environment().asMap());

        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);
        if (env.isAbsent(GO_SERVER_DASHBOARD_URL)) return envNotFound(GO_SERVER_DASHBOARD_URL);
        AmazonS3Client s3Client;
        try {
            s3Client = getS3Client(env);
        } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage());
            return ExecutionResult.failure(ex.getMessage());
        }
        final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
        final S3ArtifactStore store = new S3ArtifactStore(s3Client, bucket);

        try {
            List<Tuple2<String, String>> sourceDestinations = PublishTask.getSourceDestinations(config.getValue(SOURCEDESTINATIONS));
            foreach(sourceDestinations, new VoidFunction<Tuple2<String, String>>() {
                @Override
                public void execute(Tuple2<String, String> input) {
                    final String source = input._1();
                    final String destination = input._2();
                    String[] files = parseSourcePath(source, context.workingDir());

                    foreach(files, new VoidFunction<String>() {
                        @Override
                        public void execute(String includedFile) {
                            File localFileToUpload = new File(String.format("%s/%s", context.workingDir(), includedFile));
                            pushToS3(context, env, store, localFileToUpload, destination);
                        }
                    });
                }
            });
        } catch (JSONException e) {
            String message = "Failed while parsing configuration";
            log.error(message);
            return ExecutionResult.failure(message, e);
        }
        setMetadata(env, bucket, store);

        return ExecutionResult.success("Published all artifacts to S3");
    }

    /*
        Made public only for tests
     */
    public String[] parseSourcePath(String source, String workingDir) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(workingDir);
        directoryScanner.setIncludes(new String[]{source});
        directoryScanner.scan();
        return ArrayUtils.addAll(directoryScanner.getIncludedFiles(), directoryScanner.getIncludedDirectories());
    }

    /*
        Made public only for tests
     */
    public AmazonS3Client getS3Client(GoEnvironment env) {
        return new AmazonS3Client(awsCredentialsProvider(env));
    }

    private AWSCredentialsProvider awsCredentialsProvider(GoEnvironment env) {
        return new AWSCredentialsFactory(env.asMap()).getCredentialsProvider();
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
        String message = String.format("%s environment variable not present", environmentVariable);
        log.error(message);
        return ExecutionResult.failure(message);
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
