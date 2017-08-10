package com.indix.gocd.s3fetch;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.mocks.MockContext;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PipelineFetchExecutorTest {

    private final String bucket = "gocd";
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private FetchExecutor fetchExecutor;
    private Config config;

    @Before
    public void setUp() throws Exception {
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .with(AWS_ACCESS_KEY_ID, "accessId")
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_DEPENDENCY_LOCATOR_MYMATERIAL", "pipeline/1/stage/1");

        config = new Config(Maps.builder()
                .with(Constants.MATERIAL_TYPE, Maps.builder().with("value", "Pipeline").build())
                .with(Constants.MATERIAL, Maps.builder().with("value", "mymaterial").build())
                .with(Constants.JOB, Maps.builder().with("value", "job").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());

        fetchExecutor = spy(new PipelineFetchExecutor());
    }

    @Test
    public void shouldBeFailureIfFetchConfigNotValid() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        config = new Config(Maps.builder()
                .with(Constants.MATERIAL, Maps.builder().with("value", "Wrong").build())
                .with(Constants.JOB, Maps.builder().with("value", "job").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        AmazonS3Client mockClient = mockClient();
        S3ArtifactStore store = new S3ArtifactStore(mockClient, bucket);
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));

        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables));

        assertFalse(result.isSuccessful());
        assertThat(result.message(), is("Failure while downloading artifacts - Please check Material name configuration."));
    }

    @Test
    public void shouldBeFailureIfUnableToFetchArtifacts() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        AmazonS3Client mockClient = mockClient();
        S3ArtifactStore store = new S3ArtifactStore(mockClient, bucket);
        doThrow(new AmazonClientException("Exception message")).when(mockClient).listObjects(any(ListObjectsRequest.class));
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));

        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables));

        assertFalse(result.isSuccessful());
        assertThat(result.message(), is("Failure while downloading artifacts - Exception message"));
    }

    @Test
    public void shouldBeSuccessResultONSuccessfulFetch() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        S3ArtifactStore mockStore = mockStore();

        doReturn(mockStore).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables) );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(mockStore, times(1)).getPrefix("pipeline/stage/job/1.1", "here/artifacts");
    }

    @Test
    public void shouldBeAbleToHandleTaskConfigEntriesWithDashesInTheName() {
        Map<String, String> mockVariables = mockEnvironmentVariables
                .with("GO_DEPENDENCY_LOCATOR_MY_MATERIAL", "pipeline/1/stage/1")
                .build();
        S3ArtifactStore store = mockStore();
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));

        config = new Config(Maps.builder()
                .with(Constants.MATERIAL, Maps.builder().with("value", "my-material").build())
                .with(Constants.JOB, Maps.builder().with("value", "job").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables));

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(store, times(1)).getPrefix("pipeline/stage/job/1.1", "here/artifacts");
    }

    @Test
    public void shouldBeAbleToHandleTaskConfigEntriesWithPeriodsInTheName() {
        Map<String, String> mockVariables = mockEnvironmentVariables
                .with("GO_DEPENDENCY_LOCATOR_MY_MATERIAL", "pipeline/1/stage/1")
                .build();
        S3ArtifactStore store = mockStore();
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));

        config = new Config(Maps.builder()
                .with(Constants.MATERIAL, Maps.builder().with("value", "my.material").build())
                .with(Constants.JOB, Maps.builder().with("value", "job").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables));

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(store, times(1)).getPrefix("pipeline/stage/job/1.1", "here/artifacts");
    }

    @Test
    public void shouldBeAbleToHandleTaskConfigEntriesWithSpecialCharactersInTheName() {
        Map<String, String> mockVariables = mockEnvironmentVariables
                .with("GO_DEPENDENCY_LOCATOR_MY_______________________________MATERIAL", "pipeline/1/stage/1")
                .build();
        S3ArtifactStore store = mockStore();
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));

        config = new Config(Maps.builder()
                .with(Constants.MATERIAL, Maps.builder().with("value", "my`~!@#$%^&*()-+=[{]}\\|;:'\",<.>/?material").build())
                .with(Constants.JOB, Maps.builder().with("value", "job").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables));

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(store, times(1)).getPrefix("pipeline/stage/job/1.1", "here/artifacts");
    }

    private Context mockContext(final Map<String, String> environmentMap) {
        Map<String, Object> contextMap = Maps.<String, Object>builder()
                .with("environmentVariables", environmentMap)
                .with("workingDirectory", "here")
                .build();
        return new MockContext(contextMap);
    }

    private S3ArtifactStore mockStore() { return mock(S3ArtifactStore.class); }

    private AmazonS3Client mockClient() { return mock(AmazonS3Client.class); }

}
