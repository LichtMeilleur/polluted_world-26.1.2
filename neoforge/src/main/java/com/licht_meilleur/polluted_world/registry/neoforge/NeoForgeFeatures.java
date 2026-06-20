package com.licht_meilleur.polluted_world.registry.neoforge;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.worldgen.feature.DeadTreeFeature;
import com.licht_meilleur.polluted_world.worldgen.feature.DryGrassPatchFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, PollutedWorldMod.MOD_ID);

    public static final DeferredHolder<Feature<?>, DeadTreeFeature> DEAD_TREE =
            FEATURES.register("dead_tree", () ->
                    new DeadTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, DryGrassPatchFeature> DRY_GRASS_PATCH =
            FEATURES.register("dry_grass_patch", () ->
                    new DryGrassPatchFeature(NoneFeatureConfiguration.CODEC));

    private NeoForgeFeatures() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}