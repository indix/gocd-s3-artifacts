package com.indix.gocd.s3material.plugin;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.GsonBuilder;
import com.indix.gocd.models.Artifact;
import com.indix.gocd.models.Revision;
import com.indix.gocd.models.RevisionStatus;
import com.indix.gocd.utils.MaterialResult;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Extension
public class S3PackageMaterialPoller implements GoPlugin {
    public static String S3_BUCKET = "S3_BUCKET";
    public static String PIPELINE_NAME = "PIPELINE_NAME";
    public static String STAGE_NAME = "STAGE_NAME";
    public static String JOB_NAME = "JOB_NAME";

    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";

    private static Logger logger = Logger.getLoggerFor(S3PackageMaterialPoller.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        if (goPluginApiRequest.requestName().equals(REQUEST_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PACKAGE_CONFIGURATION)) {
            return handlePackageConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_PACKAGE_CONFIGURATION)) {
            return handlePackageValidation();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_REPOSITORY_CONNECTION)) {
            return handleRepositoryCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_PACKAGE_CONNECTION)) {
            return handlePackageCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION)) {
            return handleGetLatestRevision(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION_SINCE)) {
            return handleLatestRevisionSince(goPluginApiRequest);
        }
        return null;
    }

    private GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);
        Map<String, Object> previousRevisionMap = getMapFor(goPluginApiRequest, "previous-revision");
        String previousRevision = (String) previousRevisionMap.get("revision");


        String s3Bucket = repositoryKeyValuePairs.get(S3_BUCKET);
        S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
        Artifact artifact = artifact(packageKeyValuePairs);
        try {
            RevisionStatus revision = artifactStore.getLatest(artifact);
            if(new Revision(revision.revision.getRevision()).compareTo(new Revision(previousRevision)) > 0) {
                return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, revision.toMap());
            }

            return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, null);
        } catch (Exception e) {
            logger.error("Error during handleLatestRevisionSince for "+artifact.toString()+", with msg: "+e.getMessage(), e);
            return createResponse(DefaultGoPluginApiResponse.INTERNAL_ERROR, null);
        }
    }

    private GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);
        String s3Bucket = repositoryKeyValuePairs.get(S3_BUCKET);
        S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
        Artifact artifact = artifact(packageKeyValuePairs);
        try {
            RevisionStatus revision = artifactStore.getLatest(artifact);
            return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, revision.toMap());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("Error during getLatestRevision for "+artifact.toString()+", with msg: "+e.getMessage(), e);
            return createResponse(DefaultGoPluginApiResponse.INTERNAL_ERROR, null);
        }
    }

    private GoPluginApiResponse handlePackageCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);
        MaterialResult result;
        String s3Bucket = repositoryKeyValuePairs.get(S3_BUCKET);

        if(StringUtils.isBlank(s3Bucket)) {
            result = new MaterialResult(false, "S3 bucket must be specified");
        } else {
            S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
            String prefix = artifact(packageKeyValuePairs).prefix();
            if(artifactStore.exists(s3Bucket, prefix)) {
                result = new MaterialResult(true, "Success");
            } else {
                result = new MaterialResult(false, String.format("Couldn't find artifact at [%s]", prefix));
            }
        }
        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleRepositoryCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        MaterialResult result;
        String s3Bucket = repositoryKeyValuePairs.get(S3_BUCKET);

        if(StringUtils.isBlank(s3Bucket)) {
           result = new MaterialResult(false, "S3 bucket must be specified");
        } else {
            S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
            if (artifactStore.bucketExists()) {
                result = new MaterialResult(true, "Success");
            } else {
                result = new MaterialResult(false, String.format("Couldn't find bucket [%s]", s3Bucket));
            }
        }
        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handlePackageValidation() {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handleRepositoryValidation(GoPluginApiRequest goPluginApiRequest) {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        final Map<String, String> repositoryKeyValuePairs = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);

        if(StringUtils.isBlank(repositoryKeyValuePairs.get(S3_BUCKET))) {
            HashMap<String, Object> errorMap = new HashMap<>();
            errorMap.put("key", S3_BUCKET);
            errorMap.put("message", "S3 bucket must be specified");
            validationResult.add(errorMap);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);

    }

    private GoPluginApiResponse handlePackageConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(PIPELINE_NAME,
                createField("Pipeline Name", null, true, true, false, "1")
        );
        response.put(STAGE_NAME,
                createField("Stage Name", null, true, true, false, "2")
        );
        response.put(JOB_NAME,
                createField("Job Name", null, true, true, false, "3")
        );
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleRepositoryConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(S3_BUCKET,
                createField("S3 Bucket", null, true, true, false, "1")
        );
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", Arrays.asList("1.0"));
    }

    private GoPluginApiResponse createResponse(int responseCode, Object body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    private Map<String, Object> createField(String displayName, String defaultValue, boolean isPartOfIdentity, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("part-of-identity", isPartOfIdentity);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

    private Map<String, Object> getMapFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldProperties = (Map<String, Object>) map.get(field);
        return fieldProperties;
    }

    private Map<String, String> keyValuePairs(GoPluginApiRequest goPluginApiRequest, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<>();
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldsMap = (Map<String, Object>) map.get(mainKey);
        for (String field : fieldsMap.keySet()) {
            Map<String, Object> fieldProperties = (Map<String, Object>) fieldsMap.get(field);
            String value = (String) fieldProperties.get("value");
            keyValuePairs.put(field, value);
        }
        return keyValuePairs;
    }

    public S3ArtifactStore s3ArtifactStore(String s3Bucket) {
        return new S3ArtifactStore(s3Bucket);
    }

    private Artifact artifact(Map<String, String> packageConfig) {
        String pipelineName = packageConfig.get(PIPELINE_NAME);
        String stageName = packageConfig.get(STAGE_NAME);
        String jobName = packageConfig.get(JOB_NAME);
        return new Artifact(pipelineName, stageName,jobName);
    }
}
