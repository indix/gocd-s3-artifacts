package com.indix.gocd.utils.store;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.File;

public class S3ArtifactStore {
    private AmazonS3Client client;
    private String bucket;

    public S3ArtifactStore(AmazonS3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public void put(String from, String to) {
        put(new PutObjectRequest(bucket, to, new File(from)));
    }

    public void put(String from, String to, ObjectMetadata metadata) {
        put(new PutObjectRequest(bucket, to, new File(from))
                .withMetadata(metadata));
    }

    public void put(PutObjectRequest putObjectRequest) {
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
                if(size > 0) {
                    get(objectSummary.getKey(), destinationPath);
                }
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

    }

}
