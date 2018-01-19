package com.indix.gocd.models;

import java.util.HashMap;
import java.util.Map;

public class BadRevisionStatus extends RevisionStatus {
    public Artifact artifact = null;
    public Boolean isValid = false;
    public String msg = null;
    public BadRevisionStatus(Artifact artifact, String msg) {
        super(artifact.revision, null, null, null, null);
        this.artifact = artifact;
        this.msg = msg;
        this.isValid = false;
    }

    @Override
    public String toString() {
        return "BadRevisionStatus{" +
                "artifact=" + artifact.prefixWithRevision() +
                ", msg=" + msg +
                '}';
    }

    @Override
    public Map toMap() {
        final HashMap result = new HashMap();
        result.put("artifact", artifact.prefixWithRevision());
        result.put("msg", this.msg);

        return result;
    }
}
