package com.indix.gocd.s3fetch;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class Config {

    private final String materialType;
    private final String repo;
    private final String pkg;
    private final String material;
    private final String job;
    private final String stage;
    private final String source;
    private final String sourcePrefix;
    private final String destination;

    public String getMaterialType() {
      return materialType;
    }

    public String getRepo() {
        return escapeEnvironmentVariable(repo);
    }

    public String getPkg() {
        return escapeEnvironmentVariable(pkg);
    }

    public String getMaterial() {
      return escapeEnvironmentVariable(material);
    }

    public String getJob() {
      return job;
    }

    public String getStage() {
      return stage;
    }

    public String getSource() {
      return source;
    }

    public String getSourcePrefix() {
      return sourcePrefix;
    }

    public String getDestination() {
        return destination;
    }

    public Config(Map config) {
        materialType = getValue(config, MATERIAL_TYPE);
        repo = getValue(config, REPO);
        pkg = getValue(config, PACKAGE);
        material = getValue(config, MATERIAL);
        job = getValue(config, JOB);
        stage = getValue(config, STAGE);
        source = getValue(config, SOURCE);
        sourcePrefix = getValue(config, SOURCE_PREFIX);
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
