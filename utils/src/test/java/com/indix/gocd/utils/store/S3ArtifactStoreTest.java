package com.indix.gocd.utils.store;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.indix.gocd.utils.GoEnvironment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.AWS_ACCESS_KEY_ID;
import static com.indix.gocd.utils.Constants.AWS_REGION;
import static com.indix.gocd.utils.Constants.AWS_SECRET_ACCESS_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class S3ArtifactStoreTest {

    AmazonS3Client mockClient = mock(AmazonS3Client.class);
    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<ListObjectsRequest> listingCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);

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

    @Test
    public void verifyObjectListingRequestIsRight() {
        doReturn(null).when(mockClient).listObjects(any(ListObjectsRequest.class));
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");
        store.getLatestPrefix("pipeline", "stage", "job", "1");

        verify(mockClient).listObjects(listingCaptor.capture());
        ListObjectsRequest request = listingCaptor.getValue();
        assertEquals("foo-bar", request.getBucketName());
        assertEquals("pipeline/stage/job/1.", request.getPrefix());
        assertEquals("/", request.getDelimiter());
    }

    @Test
    public void shouldReturnNullWhenObjectListingIsNull() {
        doReturn(null).when(mockClient).listObjects(any(ListObjectsRequest.class));
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");

        String prefix = store.getLatestPrefix("pipeline", "stage", "job", "1");
        assertNull(prefix);
    }

    @Test
    public void shouldReturnNullWhenObjectListingIsSize0() {
        ObjectListing listing = new ObjectListing();
        doReturn(listing).when(mockClient).listObjects(any(ListObjectsRequest.class));
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");

        String prefix = store.getLatestPrefix("pipeline", "stage", "job", "1");
        assertNull(prefix);
    }

    @Test
    public void shouldReturnTheLatestStageCounter() {
        ObjectListing listing = new ObjectListing();
        List<String> commonPrefixes = new ArrayList<>();
        commonPrefixes.add("pipeline/stage/job/1.2");
        commonPrefixes.add("pipeline/stage/job/1.1");
        commonPrefixes.add("pipeline/stage/job/1.7");
        listing.setCommonPrefixes(commonPrefixes);
        
        doReturn(listing).when(mockClient).listObjects(any(ListObjectsRequest.class));
        S3ArtifactStore store = new S3ArtifactStore(mockClient, "foo-bar");

        String prefix = store.getLatestPrefix("pipeline", "stage", "job", "1");
        assertEquals("pipeline/stage/job/1.7", prefix);
    }
}
