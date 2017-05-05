package com.indix.gocd.utils;

import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialResult {
    private boolean success;
    private List<String> messages;

    public MaterialResult(boolean success) {
        this.success = success;
        messages = new ArrayList<>();
    }

    public MaterialResult(boolean success, String message) {
        this(success);
        messages.add(message);
    }

    public Map toMap() {
        final HashMap result = new HashMap();
        result.put("status", success ? "success" : "failure");
        result.put("messages", messages);
        return result;
    }

    public int responseCode() {
        return DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
    }
}
