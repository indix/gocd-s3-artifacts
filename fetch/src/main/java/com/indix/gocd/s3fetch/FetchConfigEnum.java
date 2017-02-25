package com.indix.gocd.s3fetch;

enum FetchConfigEnum {
    REPO("Repo"),
    PACKAGE("Package"),
    DESTINATION("Destination");

    private final String propertyValue;
    FetchConfigEnum(String sth) {
        propertyValue = sth;
    }

}
