package com.indix.gocd.utils.utils;

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

    public static <T,K> List<K> map(T[] array, Function<T, K> transformer) {
        return map(Arrays.asList(array), transformer);
    }

    public static <T,K> List<K> map(List<T> elements, Function<T, K> transformer) {
        List<K> mapped = new ArrayList<K>();
        for (T item: elements) {
            mapped.add(transformer.apply(item));
        }
        return mapped;
    }

    public static <T> List<T> filter(List<T> elements, Functions.Predicate<T> predicate) {
        List<T> filtered = new ArrayList<T>();
        for(T element: elements){
            if(predicate.execute(element))
                filtered.add(element);
        }
        return filtered;
    }

    public static <T> Boolean exists(List<T> elements, Functions.Predicate<T> predicate) {
        for (T element: elements) {
            if(predicate.execute(element))
                return true;
        }
        return false;
    }
}
