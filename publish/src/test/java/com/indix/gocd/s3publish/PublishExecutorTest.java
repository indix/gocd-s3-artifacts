package com.indix.gocd.s3publish;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.indix.gocd.s3publish.mocks.MockTaskExecutionContext;
import com.indix.gocd.s3publish.utils.Maps;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static com.indix.gocd.s3publish.Constants.*;
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
    public void shouldThrowIfAWS_ACCESS_KEY_IDNotPresent() {
        Map<String, String> mockVariables = mockEnvironmentVariables.remove(AWS_ACCESS_KEY_ID).build();

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("AWS_ACCESS_KEY_ID environment variable not present"));
    }

    @Test
    public void shouldThrowIfAWS_SECRET_ACCESS_KEYNotPresent() {
        Map<String, String> mockVariables = mockEnvironmentVariables.remove(AWS_SECRET_ACCESS_KEY).build();

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("AWS_SECRET_ACCESS_KEY environment variable not present"));
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
        doReturn(mockClient).when(publishExecutor).s3Client(any(GoEnvironment.class));
        when(config.getValue(SOURCE)).thenReturn("README.md");

        ExecutionResult executionResult = publishExecutor.execute(config, mockContext(mockVariables));
        assertTrue(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("Published all artifacts to S3"));

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockClient).putObject(putObjectRequestArgumentCaptor.capture());
        PutObjectRequest putRequest = putObjectRequestArgumentCaptor.getValue();

        assertThat(putRequest.getBucketName(), is("testS3Bucket"));
        assertThat(putRequest.getKey(), is("pipeline/stage/job/pipelineCounter.stageCounter/README.md"));
        Map<String, String> expectedUserMetadata = Maps.<String, String>builder()
                .with(METADATA_USER, "Krishna")
                .with(METADATA_TRACEBACK_URL, "http://localhost:8153/go/tab/build/detail/pipeline/pipelineCounter/stage/stageCounter/job")
                .build();
        assertThat(putRequest.getMetadata().getUserMetadata(), is(expectedUserMetadata));
    }

    private TaskExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap, "/tmp/data/");
    }

    private AmazonS3Client mockClient() {
        return mock(AmazonS3Client.class);
    }
}