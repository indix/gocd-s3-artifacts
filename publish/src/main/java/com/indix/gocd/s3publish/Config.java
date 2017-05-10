package com.indix.gocd.s3publish;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.indix.gocd.utils.utils.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.DESTINATION_PREFIX;
import static com.indix.gocd.utils.Constants.SOURCEDESTINATIONS;
import static org.apache.commons.lang3.StringUtils.trim;

public class Config {

    public String sourceDestinationsJson;
    public String destinationPrefix;


    public Config(Map config) {
        sourceDestinationsJson  = getValue(config, SOURCEDESTINATIONS);
        destinationPrefix  = getValue(config, DESTINATION_PREFIX);
    }

    public List<Tuple2<String, String>> sourceDestinations() throws JSONException {
        JSONArray sourceDestinations = new JSONArray(sourceDestinationsJson);
        List<Tuple2<String, String>> result = new ArrayList<>();
        for(int i =0; i < sourceDestinations.length(); i++) {
            JSONObject sourceDestination = (JSONObject)sourceDestinations.get(i);
            String source = trim(sourceDestination.getString("source"));
            String destination = trim(sourceDestination.getString("destination"));
            result.add(new Tuple2<>(source, destination));
        }

        return result;
    }

    private String getValue(Map config, String property) {
        return (String) ((Map) config.get(property)).get("value");
    }
}
