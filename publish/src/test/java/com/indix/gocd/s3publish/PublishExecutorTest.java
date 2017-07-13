package com.indix.gocd.s3publish;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.mocks.MockContext;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class PublishExecutorTest {
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private PublishExecutor publishExecutor;
    private Config config;
    private S3ArtifactStore store;
    private String testS3Bucket = "testS3Bucket";


    @Before
    public void setUp() throws Exception {
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .with(AWS_ACCESS_KEY_ID, "accessId")
                .with(GO_ARTIFACTS_S3_BUCKET, testS3Bucket)
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_PIPELINE_NAME", "pipeline")
                .with("GO_STAGE_NAME", "stage")
                .with("GO_JOB_NAME", "job")
                .with("GO_PIPELINE_COUNTER", "pipelineCounter")
                .with("GO_STAGE_COUNTER", "stageCounter")
                .with("GO_SERVER_URL", "http://localhost:8153/go")
                .with(GO_PIPELINE_LABEL, "1.2.3")
                .with("GO_TRIGGER_USER", "Krishna");

        publishExecutor = spy(new PublishExecutor());
        doReturn(true).when(publishExecutor).fileExists(any(File.class));
    }

    @Test
    public void shouldFailIfNoFilesToUploadBasedOnSource() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{}
        );

        assertFalse(result.isSuccessful());
        assertThat(result.message(), containsString("Source target/* didn't yield any files to upload"));
    }

    @Test
    public void shouldNotThrowIfAWSUseIAMRoleIsTrueAndAWS_SECRET_ACCESS_KEYNotPresent() {
        Maps.MapBuilder<String, String> mockVariables = mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE, "True")
                .with(AWS_ACCESS_KEY_ID, "")
                .with(AWS_SECRET_ACCESS_KEY, "");
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md"},
                mockVariables
        );

        assertTrue(result.isSuccessful());
    }


    @Test
    public void shouldThrowIfGO_ARTIFACTS_S3_BUCKETNotPresent() {
        Map<String, String> mockVariables = mockEnvironmentVariables.with(GO_ARTIFACTS_S3_BUCKET, "").build();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = publishExecutor.execute(config, mockContext(mockVariables));
        assertFalse(result.isSuccessful());
        assertThat(result.message(), is("GO_ARTIFACTS_S3_BUCKET environment variable is not set"));
    }

    @Test
    public void shouldGetBucketFromConfig() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", testS3Bucket).build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md"},
                mockEnvironmentVariables.with(GO_ARTIFACTS_S3_BUCKET, "")
        );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Published all artifacts to S3 successfully"));
    }

    @Test
    public void shouldGetDisplayMessageAfterUpload() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md"}
        );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Published all artifacts to S3 successfully"));
    }

    @Test
    public void shouldUploadALocalFileToS3WithDefaultPrefix() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md", "s3publish-0.1.31.jar"}
        );

        assertTrue(result.isSuccessful());

        final List<PutObjectRequest> allPutObjectRequests = getPutObjectRequests(mockClient, 3);

        PutObjectRequest metadataPutRequest = allPutObjectRequests.get(2);
        Map<String, String> expectedUserMetadata = Maps.<String, String>builder()
                .with(METADATA_USER, "Krishna")
                .with(METADATA_TRACEBACK_URL, "http://go.server:8153/go/tab/build/detail/pipeline/pipelineCounter/stage/stageCounter/job")
                .with(COMPLETED, COMPLETED)
                .with(GO_PIPELINE_LABEL, "1.2.3")
                .build();
        assertThat(metadataPutRequest.getMetadata().getUserMetadata(), is(expectedUserMetadata));
        assertThat(metadataPutRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/"));

        PutObjectRequest readmePutRequest = allPutObjectRequests.get(0);
        assertThat(readmePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(readmePutRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/README.md"));
        assertNull(readmePutRequest.getMetadata());

        PutObjectRequest jarPutRequest = allPutObjectRequests.get(1);
        assertNull(jarPutRequest.getMetadata());
        assertThat(jarPutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(jarPutRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/s3publish-0.1.31.jar"));
        assertNull(jarPutRequest.getMetadata());
    }


    @Test
    public void shouldUploadALocalFileToS3WithDestinationPrefix() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "destinationPrefix").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md", "s3publish-0.1.31.jar"}
        );

        assertTrue(result.isSuccessful());

        final List<PutObjectRequest> allPutObjectRequests = getPutObjectRequests(mockClient, 2);

        PutObjectRequest readmePutRequest = allPutObjectRequests.get(0);
        assertThat(readmePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(readmePutRequest.getKey(), is("destinationPrefix/README.md"));
        assertNull(readmePutRequest.getMetadata());

        PutObjectRequest jarPutRequest = allPutObjectRequests.get(1);
        assertNull(jarPutRequest.getMetadata());
        assertThat(jarPutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(jarPutRequest.getKey(), is("destinationPrefix/s3publish-0.1.31.jar"));
        assertNull(jarPutRequest.getMetadata());
    }

    @Test
    public void shouldUploadALocalFileToS3WithDestinationPrefixUsingEnvVariable() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "test/${GO_PIPELINE_COUNTER}/").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md", "s3publish-0.1.31.jar"}
        );

        assertTrue(result.isSuccessful());

        final List<PutObjectRequest> allPutObjectRequests = getPutObjectRequests(mockClient, 2);

        PutObjectRequest readmePutRequest = allPutObjectRequests.get(0);
        assertThat(readmePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(readmePutRequest.getKey(), is("test/pipelineCounter/README.md"));
        assertNull(readmePutRequest.getMetadata());

        PutObjectRequest jarPutRequest = allPutObjectRequests.get(1);
        assertNull(jarPutRequest.getMetadata());
        assertThat(jarPutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(jarPutRequest.getKey(), is("test/pipelineCounter/s3publish-0.1.31.jar"));
        assertNull(jarPutRequest.getMetadata());
    }

    @Test
    public void shouldUploadALocalFileToS3WithSlashDestinationPrefix() {
        AmazonS3Client mockClient = mockClient();

        Config config = new Config(Maps.builder()
                .with(Constants.SOURCEDESTINATIONS, Maps.builder().with("value", "[{\"source\": \"target/*\", \"destination\": \"\"}]").build())
                .with(Constants.DESTINATION_PREFIX, Maps.builder().with("value", "/").build())
                .with(Constants.ARTIFACTS_BUCKET, Maps.builder().with("value", "").build())
                .build());

        TaskExecutionResult result = executeMockPublish(
                mockClient,
                config,
                new String[]{"README.md", "s3publish-0.1.31.jar"}
        );

        assertTrue(result.isSuccessful());

        final List<PutObjectRequest> allPutObjectRequests = getPutObjectRequests(mockClient, 2);

        PutObjectRequest readmePutRequest = allPutObjectRequests.get(0);
        assertThat(readmePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(readmePutRequest.getKey(), is("README.md"));
        assertNull(readmePutRequest.getMetadata());

        PutObjectRequest jarPutRequest = allPutObjectRequests.get(1);
        assertNull(jarPutRequest.getMetadata());
        assertThat(jarPutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(jarPutRequest.getKey(), is("s3publish-0.1.31.jar"));
        assertNull(jarPutRequest.getMetadata());
    }

    private TaskExecutionResult executeMockPublish(final AmazonS3Client mockClient, Config config, String[] files) {
        return executeMockPublish(mockClient, config, files, mockEnvironmentVariables);
    }

    private TaskExecutionResult executeMockPublish(final AmazonS3Client mockClient, Config config, String[] files,
                                                   Maps.MapBuilder<String, String> mockVariablesBuilder) {
        Map<String, String> mockVariables = mockVariablesBuilder.build();

        store = new S3ArtifactStore(mockClient, testS3Bucket);

        doReturn(store).when(publishExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(testS3Bucket));
        doReturn(files).when(publishExecutor).parseSourcePath(anyString(), anyString());

        return publishExecutor.execute(config, mockContext(mockVariables));
    }

    private List<PutObjectRequest> getPutObjectRequests(AmazonS3Client mockClient, int expectedRequestsCount) {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockClient, times(expectedRequestsCount)).putObject(putObjectRequestArgumentCaptor.capture());
        List<PutObjectRequest> allPutObjectRequests = putObjectRequestArgumentCaptor.getAllValues();

        return allPutObjectRequests;
    }
    private Context mockContext(final Map<String, String> environmentMap) {
        Map<String, Object> contextMap = Maps.<String, Object>builder()
                .with("environmentVariables", environmentMap)
                .with("workingDirectory", "here")
                .build();
        return new MockContext(contextMap);
    }

    private AmazonS3Client mockClient() {
        return mock(AmazonS3Client.class);
    }
}
