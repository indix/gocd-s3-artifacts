package com.indix.gocd.s3publish.utils;

public class SourceDestination {
    private String source;
    private String destination;

    public SourceDestination(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }
}
