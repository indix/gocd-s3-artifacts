package com.indix.gocd.utils;

import com.indix.gocd.utils.utils.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.AWS_ACCESS_KEY_ID;
import static com.indix.gocd.utils.Constants.AWS_SECRET_ACCESS_KEY;
import static com.indix.gocd.utils.Constants.AWS_USE_INSTANCE_PROFILE;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AWSCredentialsFactoryValidationsTest {
    private Map<String,String> environment;

    @Test
    public void shouldIncludeAWS_ACCESS_KEY_IDMissingValidationError() {
        environment =  Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));

    }

    @Test
    public void shouldIncludeAWS_SECRET_ACCESS_KEYMissingValidationError() {
        environment =  Maps.<String, String>builder()
                .with(AWS_ACCESS_KEY_ID, "secretKey")
                .build();

        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(0));

    }

    @Test
    public void shouldIncludeBothAWSKeyCredentialsMissingValidationError() {
        environment =  Maps.<String, String>builder()
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        List<String> validationErrors = sut.validationErrors();
        assertEquals(2, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(1));
    }


    @Test
    public void shouldIncludeBothAWSKeyCredentialsMissingValidationErrorWhenUseInstanceProfileIsFalse() {
        environment =  Maps.<String, String>builder()
                .with(AWS_USE_INSTANCE_PROFILE, "False")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        List<String> validationErrors = sut.validationErrors();
        assertEquals(2, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(1));
    }
    @Test
    public void shouldIncludeAllAWSCredentialsMissingValidationError() {
        environment =  Maps.<String, String>builder()
                .with(AWS_USE_INSTANCE_PROFILE, "some invalid value")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertThat(validationErrors.get(0),
                startsWith("Unexpected value in AWS_USE_INSTANCE_PROFILE environment variable; was some invalid value, but expected"));
    }
}
