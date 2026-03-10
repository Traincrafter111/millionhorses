package com.tobyink.millionhorses.entity.constant;

public enum MovementMode {
    WANDERING, FOLLOWING, SITTING;

    public MovementMode next() {
        MovementMode[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}