package com.example.ondevicetraining;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypeConverter {

    static String listToString(List<Integer> numberList) {
        return numberList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    static List<Integer> stringToList(String numbers) {
        return Arrays.stream(numbers.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
