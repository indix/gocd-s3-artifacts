package com.indix.gocd.s3publish.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Lists {

    public static <T, K> void foreach(List<T> originalList, Function<T, Void> function) {
        for (T item : originalList) {
            function.apply(item);
        }
    }

    public static <T, K> void foreach(T[] array, Function<T, Void> function) {
        foreach(Arrays.asList(array), function);
    }

    public static <T, K> List<K> flatMap(List<T> originalList, Function<T, List<K>> transformer) {
        ArrayList<K> flatMapped = new ArrayList<K>();
        for (T item : originalList) {
            flatMapped.addAll(transformer.apply(item));
        }
        return flatMapped;
    }

    public static <T, K> List<K> flatMap(T[] array, Function<T, List<K>> transformer) {
        return flatMap(Arrays.asList(array), transformer);
    }

    public static <T> List<T> of(T... items) {
        ArrayList<T> listToReturn = new ArrayList<T>();
        Collections.addAll(listToReturn, items);
        return Collections.unmodifiableList(listToReturn);
    }
}
