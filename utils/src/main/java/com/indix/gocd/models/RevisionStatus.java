package com.indix.gocd.models;

import com.amazonaws.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RevisionStatus {
    public Revision revision;
    public Date lastModified;
    public String tracebackUrl;
    public String user;
    public String revisionLabel;
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

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

    public Map toMap() {
        final HashMap result = new HashMap();
        result.put("revision", revision.getRevision());
        result.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(lastModified));
        result.put("user", user);
        result.put("revisionComment", String.format("Original revision number: %s",
                StringUtils.isNullOrEmpty(revisionLabel) ? "unavailable" : revisionLabel));
        result.put("trackbackUrl", tracebackUrl);

        return result;
    }
}
