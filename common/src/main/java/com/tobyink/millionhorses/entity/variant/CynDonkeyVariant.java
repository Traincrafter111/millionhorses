package com.tobyink.millionhorses.entity.variant;

import java.util.Arrays;
import java.util.Comparator;

public enum CynDonkeyVariant {
    DEFAULT(0),
    GRAY(1),
    BROWN(2);

    private static final CynDonkeyVariant[] BY_ID = Arrays.stream(values())
            .sorted(Comparator.comparingInt(CynDonkeyVariant::getId))
            .toArray(CynDonkeyVariant[]::new);

    private final int id;

    CynDonkeyVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static CynDonkeyVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}