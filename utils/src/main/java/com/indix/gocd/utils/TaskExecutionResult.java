package com.indix.gocd.utils;

import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;

import java.util.HashMap;
import java.util.Map;

public class TaskExecutionResult {
    private boolean success;
    private String message;
    private Exception exception;

    public TaskExecutionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public TaskExecutionResult(boolean success, String message, Exception exception) {
        this(success, message);
        this.exception = exception;
    }

    public Map toMap() {
        final HashMap result = new HashMap();
        result.put("success", success);
        result.put("message", message);
        return result;
    }

    public int responseCode() {
        return DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
    }

    public boolean isSuccessful() {
        return success;
    }

    public String message() {
        return message;
    }
}
