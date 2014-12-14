package com.indix.gocd.s3publish.utils;

public interface Function<I, O> {
    public O apply(I input);
}
