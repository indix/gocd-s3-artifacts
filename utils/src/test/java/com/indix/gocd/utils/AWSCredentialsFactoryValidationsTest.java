package com.indix.gocd.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.indix.gocd.utils.Constants.AWS_ACCESS_KEY_ID;
import static com.indix.gocd.utils.Constants.AWS_SECRET_ACCESS_KEY;
import static com.indix.gocd.utils.Constants.AWS_USE_INSTANCE_PROFILE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AWSCredentialsFactoryValidationsTest {
    @Mock
    private GoEnvironment goEnvironment;

    @Spy
    @InjectMocks
    private AWSCredentialsFactory sut = new AWSCredentialsFactory(goEnvironment);

    @Test
    public void shouldIncludeAWS_ACCESS_KEY_IDMissingValidationError() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);

        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));

    }

    @Test
    public void shouldIncludeAWS_SECRET_ACCESS_KEYMissingValidationError() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);

        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(0));

    }

    @Test
    public void shouldIncludeBothAWSKeyCredentialsMissingValidationError() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);

        List<String> validationErrors = sut.validationErrors();
        assertEquals(2, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(1));
    }


    @Test
    public void shouldIncludeBothAWSKeyCredentialsMissingValidationErrorWhenUseInstanceProfileIsFalse() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(false);
        when(goEnvironment.get(AWS_USE_INSTANCE_PROFILE)).thenReturn("False");

        List<String> validationErrors = sut.validationErrors();
        assertEquals(2, validationErrors.size());
        assertEquals("AWS_ACCESS_KEY_ID environment variable not present", validationErrors.get(0));
        assertEquals("AWS_SECRET_ACCESS_KEY environment variable not present", validationErrors.get(1));
    }
    @Test
    public void shouldIncludeAllAWSCredentialsMissingValidationError() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(false);
        when(goEnvironment.get(AWS_USE_INSTANCE_PROFILE)).thenReturn("some invalid value");

        List<String> validationErrors = sut.validationErrors();
        assertEquals(1, validationErrors.size());
        assertThat(validationErrors.get(0),
                startsWith("Unexpected value in AWS_USE_INSTANCE_PROFILE environment variable; was some invalid value, but expected"));
    }
}
