package com.indix.gocd.utils.store;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.indix.gocd.models.Artifact;
import com.indix.gocd.models.ResponseMetadataConstants;
import com.indix.gocd.models.Revision;
import com.indix.gocd.models.RevisionStatus;
import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Lists;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.indix.gocd.utils.utils.Maps;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class S3ArtifactStore {

    private static Map<String, StorageClass> STORAGE_CLASSES = Maps.<String, StorageClass>builder()
            .with(STORAGE_CLASS_STANDARD, StorageClass.Standard)
            .with(STORAGE_CLASS_STANDARD_IA, StorageClass.StandardInfrequentAccess)
            .with(STORAGE_CLASS_RRS, StorageClass.ReducedRedundancy)
            .with(STORAGE_CLASS_GLACIER, StorageClass.Glacier)
            .build();

    private AmazonS3Client client;
    private String bucket;
    private String kmsKey = null;
    private static Logger logger = Logger.getLoggerFor(S3ArtifactStore.class);
    private StorageClass storageClass = StorageClass.Standard;

    public S3ArtifactStore(AmazonS3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public S3ArtifactStore(AmazonS3Client client, String bucket, String kmsKey) {
        this.client = client;
        this.bucket = bucket;
        this.kmsKey = kmsKey;
    }

    public void setStorageClass(String storageClass) {
        String key = StringUtils.lowerCase(storageClass);
        if (STORAGE_CLASSES.containsKey(key)) {
            this.storageClass = STORAGE_CLASSES.get(key);
        } else {
            throw new IllegalArgumentException("Invalid storage class specified for S3 - " + storageClass + ". Accepted values are standard, standard-ia, rrs and glacier");
        }
    }

    public void put(String from, String to) {
        put(from, to, null, kmsKey);
    }

    public void put(String from, String to, ObjectMetadata metadata, String kmsKey) {
        PutObjectRequest request = new PutObjectRequest(bucket, to, new File(from));
        if (metadata != null)
            request = request.withMetadata(metadata);
        if (kmsKey != null && !kmsKey.isEmpty())
            request = request.withSSEAwsKeyManagementParams(
                    new SSEAwsKeyManagementParams(kmsKey)
            );

        put(request);
    }

    public void put(PutObjectRequest putObjectRequest) {
        putObjectRequest.setStorageClass(this.storageClass);
        putObjectRequest.setCannedAcl(CannedAccessControlList.BucketOwnerFullControl);
        client.putObject(putObjectRequest);
    }

    public String pathString(String pathOnS3) {
        return String.format("s3://%s/%s", bucket, pathOnS3);
    }

    public void get(String from, String to) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, from);
        File destinationFile = new File(to);
        destinationFile.getParentFile().mkdirs();
        client.getObject(getObjectRequest, destinationFile);
    }

    public ObjectMetadata getMetadata(String key) {
        return client.getObjectMetadata(bucket, key);
    }

    public void getPrefix(String prefix, String to) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(prefix);

        ObjectListing objectListing;
        do {
            objectListing = client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                String destinationPath = to + "/" + objectSummary.getKey().replace(prefix + "/", "");
                long size = objectSummary.getSize();
                if (size > 0) {
                    get(objectSummary.getKey(), destinationPath);
                }
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

    }

    public boolean bucketExists() {
        try {
            List<Bucket> buckets = client.listBuckets();
            return Lists.exists(buckets, new Functions.Predicate<Bucket>() {
                @Override
                public Boolean execute(Bucket input) {
                    return input.getName().equals(bucket);
                }
            });
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean exists(String bucket, String key) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(key)
                .withDelimiter("/");
        try {
            ObjectListing objectListing = client.listObjects(listObjectsRequest);
            return objectListing != null && objectListing.getCommonPrefixes().size() > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private Boolean isComplete(AmazonS3Client client, String prefix) {
        return client.getObjectMetadata(bucket, prefix).getUserMetadata().containsKey(ResponseMetadataConstants.COMPLETED);
    }

    private Revision mostRecentRevision(final AmazonS3Client client, ObjectListing listing) {
        List<String> prefixes = Lists.filter(listing.getCommonPrefixes(), new Functions.Predicate<String>() {
            @Override
            public Boolean execute(String input) {
                return isComplete(client, input);
            }
        });

        List<Revision> revisions = Lists.map(prefixes, new Function<String, Revision>() {
            @Override
            public Revision apply(String prefix) {
                String[] parts = prefix.split("/");
                String last = parts[parts.length - 1];
                return new Revision(last);
            }
        });

        if (revisions.size() > 0)
            return Collections.max(revisions);
        else
            return Revision.base();
    }

    private Revision latestOfInternal(AmazonS3Client client, ObjectListing listing, Revision latestSoFar) {
        if (!listing.isTruncated()) {
            return latestSoFar;
        } else {
            ObjectListing objects = client.listNextBatchOfObjects(listing);
            Revision mostRecent = mostRecentRevision(client, objects);
            if (latestSoFar.compareTo(mostRecent) > 0)
                mostRecent = latestSoFar;
            return latestOfInternal(client, objects, mostRecent);
        }
    }

    private Revision latestOf(AmazonS3Client client, ObjectListing listing) {
        return latestOfInternal(client, listing, mostRecentRevision(client, listing));
    }

    public RevisionStatus getLatest(AmazonS3Client client, Artifact artifact) {
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(bucket)
                    .withPrefix(artifact.prefix())
                    .withDelimiter("/");

            ObjectListing listing = client.listObjects(listObjectsRequest);
            if (listing != null) {
                Revision recent = latestOf(client, listing);
                Artifact artifactWithRevision = artifact.withRevision(recent);
                GetObjectMetadataRequest objectMetadataRequest = new GetObjectMetadataRequest(bucket, artifactWithRevision.prefixWithRevision());
                ObjectMetadata metadata = client.getObjectMetadata(objectMetadataRequest);
                Map<String, String> userMetadata = metadata.getUserMetadata();
                String tracebackUrl = userMetadata.get(ResponseMetadataConstants.TRACEBACK_URL);
                String user = userMetadata.get(ResponseMetadataConstants.USER);
                String revisionLabel = userMetadata.containsKey(ResponseMetadataConstants.GO_PIPELINE_LABEL) ?
                        userMetadata.get(ResponseMetadataConstants.GO_PIPELINE_LABEL)
                        : "";
                return new RevisionStatus(recent, metadata.getLastModified(), tracebackUrl, user, revisionLabel);
            }
            return null;
        } catch (RuntimeException e) {
            logger.error(String.format("Error in getLatest from S3 repository bucket {%s}, prefix {%s}", bucket, artifact.prefix()), e);
            throw e;
        }
    }
}
