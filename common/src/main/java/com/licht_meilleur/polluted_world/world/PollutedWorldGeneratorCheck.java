package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

public final class PollutedWorldGeneratorCheck {
    private static final ResourceKey<NoiseGeneratorSettings> POLLUTED_OVERWORLD_SIMPLE =
            ResourceKey.create(
                    Registries.NOISE_SETTINGS,
                    PollutedWorldMod.id("polluted_overworld_simple")
            );

    private PollutedWorldGeneratorCheck() {
    }

    public static boolean isPollutedWorld(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)) {
            return false;
        }

        return generator.generatorSettings().is(POLLUTED_OVERWORLD_SIMPLE);
    }
}