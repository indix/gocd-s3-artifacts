package com.indix.gocd.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.StringUtils;

/*
 * Using this provider instead of built-in EnvironmentVariableCredentialsProvider since the latter will use either of the
 * environment variables AWS_ACCESS_KEY_ID (or AWS_ACCESS_KEY) and AWS_SECRET_KEY (or AWS_SECRET_ACCESS_KEY), thus
 * potentially breaking backward compatibility with prior versions of the GOCD S3 plugins which explicitly only work
 * with AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY variables.
 */
public class AccessKeyCredentialsProvider implements AWSCredentialsProvider {
    private String accessKey;
    private String secretKey;

    public AccessKeyCredentialsProvider(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public AWSCredentials getCredentials() {
        if(!StringUtils.isNullOrEmpty(accessKey) && !StringUtils.isNullOrEmpty(secretKey)) {
            return new BasicAWSCredentials(accessKey, secretKey);
        } else {
            throw new AmazonClientException("Unable to load AWS credentials from initialized properties (accessKeyId and secretKey)");
        }
    }

    public void refresh() {
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }
}