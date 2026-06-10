package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomeTags {

    public static final TagKey<Biome> CLEAN_BIOMES =
            TagKey.create(Registries.BIOME, PollutedWorldMod.id("clean_biomes"));

    public static final TagKey<Biome> POLLUTED_BIOMES =
            TagKey.create(Registries.BIOME, PollutedWorldMod.id("polluted_biomes"));

    private ModBiomeTags() {
    }
}