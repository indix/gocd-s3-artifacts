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

public class StageFetchExecutorTest {

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
                .with("GO_PIPELINE_NAME", "pipeline")
                .with("GO_PIPELINE_COUNTER", "1");

        config = new Config(Maps.builder()
                .with(Constants.STAGE_NAME, Maps.builder().with("value", "stageName").build())
                .with(Constants.STAGE_JOB, Maps.builder().with("value", "stageJob").build())
                .with(Constants.STAGE_SOURCE, Maps.builder().with("value", "stageSource").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());

        fetchExecutor = spy(new StageFetchExecutor());
    }

    @Test
    public void shouldBeFailureIfCouldntFindS3Path() {
        assertFalse(true);
    }

    @Test
    public void shouldBeAbleToFindStageCounterIfOnlyThanOne() {
        assertFalse(true);
    }

    @Test
    public void shouldBeAbleToFindLatestStageCounterIfMoreThanOne() {
        assertFalse(true);
    }

    @Test
    public void shouldUseCustomPrefixIfProvided() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        config = new Config(Maps.builder()
                .with(Constants.STAGE_SOURCE, Maps.builder().with("value", "stageSource").build())
                .with(Constants.STAGE_SOURCE_PREFIX, Maps.builder().with("value", "stageSourcePrefix").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        S3ArtifactStore mockStore = mockStore();

        doReturn(mockStore).when(fetchExecutor).getS3ArtifactStore(any(GoEnvironment.class), eq(bucket));
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables) );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(mockStore, times(1)).getPrefix("stageSourcePrefix/stageSource", "here/artifacts");
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
