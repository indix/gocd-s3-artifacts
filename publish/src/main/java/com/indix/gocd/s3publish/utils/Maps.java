package com.indix.gocd.s3publish.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Maps {
    public static <K, V> MapBuilder<K, V> builder() {
        return new MapBuilder<K, V>();
    }

    public static class MapBuilder<K, V> {
        private Map<K, V> internal = new HashMap<K, V>();

        public MapBuilder<K, V> with(K key, V value) {
            internal.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return Collections.unmodifiableMap(internal);
        }
    }
}
