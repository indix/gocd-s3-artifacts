package com.indix.gocd.utils.utils;

public interface Function<I, O> {
    public O apply(I input);
}

