package com.indix.gocd.s3fetch;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class Config {

    private final String materialType;
    private final String packageRepo;
    private final String packageName;
    private final String pipelineMaterial;
    private final String pipelineJob;
    private final String stagePipeline;
    private final String stageName;
    private final String stageJob;
    private final String stageSource;
    private final String stageSourcePrefix;
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

    public String getStagePipeline() {
      return stagePipeline;
    }

    public String getStageName() {
      return stageName;
    }

    public String getStageJob() {
      return stageJob;
    }

    public String getStageSource() {
      return stageSource;
    }

    public String getStageSourcePrefix() {
      return stageSourcePrefix;
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
        stagePipeline = getValue(config, STAGE_PIPELINE);
        stageName = getValue(config, STAGE_NAME);
        stageJob = getValue(config, STAGE_JOB);
        stageSource = getValue(config, STAGE_SOURCE);
        stageSourcePrefix = getValue(config, STAGE_SOURCE_PREFIX);
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
