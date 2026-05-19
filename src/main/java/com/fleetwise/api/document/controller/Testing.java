package com.fleetwise.api.document.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Testing {

    public static void main(String[] args) {

        String s = "aaaabaaaa";
        List<Map.Entry<Character, Long>> list = s.chars().mapToObj(i -> (char) i)
                .collect(
                        Collectors.groupingBy(
                                Function.identity(),
                                LinkedHashMap::new,
                                Collectors.counting()
                        )
                )
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() == 1)
                .toList();

        list.stream().map(Map.Entry::getKey).forEach(System.out::println);
    }
}
