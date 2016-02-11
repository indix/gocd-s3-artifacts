package com.indix.gocd.models;

import java.util.Date;

public class RevisionStatus {
    public Revision revision;
    public Date lastModified;
    public String tracebackUrl;
    public String user;
    public String revisionLabel;

    public RevisionStatus(Revision revision, Date lastModified, String tracebackUrl, String user) {
        this(revision, lastModified, tracebackUrl, user, "");
    }
    public RevisionStatus(Revision revision, Date lastModified, String tracebackUrl, String user, String revisionLabel) {
        this.revision = revision;
        this.lastModified = lastModified;
        this.tracebackUrl = tracebackUrl;
        this.user = user;
        this.revisionLabel = revisionLabel;
    }
}
