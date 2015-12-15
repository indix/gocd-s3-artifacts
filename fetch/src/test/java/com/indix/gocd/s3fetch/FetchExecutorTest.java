package com.indix.gocd.s3fetch;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.indix.gocd.utils.AWSCredentialsFactory;
import com.indix.gocd.utils.mocks.MockTaskExecutionContext;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FetchExecutorTest {
    private final String destination = "artifacts";
    private final String bucket = "gocd";
    Maps.MapBuilder<String, String> mockEnvironmentVariables;

    @Mock
    private TaskConfig config;

    @Mock
    private AmazonS3Client s3ClientMock;

    @Mock
    private AWSCredentialsFactory awsCredentialsFactory;

    @Spy
    @InjectMocks
    private FetchExecutor fetchExecutor = new FetchExecutor();

    @Before
    public void setUp() throws Exception {
        when(config.getValue(FetchTask.REPO)).thenReturn(bucket);
        when(config.getValue(FetchTask.PACKAGE)).thenReturn("TestPublishS3Artifacts");
        when(config.getValue(FetchTask.DESTINATION)).thenReturn(destination);
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .with(AWS_ACCESS_KEY_ID, "accessId")
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_LABEL", "20.1")
                .with("GO_REPO_GOCD_TESTPUBLISHS3ARTIFACTS_S3_BUCKET", bucket)
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_PIPELINE_NAME", "TestPublish")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_STAGE_NAME", "defaultStage")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_JOB_NAME", "defaultJob");

    }

    @Test
    public void shouldBeFailureIfFetchConfigNotValid() {
        Map<String, String> mockVariables = mockEnvironmentVariables.remove(AWS_ACCESS_KEY_ID).build();

        ExecutionResult executionResult = fetchExecutor.execute(config, mockContext(mockVariables));
        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("[AWS_ACCESS_KEY_ID environment variable not present]"));
    }

    @Test
    public void shouldBeFailureIfUnableToFetchArtifacts() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        doReturn(s3ClientMock).when(fetchExecutor).s3Client(any(AWSCredentialsFactory.class));
        doThrow(new AmazonClientException("Exception message")).when(s3ClientMock).listObjects(any(ListObjectsRequest.class));

        ExecutionResult executionResult = fetchExecutor.execute(config, mockContext(mockVariables));

        assertFalse(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("Failure while downloading artifacts - Exception message"));
    }

    @Test
    public void shouldBeSuccessResultOnSuccessfulFetch() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        S3ArtifactStore mockStore = mockStore();
        doReturn(mockStore).when(fetchExecutor).s3ArtifactStore(any(FetchConfig.class),any(AWSCredentialsFactory.class));

        ExecutionResult executionResult = fetchExecutor.execute(config, mockContext(mockVariables));

        assertTrue(executionResult.isSuccessful());
        assertThat(executionResult.getMessagesForDisplay(), is("Fetched all artifacts"));
        verify(mockStore, times(1)).getPrefix("TestPublish/defaultStage/defaultJob/20.1", "./artifacts");

    }

    private TaskExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap);
    }

    private S3ArtifactStore mockStore() { return mock(S3ArtifactStore.class); }

}