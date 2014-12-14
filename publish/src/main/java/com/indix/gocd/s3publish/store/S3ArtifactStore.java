package com.indix.gocd.s3publish.store;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;

public class S3ArtifactStore {
    private AmazonS3Client client;
    private String bucket;

    public S3ArtifactStore(AmazonS3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public void put(String from, String to, ObjectMetadata metadata) {
        client.putObject(new PutObjectRequest(bucket, to, new File(from))
                .withMetadata(metadata));
    }

}
