package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.Context;
import com.indix.gocd.utils.TaskExecutionResult;
import com.indix.gocd.utils.mocks.MockContext;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SelfFetchExecutorTest {

    private Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private FetchExecutor fetchExecutor;
    private Config config;
    private S3ArtifactStore store;

    private final String PIPELINE = "pipeline";
    private final String PIPELINE_COUNTER = "1";
    private final String STAGE = "stage";
    private final String JOB = "job";

    @Before
    public void setUp() throws Exception {
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .with(AWS_ACCESS_KEY_ID, "accessId")
                .with(GO_ARTIFACTS_S3_BUCKET, "bucket")
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_PIPELINE_NAME", PIPELINE)
                .with("GO_PIPELINE_COUNTER", PIPELINE_COUNTER);

        config = new Config(Maps.builder()
                .with(Constants.STAGE, Maps.builder().with("value", STAGE).build())
                .with(Constants.JOB, Maps.builder().with("value", JOB).build())
                .with(Constants.SOURCE, Maps.builder().with("value", "source").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());

        store = mock(S3ArtifactStore.class);
        fetchExecutor = spy(new SelfFetchExecutor());
        doReturn(store).when(fetchExecutor).getS3ArtifactStore(any(), any());
    }

    @Test
    public void shouldBeFailureIfCouldntFindS3Path() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        doReturn(null).when(store).getLatestPrefix(PIPELINE, STAGE, JOB, PIPELINE_COUNTER);
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables) );

        assertFalse(result.isSuccessful());
        assertEquals("Failure while downloading artifacts - Could not determine stage counter on s3 with path: s3://bucket/pipeline/stage/job/1.", result.message());
    }

    @Test
    public void shouldBeSuccessWhenAbleToFindSS3Path() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        doReturn("sourcePrefix").when(store).getLatestPrefix(PIPELINE, STAGE, JOB, PIPELINE_COUNTER);
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables) );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(store).getPrefix("sourcePrefix/source", "here/artifacts");
    }

    @Test
    public void shouldBeSuccessWhenCustomPrefixProvided() {
        Map<String, String> mockVariables = mockEnvironmentVariables.build();
        config = new Config(Maps.builder()
                .with(Constants.SOURCE, Maps.builder().with("value", "source").build())
                .with(Constants.SOURCE_PREFIX, Maps.builder().with("value", "sourcePrefix").build())
                .with(Constants.DESTINATION, Maps.builder().with("value", "artifacts").build())
                .build());
        TaskExecutionResult result = fetchExecutor.execute(config, mockContext(mockVariables) );

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Fetched all artifacts"));
        verify(store).getPrefix("sourcePrefix/source", "here/artifacts");
    }

    private Context mockContext(final Map<String, String> environmentMap) {
        Map<String, Object> contextMap = Maps.<String, Object>builder()
                .with("environmentVariables", environmentMap)
                .with("workingDirectory", "here")
                .build();
        return new MockContext(contextMap);
    }
}
