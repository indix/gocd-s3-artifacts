package com.indix.gocd.models;

import com.indix.gocd.models.Revision;

import java.util.Date;

public class RevisionStatus {
    public Revision revision;
    public Date lastModified;
    public String tracebackUrl;
    public String user;

    public RevisionStatus(Revision revision, Date lastModified, String tracebackUrl, String user) {
        this.revision = revision;
        this.lastModified = lastModified;
        this.tracebackUrl = tracebackUrl;
        this.user = user;
    }
}
