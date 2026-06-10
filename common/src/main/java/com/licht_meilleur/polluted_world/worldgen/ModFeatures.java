package com.licht_meilleur.polluted_world.worldgen;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.worldgen.feature.DeadTreeFeature;
import com.licht_meilleur.polluted_world.worldgen.feature.DryGrassPatchFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ModFeatures {

    public static final Feature<NoneFeatureConfiguration> DEAD_TREE =
            new DeadTreeFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> DRY_GRASS_PATCH =
            new DryGrassPatchFeature(NoneFeatureConfiguration.CODEC);

    private ModFeatures() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.FEATURE,
                PollutedWorldMod.id("dead_tree"),
                DEAD_TREE
        );
        Registry.register(
                BuiltInRegistries.FEATURE,
                PollutedWorldMod.id("dry_grass_patch"),
                DRY_GRASS_PATCH
        );
    }
}