package com.indix.gocd.utils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.ArrayList;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AWSCredentialsFactoryTest {

    @Mock
    private GoEnvironment goEnvironment;

    @Spy
    @InjectMocks
    private AWSCredentialsFactory sut = new AWSCredentialsFactory(goEnvironment);

    @Captor
    private ArgumentCaptor<ArrayList<AWSCredentialsProvider>> argumentCaptor;

    @Mock
    private AWSCredentialsProviderChain mockCredentialProviderChain;

    @Test
    public void shouldThrowIfAWS_ACCESS_KEY_IDOrAWS_USE_INSTANCE_PROFILE_CREDENTIALS_NotPresent() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);
        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("AWS_ACCESS_KEY_ID environment variable not present"));
        }

    }

    @Test
    public void shouldThrowIfAWS_SECRET_ACCESS_KEYOrAWS_USE_INSTANCE_PROFILE_CREDENTIALS_NotPresent() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);

        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("AWS_SECRET_ACCESS_KEY environment variable not present"));
        }
    }

    @Test
    public void shouldCreateEnvironmentVariableProviderWhenAWS_ACCESS_and_SECRET_KEY_ID_Provided() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(true);
        when(goEnvironment.get(AWS_ACCESS_KEY_ID)).thenReturn("access Key");
        when(goEnvironment.get(AWS_SECRET_ACCESS_KEY)).thenReturn("secret Key");

        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(AccessKeyCredentialsProvider.class, providers.get(0).getClass());
    }

    @Test
    public void shouldCreateInstanceProfileProviderWhen_AWS_USE_INSTANCE_PROFILEIsTrue() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(false);
        when(goEnvironment.get(AWS_USE_INSTANCE_PROFILE)).thenReturn("True");

        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(InstanceProfileCredentialsProvider.class, providers.get(0).getClass());
    }


    @Test
    public void shouldThrowIf_AWS_USE_INSTANCE_PROFILEIsProvidedButNotOneOfExpectedValues() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(true);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(false);
        when(goEnvironment.get(AWS_USE_INSTANCE_PROFILE)).thenReturn("blah");

        try {
            sut.getCredentialsProvider();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(),
                    startsWith("Unexpected value in AWS_USE_INSTANCE_PROFILE environment variable; was blah, but expected"));
        }
    }


    @Test
    public void shouldCreateEnvironmentVariableProviderWhenAWS_ACCESS_and_SECRET_KEY_ID_ProvidedAnd_AWS_USE_INSTANCE_PROFILE_IsSetToNo() {
        when(goEnvironment.isAbsent(AWS_ACCESS_KEY_ID)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_SECRET_ACCESS_KEY)).thenReturn(false);
        when(goEnvironment.isAbsent(AWS_USE_INSTANCE_PROFILE)).thenReturn(false);
        when(goEnvironment.get(AWS_USE_INSTANCE_PROFILE)).thenReturn("No");
        when(goEnvironment.get(AWS_ACCESS_KEY_ID)).thenReturn("access Key");
        when(goEnvironment.get(AWS_SECRET_ACCESS_KEY)).thenReturn("secret Key");

        AWSCredentialsProviderChain result = (AWSCredentialsProviderChain)sut.getCredentialsProvider();
        assertNotNull(result);
        verify(sut, times(1)).makeProvidersChain(argumentCaptor.capture());
        List<AWSCredentialsProvider> providers = argumentCaptor.getValue();
        assertEquals(1, providers.size());
        assertEquals(AccessKeyCredentialsProvider.class, providers.get(0).getClass());
    }
}
