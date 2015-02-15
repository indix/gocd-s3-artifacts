package com.indix.gocd.models;

public class Artifact {
    String pipelineName;
    String stageName;
    String jobName;
    Revision revision;

    public Artifact(String pipelineName, String stageName, String jobName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
    }

    public Artifact(String pipelineName, String stageName, String jobName, Revision revision) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
        this.revision = revision;
    }

    public Artifact withRevision(Revision revision) {
        this.revision = revision;
        return this;
    }

    public String prefix(){
        return String.format("%s/%s/%s", pipelineName, stageName, jobName);
    }

    public String prefixWithRevision(){
        if(revision != null)
            return String.format("%s/%s/%s/%s", pipelineName, stageName, jobName, revision.getRevision());
        else
            return prefix();
    }
}
