package com.indix.gocd.utils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.indix.gocd.utils.utils.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AWSCredentialsFactoryTest {

    private Map<String,String> environment;

    @Captor
    private ArgumentCaptor<ArrayList<AWSCredentialsProvider>> argumentCaptor;

    @Mock
    private AWSCredentialsProviderChain mockCredentialProviderChain;

    @Before
    public void TestSetup()
    {
        this.environment = new HashMap<String, String>();
    }

    @Test
    public void shouldThrowIfAWS_ACCESS_KEY_IDOrAWS_USE_IAM_ROLE_CREDENTIALS_NotPresent() {
        environment =  Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, "secretKey")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("AWS_ACCESS_KEY_ID environment variable not present"));
        }

    }

    @Test
    public void shouldThrowIfAWS_SECRET_ACCESS_KEYOrAWS_USE_IAM_ROLE_CREDENTIALS_NotPresent() {
        environment =  Maps.<String, String>builder()
                .with(AWS_ACCESS_KEY_ID, "secretKey")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("AWS_SECRET_ACCESS_KEY environment variable not present"));
        }
    }

    @Test
    public void shouldCreateEnvironmentVariableProviderWhenAWS_ACCESS_and_SECRET_KEY_ID_Provided() {
        environment =  Maps.<String, String>builder()
                .with(AWS_ACCESS_KEY_ID, "access Key")
                .with(AWS_SECRET_ACCESS_KEY, "secret Key")
                .build();
        AWSCredentialsFactory sut = spy(new AWSCredentialsFactory(environment));
        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(AccessKeyCredentialsProvider.class, providers.get(0).getClass());
    }

    @Test
    public void shouldCreateInstanceProfileProviderWhen_AWS_USE_IAM_ROLEIsTrue() {
        environment =  Maps.<String, String>builder()
                .with(AWS_USE_IAM_ROLE, "True")
                .build();
        AWSCredentialsFactory sut = spy(new AWSCredentialsFactory(environment));
        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(InstanceProfileCredentialsProvider.class, providers.get(0).getClass());
    }


    @Test
    public void shouldThrowIf_AWS_USE_IAM_ROLEIsProvidedButNotOneOfExpectedValues() {
        environment =  Maps.<String, String>builder()
                .with(AWS_USE_IAM_ROLE, "blah")
                .build();
        AWSCredentialsFactory sut = new AWSCredentialsFactory(environment);
        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(),
                    startsWith("Unexpected value in AWS_USE_IAM_ROLE environment variable; was blah, but expected"));
        }
    }


    @Test
    public void shouldCreateEnvironmentVariableProviderWhenAWS_ACCESS_and_SECRET_KEY_ID_ProvidedAnd_AWS_USE_IAM_ROLE_IsSetToNo() {
        environment =  Maps.<String, String>builder()
                .with(AWS_ACCESS_KEY_ID, "access Key")
                .with(AWS_SECRET_ACCESS_KEY, "secret Key")
                .with(AWS_USE_IAM_ROLE, "No")
                .build();
        AWSCredentialsFactory sut = spy(new AWSCredentialsFactory(environment));
        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(AccessKeyCredentialsProvider.class, providers.get(0).getClass());
    }
}
