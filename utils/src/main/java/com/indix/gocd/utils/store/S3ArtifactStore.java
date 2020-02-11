package com.indix.gocd.utils.store;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.indix.gocd.models.Artifact;
import com.indix.gocd.models.ResponseMetadataConstants;
import com.indix.gocd.models.Revision;
import com.indix.gocd.models.RevisionStatus;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Lists;
import com.indix.gocd.utils.utils.Maps;
import org.apache.commons.lang3.StringUtils;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class S3ArtifactStore {
    private static Logger logger = Logger.getLoggerFor(S3ArtifactStore.class);
    private static Map<String, StorageClass> STORAGE_CLASSES = Maps.<String, StorageClass>builder()
            .with(STORAGE_CLASS_STANDARD, StorageClass.Standard)
            .with(STORAGE_CLASS_STANDARD_IA, StorageClass.StandardInfrequentAccess)
            .with(STORAGE_CLASS_RRS, StorageClass.ReducedRedundancy)
            .with(STORAGE_CLASS_GLACIER, StorageClass.Glacier)
            .build();

    private AmazonS3 client;
    private String bucket;
    private StorageClass storageClass = StorageClass.Standard;

    public S3ArtifactStore(AmazonS3 client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public S3ArtifactStore(GoEnvironment env, String bucket) {
        this(getS3client(env), bucket);
    }

    public S3ArtifactStore(String bucket) {
        this(getS3client(new GoEnvironment()), bucket);
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
        put(new PutObjectRequest(bucket, to, new File(from)));
    }

    public void put(String from, String to, ObjectMetadata metadata) {
        put(new PutObjectRequest(bucket, to, new File(from))
                .withMetadata(metadata));
    }

    public void put(PutObjectRequest putObjectRequest) {
        putObjectRequest.setStorageClass(this.storageClass);
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
            client.listObjects(new ListObjectsRequest(bucket, null, null, null, 0));
            return true;
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

    private Boolean isComplete(String prefix) {
        return client.getObjectMetadata(bucket, prefix).getUserMetadata().containsKey(ResponseMetadataConstants.COMPLETED);
    }

    private Revision mostRecentRevision(ObjectListing listing) {
        List<String> prefixes = Lists.filter(listing.getCommonPrefixes(), new Functions.Predicate<String>() {
            @Override
            public Boolean execute(String input) {
                return isComplete(input);
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

    private Revision latestOfInternal(ObjectListing listing, Revision latestSoFar) {
        if (!listing.isTruncated()) {
            return latestSoFar;
        } else {
            ObjectListing objects = client.listNextBatchOfObjects(listing);
            Revision mostRecent = mostRecentRevision(objects);
            if (latestSoFar.compareTo(mostRecent) > 0)
                mostRecent = latestSoFar;
            return latestOfInternal(objects, mostRecent);
        }
    }

    private Revision latestOf(ObjectListing listing) {
        return latestOfInternal(listing, mostRecentRevision(listing));
    }

    public RevisionStatus getLatest(Artifact artifact) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(artifact.prefix())
                .withDelimiter("/");

        ObjectListing listing = client.listObjects(listObjectsRequest);
        if (listing != null) {
            Revision recent = latestOf(listing);
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
    }

    public String getLatestPrefix(String pipeline, String stage, String job, String pipelineCounter) {
        String prefix = String.format("%s/%s/%s/%s.", pipeline, stage, job, pipelineCounter);
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(prefix)
                .withDelimiter("/");

        ObjectListing listing = client.listObjects(listObjectsRequest);

        if (listing != null) {
            List<String> commonPrefixes = listing.getCommonPrefixes();
            List<String> stageCounters = Lists.map(commonPrefixes,
                    input ->
                            input.replaceAll(prefix, "").replaceAll("/", ""));
            if (stageCounters.size() > 0) {
                int maxStageCounter = Integer.valueOf(stageCounters.get(0));

                for (int i = 1; i < stageCounters.size(); i++) {
                    int stageCounter = Integer.valueOf(stageCounters.get(i));
                    if (stageCounter > maxStageCounter) {
                        maxStageCounter = stageCounter;
                    }
                }

                return prefix + maxStageCounter;
            }
        }
        return null;
    }

    public static AmazonS3 getS3client(GoEnvironment env) {
        AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder.standard();
        logger.debug("Instantiating S3 client with following env variables: ");
        logger.debug(env.toString());
        if (env.has(AWS_REGION)) {
            amazonS3ClientBuilder.withRegion(env.get(AWS_REGION));
        }
        if (env.hasAWSUseIamRole()) {
            logger.info("S3Artifact's getS3client uses AWS IAM Role");
            amazonS3ClientBuilder.withCredentials(new InstanceProfileCredentialsProvider(false));
        } else if (env.has(AWS_ACCESS_KEY_ID) && env.has(AWS_SECRET_ACCESS_KEY)) {
            logger.info("S3Artifact's getS3client uses AWS credentials from ENV");
            BasicAWSCredentials basicCreds = new BasicAWSCredentials(env.get(AWS_ACCESS_KEY_ID), env.get(AWS_SECRET_ACCESS_KEY));
            amazonS3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(basicCreds));
        } else {
            logger.warn("S3Artifact's getS3client fallback to default credentials chain");
        }

        return amazonS3ClientBuilder.build();
    }
}
