package com.licht_meilleur.polluted_world.worldgen;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {

    public static final ResourceKey<Biome> POLLUTED_DEAD_FOREST =
            ResourceKey.create(Registries.BIOME, PollutedWorldMod.id("polluted_dead_forest"));

    public static final ResourceKey<Biome> POLLUTED_SNOWFIELD =
            ResourceKey.create(Registries.BIOME, PollutedWorldMod.id("polluted_snowfield"));

    public static final ResourceKey<Biome> CLEAN_OASIS =
            ResourceKey.create(Registries.BIOME, PollutedWorldMod.id("clean_oasis"));

    private ModBiomes() {
    }
}