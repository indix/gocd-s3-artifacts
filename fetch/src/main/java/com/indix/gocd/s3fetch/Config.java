package com.indix.gocd.s3fetch;

import java.util.Map;

import static com.indix.gocd.utils.Constants.*;

public class Config {

    public String materialType;
    public String repo;
    public String pkg;
    public String material;
    public String job;
    public String destination;

    public String getMaterialType() { return materialType; }

    public String getRepo() {
        return escapeEnvironmentVariable(repo);
    }

    public String getPkg() {
        return escapeEnvironmentVariable(pkg);
    }

    public String getMaterial() { return material; }

    public String getJob() { return job; }

    public String getDestination() {
        return destination;
    }

    public Config(Map config) {
        materialType = getValue(config, MATERIAL_TYPE);
        repo = getValue(config, REPO);
        pkg = getValue(config, PACKAGE);
        material = getValue(config, MATERIAL);
        job = getValue(config, JOB);
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
