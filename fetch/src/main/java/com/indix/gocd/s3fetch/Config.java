package com.indix.gocd.s3fetch;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class Config {

    private final String materialType;
    private final String packageRepo;
    private final String packageName;
    private final String pipelineMaterial;
    private final String pipelineJob;
    private final String destination;

    public String getMaterialType() {
      return materialType;
    }

    public String getPackageRepo() {
        return escapeEnvironmentVariable(packageRepo);
    }

    public String getPackageName() {
        return escapeEnvironmentVariable(packageName);
    }

    public String getPipelineMaterial() {
      return escapeEnvironmentVariable(pipelineMaterial);
    }

    public String getPipelineJob() {
      return pipelineJob;
    }


    public String getDestination() {
        return destination;
    }

    public Config(Map config) {
        materialType = getValue(config, MATERIAL_TYPE);
        packageRepo = getValue(config, PACKAGE_REPO);
        packageName = getValue(config, PACKAGE_NAME);
        pipelineMaterial = getValue(config, PIPELINE_MATERIAL);
        pipelineJob = getValue(config, PIPELINE_JOB);
        destination = getValue(config, DESTINATION);
    }

    private String escapeEnvironmentVariable(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
    }

    private String getValue(Map config, String property) {
        Map propertyMap = ((Map) config.get(property));
        if (propertyMap != null) {
            return (String) propertyMap.get("value");
        }
        return null;
    }
}
