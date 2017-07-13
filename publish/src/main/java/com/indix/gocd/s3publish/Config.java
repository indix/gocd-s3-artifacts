package com.indix.gocd.s3publish;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.ARTIFACTS_BUCKET;
import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;

public class Config {

    public String sourceDestinationsJson;
    public String destinationPrefix;
    public String artifactsBucket;

    public Config(Map config) {
        sourceDestinationsJson  = getValue(config, SOURCEDESTINATIONS);
        destinationPrefix  = getValue(config, DESTINATION_PREFIX);
        artifactsBucket  = getValue(config, ARTIFACTS_BUCKET);
    }

    public List<SourceDestination> sourceDestinations() throws JsonSyntaxException {
        Type type = new TypeToken<ArrayList<SourceDestination>>() {}.getType();
        return new GsonBuilder().create().fromJson(sourceDestinationsJson, type);
    }

    private String getValue(Map config, String property) {
        return (String) ((Map) config.get(property)).get("value");
    }
}
