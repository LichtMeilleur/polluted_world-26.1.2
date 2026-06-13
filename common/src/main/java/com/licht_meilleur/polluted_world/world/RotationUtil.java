package com.licht_meilleur.polluted_world.world;

import net.minecraft.world.level.block.Rotation;

public class RotationUtil {

    public static Rotation add(Rotation parent, Rotation child) {
        int value = toQuarter(parent) + toQuarter(child);
        return fromQuarter(value);
    }

    private static int toQuarter(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
    }

    private static Rotation fromQuarter(int value) {
        return switch ((value % 4 + 4) % 4) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}