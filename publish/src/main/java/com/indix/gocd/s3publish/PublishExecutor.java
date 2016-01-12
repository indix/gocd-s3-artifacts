package com.indix.gocd.s3publish;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.amazonaws.util.json.JSONException;
import com.indix.gocd.utils.AWSCredentialsFactory;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.zip.IZipArchiveManager;
import com.indix.gocd.utils.zip.ZipArchiveManager;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
    private IZipArchiveManager zipArchiveManager = getZipArchiveManager();

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
        String kmsKey = null;
        if (env.has(AWS_KMS_KEY_ID))
            kmsKey = env.get(AWS_KMS_KEY_ID);
        final S3ArtifactStore store = new S3ArtifactStore(s3Client, bucket, kmsKey);

        try {
            List<Tuple2<String, String>> sourceDestinations = PublishTask.getSourceDestinations(config.getValue(SOURCEDESTINATIONS));
            foreach(sourceDestinations, new Functions.VoidFunction<Tuple2<String, String>>() {
                @Override
                public void execute(Tuple2<String, String> input) {
                    final String source = input._1();
                    final String destination = input._2();

                    if (CompressArtifactsInS3(env)) {
                        try {
                            CompressDirectoryStructureAndUploadArchiveToS3(source, destination, context, env, store);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed while compressing artifacts", e);
                        }
                    } else {
                        UploadDirectoryStructureToS3(source, destination, context, env, store);
                    }
                }
            });
        } catch (JSONException e) {
            String message = "Failed while parsing configuration";
            log.error(message);
            return ExecutionResult.failure(message, e);
        }
        catch (RuntimeException e) {
            log.error(e.getMessage());
            return ExecutionResult.failure(e.getMessage(), e);
        }
        setMetadata(env, bucket, store);

        return ExecutionResult.success("Published all artifacts to S3");
    }

    private void UploadDirectoryStructureToS3(String source, final String destination, final TaskExecutionContext context, final GoEnvironment env, final S3ArtifactStore store) {
        String[] files = parseSourcePath(source, context.workingDir());

        foreach(files, new VoidFunction<String>() {
            @Override
            public void execute(String includedFile) {
                File localFileToUpload = new File(String.format("%s/%s", context.workingDir(), includedFile));
                pushToS3(context, env, store, localFileToUpload, destination);
            }
        });
    }

    private void CompressDirectoryStructureAndUploadArchiveToS3(String source, final String destination, final TaskExecutionContext context, final GoEnvironment env, final S3ArtifactStore store)
            throws IOException
    {
        File zipFile = CompressSourceIntoDestinationZipFile(String.format("%s/%s", context.workingDir(), source), zipArchiveManager);

        pushToS3(context, env, store, zipFile, destination);

        CleanUpZip(zipFile);
    }

    private void CleanUpZip(File zipFile) {
        try {
            zipFile.getParentFile().delete();
        } catch (RuntimeException e)
        {
            log.warn(String.format("Could not delete zip file folder %s", zipFile.getAbsolutePath()));
        }
    }

    private File CompressSourceIntoDestinationZipFile(String source, IZipArchiveManager zipArchiveManager) throws IOException {
        String zipFilePath = Files.createTempDirectory("gos3").toString().concat("/").concat(GO_ARTIFACTS_ARCHIVE_FILENAME);
        String sourcePath = source;
        if (sourcePath.endsWith("/")) {
            sourcePath = sourcePath.substring(0,sourcePath.length()-1);
        }
        if (sourcePath.endsWith("/*")) {
            sourcePath = sourcePath.substring(0,sourcePath.length()-2);
        }

        log.info(String.format("Compressing %s into %s", sourcePath, zipFilePath));
        zipArchiveManager.compressDirectory(sourcePath, zipFilePath);
        return new File(zipFilePath);
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

    public IZipArchiveManager getZipArchiveManager() {
        return new ZipArchiveManager();
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

    private static final List<String> validCompressArtifactsInS3Values = new ArrayList<String>(Arrays.asList("true","false","yes","no","1","0"));
    private static final List<String> affirmativeCompressArtifactsInS3Values = new ArrayList<String>(Arrays.asList("true","yes","1"));
    private boolean CompressArtifactsInS3(GoEnvironment env) {
        if (env.has(GO_ARTIFACTS_COMPRESS_IN_S3)) {
            String compressArtifactsValue = env.get(GO_ARTIFACTS_COMPRESS_IN_S3);
            if (affirmativeCompressArtifactsInS3Values.contains(compressArtifactsValue.toLowerCase())) {
                log.debug(String.format("GO_ARTIFACTS_COMPRESS_IN_S3=%s",compressArtifactsValue));
                return true;
            }
            else if (!validCompressArtifactsInS3Values.contains(compressArtifactsValue.toLowerCase())) {
                throw new IllegalArgumentException(getEnvInvalidFormatMessage(GO_ARTIFACTS_COMPRESS_IN_S3,
                        compressArtifactsValue, validCompressArtifactsInS3Values.toString()));
            }
        }
        return false;
    }

    private String getEnvInvalidFormatMessage(String environmentVariable, String value, String expected) {
        return String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
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
