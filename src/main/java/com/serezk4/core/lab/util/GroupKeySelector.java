package com.serezk4.core.lab.util;

import com.serezk4.core.lab.model.Clazz;

import java.util.NavigableMap;
import java.util.TreeMap;

public class GroupKeySelector {

    private final NavigableMap<Integer, Integer> groupRanges = new TreeMap<>();
    private final int minLength;
    private final int maxLength;
    private final int step;

    public GroupKeySelector(int minLength, int maxLength, int step) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.step = step;

        buildRanges();
    }

    private void buildRanges() {
        int groupKey = 0;
        for (int length = minLength; length <= maxLength; length += step) {
            groupRanges.put(length, groupKey++);
        }

    }

    public int selectGroupKey(final Clazz clazz) {
        int length = clazz.source().replaceAll("\\n", "").length();

        return groupRanges.floorEntry(length) != null
                ? groupRanges.floorEntry(length).getValue()
                : -1;
    }

    public void printRanges() {
        groupRanges.forEach((key, value) ->
                System.out.println("Max Length: " + key + " -> Group: " + value)
        );
    }
}
