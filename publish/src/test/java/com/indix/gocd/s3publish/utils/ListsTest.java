package com.indix.gocd.s3publish.utils;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static com.indix.gocd.s3publish.utils.Lists.*;

public class ListsTest {
    @Test
    public void shouldRunForEachOnceForEveryElement() {
        final int[] sum = {0};
        List<Integer> list = Lists.of(1, 2, 3, 4, 5);
        foreach(list, new Function<Integer, Void>() {
            @Override
            public Void apply(Integer input) {
                sum[0] += (input * 2);
                return null;
            }
        });

        assertThat(sum[0], is(30));
    }

    @Test
    public void shouldTransformEveryElementInTheListAndFlatten() {
        List<Integer> duplicateNumbers = flatMap(Lists.of(1, 2, 3, 4, 5), new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer input) {
                return Lists.of(input, input * 2);
            }
        });

        assertThat(duplicateNumbers, is(Lists.of(1, 2, 2, 4, 3, 6, 4, 8, 5, 10)));
    }
}