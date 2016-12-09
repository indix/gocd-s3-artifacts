package com.indix.gocd.s3fetch;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.mocks.MockTaskExecutionContext;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionConfiguration;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionContext;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionResult;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class FetchExecutorTest {
    private final String destination = "artifacts";
    private final String bucket = "gocd";
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private FetchExecutor fetchExecutor;
    private ExecutionConfiguration config;
    private JobConsoleLogger logger;

    @Before
    public void setUp() throws Exception {
        config = mock(ExecutionConfiguration.class);
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn(bucket);
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("TestPublishS3Artifacts");
        when(config.getProperty(FetchConfigEnum.DESTINATION.name())).thenReturn(destination);
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

        fetchExecutor = spy(new FetchExecutor());
    }

    @Test
    public void shouldBeFailureIfFetchConfigNotValid() {
        Map<String, String> mockVariables = mockEnvironmentVariables.with(AWS_ACCESS_KEY_ID, "").build();
        ExecutionContext mockContext = mockContext(mockVariables);
        FetchConfig fetchConfig = spy(new FetchConfig(config, mockContext, new GoEnvironment(new HashMap<String,String>())));
        doReturn(fetchConfig).when(fetchExecutor).getFetchConfig(any(ExecutionConfiguration.class), any(ExecutionContext.class));

        ExecutionResult executionResult = fetchExecutor.execute(mockContext(mockVariables), config, logger);

        assertFalse(executionResult.isSuccess());
        assertThat(executionResult.getMessage(), is("[AWS_ACCESS_KEY_ID environment variable not present]"));
    }

    @Test
    public void shouldBeFailureIfUnableToFetchArtifacts() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        AmazonS3Client mockClient = mockClient();
        doReturn(mockClient).when(fetchExecutor).s3Client(any(FetchConfig.class));
        doThrow(new AmazonClientException("Exception message")).when(mockClient).listObjects(any(ListObjectsRequest.class));

        ExecutionResult executionResult = fetchExecutor.execute(mockContext(mockVariables), config, logger);

        assertFalse(executionResult.isSuccess());
        assertThat(executionResult.getMessage(), is("Failure while downloading artifacts - Exception message"));
    }

    @Test
    public void shouldBeSuccessResultOnSuccessfulFetch() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        S3ArtifactStore mockStore = mockStore();
        doReturn(mockStore).when(fetchExecutor).s3ArtifactStore(any(FetchConfig.class));

        ExecutionResult executionResult = fetchExecutor.execute(mockContext(mockVariables), config, logger);

        assertTrue(executionResult.isSuccess());
        assertThat(executionResult.getMessage(), is("Fetched all artifacts"));
        verify(mockStore, times(1)).getPrefix("TestPublish/defaultStage/defaultJob/20.1", "./artifacts");

    }

    private ExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap);
    }

    private S3ArtifactStore mockStore() { return mock(S3ArtifactStore.class); }

    private AmazonS3Client mockClient() { return mock(AmazonS3Client.class); }
}