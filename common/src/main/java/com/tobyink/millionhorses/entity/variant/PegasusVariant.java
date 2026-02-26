package com.tobyink.millionhorses.entity.variant;

import java.util.Arrays;
import java.util.Comparator;

public enum PegasusVariant {
    WHITE(0),
    PURPLE(1),
    RAINBOW(2),
    DARK(3),
    WHITE_BLUE(4),
    PURPLE_BLUE(5),
    RAINBOW_BLUE(6),
    DARK_BLUE(7);

    private static final PegasusVariant[] BY_ID = Arrays.stream(values())
            .sorted(Comparator.comparingInt(PegasusVariant::getId))
            .toArray(PegasusVariant[]::new);

    private final int id;

    PegasusVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static PegasusVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}