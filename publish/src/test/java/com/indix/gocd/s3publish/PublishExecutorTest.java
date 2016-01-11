package com.indix.gocd.s3publish;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.mocks.MockTaskExecutionContext;
import com.indix.gocd.utils.utils.Maps;
import com.indix.gocd.utils.zip.IZipArchiveManager;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class PublishExecutorTest {
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private PublishExecutor publishExecutor;
    private TaskConfig config;

    @Before
    public void setUp() throws Exception {
        config = mock(TaskConfig.class);
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .with(AWS_ACCESS_KEY_ID, "accessId")
                .with(GO_ARTIFACTS_S3_BUCKET, "testS3Bucket")
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_PIPELINE_NAME", "pipeline")
                .with("GO_STAGE_NAME", "stage")
                .with("GO_JOB_NAME", "job")
                .with("GO_PIPELINE_COUNTER", "pipelineCounter")
                .with("GO_STAGE_COUNTER", "stageCounter")
                .with("GO_SERVER_URL", "http://localhost:8153/go")
                .with("GO_TRIGGER_USER", "Krishna");

        publishExecutor = spy(new PublishExecutor());
    }

    @Test
    public void shouldThrowIfS3ClientCannotBeInitialized() {
        Map<String, String> mockVariables = mockEnvironmentVariables.remove(AWS_ACCESS_KEY_ID).build();

        doThrow(new IllegalArgumentException("AWS_ACCESS_KEY_ID environment variable not present"))
                .when(publishExecutor).getS3Client(any(GoEnvironment.class));
        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("AWS_ACCESS_KEY_ID environment variable not present"));
    }

    @Test
    public void shouldThrowIfGO_ARTIFACTS_S3_BUCKETNotPresent() {
        Map<String, String> mockVariables = mockEnvironmentVariables.remove(GO_ARTIFACTS_S3_BUCKET).build();

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("GO_ARTIFACTS_S3_BUCKET environment variable not present"));
    }

    @Test
    public void shouldUploadALocalFileToS3() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        AmazonS3Client mockClient = mockClient();
        doReturn(mockClient).when(publishExecutor).getS3Client(any(GoEnvironment.class));
        when(config.getValue(SOURCEDESTINATIONS)).thenReturn("[{\"source\": \"target/*\", \"destination\": \"\"}]");
        doReturn(new String[]{"README.md", "s3publish-0.1.31.jar"}).when(publishExecutor).parseSourcePath(anyString(), anyString());

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertTrue(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("Published all artifacts to S3"));

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockClient, times(3)).putObject(putObjectRequestArgumentCaptor.capture());
        List<PutObjectRequest> allPutObjectRequests = putObjectRequestArgumentCaptor.getAllValues();


        PutObjectRequest filePutRequest = allPutObjectRequests.get(0);
        assertThat(filePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(filePutRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/README.md"));

        PutObjectRequest metadataPutRequest = allPutObjectRequests.get(2);
        Map<String, String> expectedUserMetadata = Maps.<String, String>builder()
                .with(METADATA_USER, "Krishna")
                .with(METADATA_TRACEBACK_URL, "http://go.server:8153/go/tab/build/detail/pipeline/pipelineCounter/stage/stageCounter/job")
                .with(COMPLETED, COMPLETED)
                .build();
        assertThat(metadataPutRequest.getMetadata().getUserMetadata(), is(expectedUserMetadata));

        assertNull(filePutRequest.getMetadata());
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
    public void shouldUploadCompressedArchiveToS3() {
        Map<String, String> mockVariables = mockEnvironmentVariables.with(GO_ARTIFACTS_COMPRESS_IN_S3,"True").build();
        AmazonS3Client mockClient = mockClient();
        doReturn(mockClient).when(publishExecutor).getS3Client(any(GoEnvironment.class));
        doReturn(mock(IZipArchiveManager.class)).when(publishExecutor).getZipArchiveManager();
        when(config.getValue(SOURCEDESTINATIONS)).thenReturn("[{\"source\": \"target/*\", \"destination\": \"\"}]");

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertTrue(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("Published all artifacts to S3"));

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockClient, times(2)).putObject(putObjectRequestArgumentCaptor.capture());
        List<PutObjectRequest> allPutObjectRequests = putObjectRequestArgumentCaptor.getAllValues();


        PutObjectRequest filePutRequest = allPutObjectRequests.get(0);
        assertThat(filePutRequest.getBucketName(), is("testS3Bucket"));
        assertThat(filePutRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/artifacts.zip"));

        PutObjectRequest metadataPutRequest = allPutObjectRequests.get(1);
        Map<String, String> expectedUserMetadata = Maps.<String, String>builder()
                .with(METADATA_USER, "Krishna")
                .with(METADATA_TRACEBACK_URL, "http://go.server:8153/go/tab/build/detail/pipeline/pipelineCounter/stage/stageCounter/job")
                .with(COMPLETED, COMPLETED)
                .build();
        assertThat(metadataPutRequest.getMetadata().getUserMetadata(), is(expectedUserMetadata));
    }

    private TaskExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap);
    }

    private AmazonS3Client mockClient() {
        return mock(AmazonS3Client.class);
    }
}