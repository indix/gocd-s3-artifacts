package com.indix.gocd.s3fetch;

import java.util.Map;

import static com.indix.gocd.utils.Constants.DESTINATION;
import static com.indix.gocd.utils.Constants.PACKAGE;
import static com.indix.gocd.utils.Constants.REPO;

public class Config {

    public String repo;
    public String pkg;
    public String destination;

    public String getRepo() {
        return escapeEnvironmentVariable(repo);
    }

    public String getPkg() {
        return escapeEnvironmentVariable(pkg);
    }

    public String getDestination() {
        return destination;
    }

    public Config(Map config) {
        repo = getValue(config, REPO);
        pkg = getValue(config, PACKAGE);
        destination = getValue(config, DESTINATION);
    }

    private String escapeEnvironmentVariable(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
    }

    private String getValue(Map config, String property) {
        return (String) ((Map) config.get(property)).get("value");
    }
}
