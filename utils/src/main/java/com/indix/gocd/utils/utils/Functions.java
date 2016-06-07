package com.indix.gocd.utils.utils;

public class Functions {
    public static abstract class VoidFunction<I> implements Function<I, Void> {
        public abstract void execute(I input);

        public Void apply(I input) {
            execute(input);
            return null;
        }
    }

    public static abstract class Predicate<T> implements Function<T, Boolean> {
        public abstract Boolean execute(T input);
        public Boolean apply(T input){
            return execute(input);
        }
    }

}
