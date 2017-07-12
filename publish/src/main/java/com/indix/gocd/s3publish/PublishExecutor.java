package com.indix.gocd.s3publish;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.JsonSyntaxException;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Tuple2;
import com.thoughtworks.go.plugin.api.logging.Logger;
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


public class PublishExecutor {
    private Logger logger = Logger.getLoggerFor(PublishTask.class);

    public TaskExecutionResult execute(Config config, final Context context) {
        try {
            final GoEnvironment env = new GoEnvironment(context.getEnvironmentVariables());
            if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) return envNotFound(GO_ARTIFACTS_S3_BUCKET);
            if (env.isAbsent(GO_SERVER_DASHBOARD_URL)) return envNotFound(GO_SERVER_DASHBOARD_URL);

            final String bucket = env.get(GO_ARTIFACTS_S3_BUCKET);
            final S3ArtifactStore store = getS3ArtifactStore(env, bucket);
            store.setStorageClass(env.getOrElse(AWS_STORAGE_CLASS, STORAGE_CLASS_STANDARD));

            final String destinationPrefix = getDestinationPrefix(config, env);

            List<SourceDestination> sourceDestinations = config.sourceDestinations();
            for(SourceDestination input : sourceDestinations) {
                String[] files = parseSourcePath(input.source, context.getWorkingDir());
                if (files.length == 0) {
                    return new TaskExecutionResult(false, String.format("Source %s didn't yield any files to upload", input.source));
                }
                for (String includedFile : files) {
                    File localFileToUpload = new File(String.format("%s/%s", context.getWorkingDir(), includedFile));
                    pushToS3(context, destinationPrefix, store, localFileToUpload, input.destination);
                }
            }

            if(!hasConfigDestinationPrefix(config)) {
                setMetadata(env, bucket, destinationPrefix, store);
            }

            return new TaskExecutionResult(true, "Published all artifacts to S3 successfully");
        } catch (JsonSyntaxException e) {
            String message = "Failed while parsing configuration";
            logger.error(message);
            return new TaskExecutionResult(false, message, e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new TaskExecutionResult(false, e.getMessage());
        }
    }

    protected S3ArtifactStore getS3ArtifactStore(GoEnvironment env, String bucket) {
        return new S3ArtifactStore(env, bucket);
    }

    protected String[] parseSourcePath(String source, String workingDir) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(workingDir);
        directoryScanner.setIncludes(new String[]{source});
        directoryScanner.scan();
        return ArrayUtils.addAll(directoryScanner.getIncludedFiles(), directoryScanner.getIncludedDirectories());
    }

    protected boolean fileExists(File localFileToUpload) {
        return localFileToUpload.exists();
    }

    private void pushToS3(final Context context, final String destinationPrefix, final S3ArtifactStore store, File localFileToUpload, String destination) {
        String templateSoFar = ensureKeySegmentValid(destinationPrefix);
        if(!StringUtils.isBlank(destination)) {
            templateSoFar += destination;
        }
        List<FilePathToTemplate> filesToUpload = generateFilesToUpload(templateSoFar, localFileToUpload);
        foreach(filesToUpload, new VoidFunction<FilePathToTemplate>() {
            @Override
            public void execute(FilePathToTemplate filePathToTemplate) {
                String localFile = filePathToTemplate._1();
                String destinationOnS3 = filePathToTemplate._2();
                context.printMessage(String.format("Pushing %s to %s", localFile, store.pathString(destinationOnS3)));
                store.put(localFile, destinationOnS3);
                context.printMessage(String.format("Pushed %s to %s", localFile, store.pathString(destinationOnS3)));
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

    private TaskExecutionResult envNotFound(String environmentVariable) {
        String message = String.format("%s environment variable is not set", environmentVariable);
        logger.error(message);
        return new TaskExecutionResult(false, message);
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

    private String getConfigDestinationPrefix(final Config config) {
        return config.destinationPrefix;
    }

    private boolean hasConfigDestinationPrefix(final Config config) {
        String destinationPrefix = getConfigDestinationPrefix(config);

        return !StringUtils.isBlank(destinationPrefix);
    }

    private String getDestinationPrefix(final Config config, final GoEnvironment env) {
        if(!hasConfigDestinationPrefix(config)) {
            return env.artifactsLocationTemplate();
        }

        String destinationPrefix = getConfigDestinationPrefix(config);

        destinationPrefix = env.replaceVariables(destinationPrefix);

        if(destinationPrefix.endsWith("/")) {
            destinationPrefix = destinationPrefix.substring(0, destinationPrefix.length() - 1);
        }

        return destinationPrefix;
    }

    private String ensureKeySegmentValid(String segment) {
        if(StringUtils.isBlank(segment)) {
            return segment;
        }

        if(!StringUtils.endsWith(segment, "/")) {
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
