package com.indix.gocd.utils;

import static com.indix.gocd.utils.Constants.AWS_USE_IAM_ROLE;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import com.indix.gocd.utils.utils.Maps;
import static com.indix.gocd.utils.Constants.GO_SERVER_DASHBOARD_URL;
import static org.junit.Assert.fail;


public class GoEnvironmentTest {
    private GoEnvironment goEnvironment;
    Maps.MapBuilder<String, String> mockEnvironmentVariables;

    @Before
    public void setUp() throws Exception {
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(GO_SERVER_DASHBOARD_URL, "http://go.server:8153")
                .with("GO_SERVER_URL", "https://localhost:8154/go")
                .with("GO_PIPELINE_NAME", "s3-publish-test")
                .with("GO_PIPELINE_COUNTER", "20")
                .with("GO_STAGE_NAME", "build-and-publish")
                .with("GO_STAGE_COUNTER", "1")
                .with("GO_JOB_NAME", "publish")
                .with("GO_TRIGGER_USER", "Krishna");
        goEnvironment = new GoEnvironment(new HashMap<String, String>()).putAll(mockEnvironmentVariables.build());
    }

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
        for(Map.Entry<String, String> entry : mockEnvironmentVariables.build().entrySet()) {
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

    @Test
    public void shouldGetHasAWSUseIamRoleTrueIfSetToTrue() {
        GoEnvironment sut = new GoEnvironment(mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"True")
                .build());

        Boolean result = sut.hasAWSUseIamRole();

        assertThat(result, Matchers.is(Boolean.TRUE));
    }

    @Test
    public void shouldGetHasAWSUseIamRoleFalseIfSetToFalse() {
        GoEnvironment sut = new GoEnvironment(mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"False")
                .build());

        Boolean result = sut.hasAWSUseIamRole();

        assertThat(result, Matchers.is(Boolean.FALSE));
    }


    @Test
    public void shouldThrowExceptionIfAWSUseIamRoleNotWithinExpectedValues() {
        GoEnvironment sut = new GoEnvironment(mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"blue")
                .build());

        try {
            sut.hasAWSUseIamRole();
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "Unexpected value in AWS_USE_IAM_ROLE environment variable; was blue, " +
                            "but expected one of the following [true, false, yes, no, on, off]",
                    e.getMessage());
        }

    }

}
