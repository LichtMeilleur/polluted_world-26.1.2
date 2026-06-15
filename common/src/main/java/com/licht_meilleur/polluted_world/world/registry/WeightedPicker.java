package com.licht_meilleur.polluted_world.world.registry;

import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.function.ToIntFunction;

public class WeightedPicker {

    public static <T> T pick(ServerLevel level, List<T> entries, ToIntFunction<T> weightGetter) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("Cannot pick from empty list.");
        }

        int totalWeight = 0;

        for (T entry : entries) {
            totalWeight += Math.max(0, weightGetter.applyAsInt(entry));
        }

        if (totalWeight <= 0) {
            return entries.get(level.getRandom().nextInt(entries.size()));
        }

        int roll = level.getRandom().nextInt(totalWeight);

        for (T entry : entries) {
            roll -= Math.max(0, weightGetter.applyAsInt(entry));

            if (roll < 0) {
                return entry;
            }
        }

        return entries.get(entries.size() - 1);
    }
}