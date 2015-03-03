package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.mocks.MockTaskExecutionContext;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetchConfigTest {
    private final String bucket = "gocd";
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private TaskConfig config;
    private FetchConfig fetchConfig;
    private final String secretKey = "secretKey";
    private final String accessId = "accessId";

    @Before
    public void setUp() throws Exception {
        config = mock(TaskConfig.class);
        when(config.getValue(FetchTask.REPO)).thenReturn(bucket);
        when(config.getValue(FetchTask.PACKAGE)).thenReturn("TestPublishS3Artifacts");
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, secretKey)
                .with(AWS_ACCESS_KEY_ID, accessId)
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_LABEL", "20.1")
                .with("GO_REPO_GOCD_TESTPUBLISHS3ARTIFACTS_S3_BUCKET", bucket)
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_PIPELINE_NAME", "TestPublish")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_STAGE_NAME", "defaultStage")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_JOB_NAME", "defaultJob");
    }

    @Test
    public void shouldGetAWSSecretAccessKey() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        String awsSecretAccessKey = fetchConfig.getAWSSecretAccessKey();
        assertThat(awsSecretAccessKey, is(secretKey));
    }

    @Test
    public void shouldGetAWSAccessKeyId() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        String awsSecretAccessKey = fetchConfig.getAWSAccessKeyId();
        assertThat(awsSecretAccessKey, is(accessId));
    }

    @Test
    public void shouldS3Bucket() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        String awsSecretAccessKey = fetchConfig.getS3Bucket();
        assertThat(awsSecretAccessKey, is(bucket));
    }

    @Test
    public void shouldGetArtifactLocation() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        String location = fetchConfig.getArtifactsLocationTemplate();
        assertThat(location, is("TestPublish/defaultStage/defaultJob/20.1"));
    }

    @Test
    public void shouldBeValid() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldNotBeValidIfAWSSecretAccessKeyNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.remove(AWS_SECRET_ACCESS_KEY).build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_SECRET_ACCESS_KEY environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfAWSAccessKeyIdNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.remove(AWS_ACCESS_KEY_ID).build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_ACCESS_KEY_ID environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfS3BucketNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.remove(GO_ARTIFACTS_S3_BUCKET).build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("GO_ARTIFACTS_S3_BUCKET environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfRepoConfigIsNotValid() {
        when(config.getValue(FetchTask.REPO)).thenReturn("Wrong");
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline.");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfPackageConfigIsNotValid() {
        when(config.getValue(FetchTask.PACKAGE)).thenReturn("Wrong");
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()));
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline.");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    private TaskExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap);
    }
}
