package com.indix.gocd.s3publish.utils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.indix.gocd.utils.utils.Maps;

import java.util.Map;

public class UserMetadataPrinter {

    public static void main(String[] args) {
        if(args.length < 2) {
            String usage = "UserMetadataPrinter [s3BucketName] [s3FileLocation]";
            System.out.println(usage);
            System.exit(1);
        }
        String s3Bucket = args[0];
        String s3FileLocation = args[1];
        S3ArtifactStore store = new S3ArtifactStore(new AmazonS3Client(), s3Bucket);
        ObjectMetadata metadata = store.getMetadata(s3FileLocation);
        Map<String, String> userMetadata = metadata.getUserMetadata();
        if (Maps.isEmpty(userMetadata)) {
            System.out.println("File has no user Metadata");
        } else {
            for (String key : userMetadata.keySet()) {
                System.out.println(key + " -> " + userMetadata.get(key));
            }
        }
    }
}
