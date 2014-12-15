package com.indix.gocd.s3publish.utils;

public class Functions {
    public static abstract class VoidFunction<I> implements Function<I, Void> {
        public abstract void execute(I input);

        public Void apply(I input) {
            execute(input);
            return null;
        }
    }
}
