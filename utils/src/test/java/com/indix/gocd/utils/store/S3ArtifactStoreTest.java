package com.indix.gocd.utils.store;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class S3ArtifactStoreTest {
    AmazonS3Client mockClient = mock(AmazonS3Client.class);
    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

    @Test
    public void shouldUseStandardStorageClassAsDefault() {
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");
        store.put(new PutObjectRequest("foo-bar", "key", new File("/tmp/baz")));
        verify(mockClient, times(1)).putObject(putCaptor.capture());
        PutObjectRequest putRequest = putCaptor.getValue();
        assertThat(putRequest.getStorageClass(), is("STANDARD"));
    }

    @Test
    public void shouldUseStandardIAStorageClassAsDefault() {
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");
        store.setStorageClass("standard-ia");
        store.put(new PutObjectRequest("foo-bar", "key", new File("/tmp/baz")));
        verify(mockClient, times(1)).putObject(putCaptor.capture());
        PutObjectRequest putRequest = putCaptor.getValue();
        assertThat(putRequest.getStorageClass(), is("STANDARD_IA"));
    }

    @Test
    public void shouldUseReducedRedundancyStorageClass() {
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");
        store.setStorageClass("rrs");
        store.put(new PutObjectRequest("foo-bar", "key", new File("/tmp/baz")));
        verify(mockClient, times(1)).putObject(putCaptor.capture());
        PutObjectRequest putRequest = putCaptor.getValue();
        assertThat(putRequest.getStorageClass(), is("REDUCED_REDUNDANCY"));
    }

    @Test
    public void shouldUseGlacierStorageClass() {
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");
        store.setStorageClass("glacier");
        store.put(new PutObjectRequest("foo-bar", "key", new File("/tmp/baz")));
        verify(mockClient, times(1)).putObject(putCaptor.capture());
        PutObjectRequest putRequest = putCaptor.getValue();
        assertThat(putRequest.getStorageClass(), is("GLACIER"));
    }

}