package com.indix.gocd.utils;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Map;
import com.indix.gocd.utils.utils.Maps;
import static com.indix.gocd.utils.Constants.GO_SERVER_DASHBOARD_URL;
import static org.junit.Assert.assertTrue;


public class GoEnvironmentTest {
    Map<String, String> mockEnvironment = Maps.<String, String>builder()
            .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
            .with("GO_SERVER_URL", "https://localhost:8154/go")
            .with("GO_PIPELINE_NAME", "s3-publish-test")
            .with("GO_PIPELINE_COUNTER", "20")
            .with("GO_STAGE_NAME", "build-and-publish")
            .with("GO_STAGE_COUNTER", "1")
            .with("GO_JOB_NAME", "publish")
            .with("GO_TRIGGER_USER", "Krishna")
            .build();
    GoEnvironment goEnvironment = new GoEnvironment().putAll(mockEnvironment);

    @Test
    public void shouldGenerateTracebackUrl() {
        assertThat(goEnvironment.traceBackUrl(), is("http://go.server:8153/go/tab/build/detail/s3-publish-test/20/build-and-publish/1/publish"));
    }

    @Test
    public void shouldReturnTriggeredUser() {
        assertThat(goEnvironment.triggeredUser(), is("Krishna"));
    }

    @Test
    public void shouldGenerateArtifactLocationTemplate() {
        assertThat(goEnvironment.artifactsLocationTemplate(), is("s3-publish-test/build-and-publish/publish/20.1"));
    }

    @Test
    public void shouldReturnAsMap() {
        for(Map.Entry<String, String> entry : mockEnvironment.entrySet()) {
            assertEquals(entry.getValue(), goEnvironment.asMap().get(entry.getKey()));
        }
    }

    @Test
    public void shouldReplaceWithEnvVariables() {
        final String envTestTemplate = "COUNT:${GO_STAGE_COUNTER} Name:${GO_STAGE_NAME} COUNT2:${GO_STAGE_COUNTER}";
        final String replaced = goEnvironment.replaceVariables(envTestTemplate);

        assertThat(replaced, is("COUNT:1 Name:build-and-publish COUNT2:1"));
    }

    @Test
    public void shouldNotReplaceUnknownEnvVariables() {
        final String envTestTemplate = "COUNT:${GO_STAGE_COUNTER} ${DOESNT_EXIST}";
        final String replaced = goEnvironment.replaceVariables(envTestTemplate);

        assertThat(replaced, is("COUNT:1 ${DOESNT_EXIST}"));
    }

}
