package com.indix.gocd.s3publish;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.json.JSONException;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Tuple2;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static com.indix.gocd.utils.Constants.*;
import static com.indix.gocd.utils.utils.Functions.VoidFunction;
import static com.indix.gocd.utils.utils.Lists.flatMap;
import static com.indix.gocd.utils.utils.Lists.foreach;


public class PublishExecutor implements TaskExecutor {
    private Logger log = Logger.getLoggerFor(PublishTask.class);

    @Override
    public ExecutionResult execute(TaskConfig config, final TaskExecutionContext context) {
        final GoEnvironment env = getGoEnvironment();
        env.putAll(context.environment().asMap());
        if (!env.hasAWSUseIamRole()) {
            if (env.isAbsent(AWS_ACCESS_KEY_ID)) return envNotFound(AWS_ACCESS_KEY_ID);
            if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) return envNotFound(AWS_SECRET_ACCESS_KEY);
        }
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);
        if (env.isAbsent(GO_SERVER_DASHBOARD_URL)) return envNotFound(GO_SERVER_DASHBOARD_URL);

        final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
        final S3ArtifactStore store = getStore(env, bucket);
        store.setStorageClass(env.getOrElse(AWS_STORAGE_CLASS, STORAGE_CLASS_STANDARD));

        final String destinationPrefix = getDestinationPrefix(config, env);

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

                            pushToS3(context, destinationPrefix, store, localFileToUpload, destination);
                        }
                    });
                }
            });
        } catch (JSONException e) {
            String message = "Failed while parsing configuration";
            log.error(message);
            return ExecutionResult.failure(message, e);
        }

        // A configured destination prefix is used to deploy files rather than publish artifacts
        // We only want to set metadata when publishing artifacts
        if (!hasConfigDestinationPrefix(config)) {
            setMetadata(env, bucket, destinationPrefix, store);
        }

        return ExecutionResult.success("Published all artifacts to S3");
    }

    /*
        Made public for tests else mockito fails
    */
    public S3ArtifactStore getStore(GoEnvironment env, String bucket) {
        return S3ArtifactStore.createStore(env.get(AWS_ACCESS_KEY_ID), env.get(AWS_SECRET_ACCESS_KEY), bucket);
    }

    /*
        Made public for tests else mockito fails
    */
    public String[] parseSourcePath(String source, String workingDir) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(workingDir);
        directoryScanner.setIncludes(new String[]{source});
        directoryScanner.scan();
        return ArrayUtils.addAll(directoryScanner.getIncludedFiles(), directoryScanner.getIncludedDirectories());
    }

    /*
        Made public for tests else mockito fails
     */
    public GoEnvironment getGoEnvironment() {
        return new GoEnvironment();
    }

    private void pushToS3(final TaskExecutionContext context, final String destinationPrefix, final S3ArtifactStore store, File localFileToUpload, String destination) {
        String templateSoFar = ensureKeySegmentValid(destinationPrefix);
        if (!StringUtils.isBlank(destination)) {
            templateSoFar += destination;
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
        objectMetadata.addUserMetadata(GO_PIPELINE_LABEL, env.get(GO_PIPELINE_LABEL));
        return objectMetadata;
    }

    private List<FilePathToTemplate> generateFilesToUpload(final String templateSoFar, final File fileToUpload) {
        final String templateWithFolder = ensureKeySegmentValid(templateSoFar) + fileToUpload.getName(); // ensure it ends with a slash and add filename

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

    private void setMetadata(GoEnvironment env, String bucket, String destinationPrefix, S3ArtifactStore store) {
        ObjectMetadata metadata = metadata(env);
        metadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket,
                ensureKeySegmentValid(destinationPrefix),
                emptyContent,
                metadata);

        store.put(putObjectRequest);
    }

    private String getConfigDestinationPrefix(final TaskConfig config) {
        return config.getValue(DESTINATION_PREFIX);
    }

    private boolean hasConfigDestinationPrefix(final TaskConfig config) {
        String destinationPrefix = getConfigDestinationPrefix(config);

        return !StringUtils.isBlank(destinationPrefix);
    }

    private String getDestinationPrefix(final TaskConfig config, final GoEnvironment env) {
        if (!hasConfigDestinationPrefix(config)) {
            return env.artifactsLocationTemplate();
        }

        String destinationPrefix = getConfigDestinationPrefix(config);

        destinationPrefix = env.replaceVariables(destinationPrefix);

        if (destinationPrefix.endsWith("/")) {
            destinationPrefix = destinationPrefix.substring(0, destinationPrefix.length() - 1);
        }

        return destinationPrefix;
    }

    private String ensureKeySegmentValid(String segment) {
        if (StringUtils.isBlank(segment)) {
            return segment;
        }

        if (!StringUtils.endsWith(segment, "/")) {
            segment += "/";
        }

        return segment;
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
