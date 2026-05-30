package com.fleetwise.api;

import java.util.*;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Testing {

    @FunctionalInterface
    public interface MyComparator<E>
    {
        int compare(E a,E b);
    }

    public static void main(String[] args) {

        var strings = List.of("one", "two", "three",  "four", "five", "six", "seven", "eight", "nine");

        MyComparator<String> stringMyComparator = (s1,s2) -> {
            var len1 = s1.length();
            var len2 = s2.length();
            return Integer.compare(len1, len2);
        };


    }
}
