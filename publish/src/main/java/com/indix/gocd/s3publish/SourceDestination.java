package com.indix.gocd.s3publish;

public class SourceDestination {
    public String source;
    public String destination;

    public SourceDestination() {
    }

    public SourceDestination(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }
}
