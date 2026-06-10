package com.licht_meilleur.polluted_world.worldgen;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class PollutedRegion extends Region {

    public PollutedRegion() {
        super(PollutedWorldMod.id("polluted_region"), RegionType.OVERWORLD, 600);
    }

    @Override
    public void addBiomes(
            Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper
    ) {

        // 汚染雪原
        addBiome(
                mapper,
                Climate.Parameter.span(-1.0F, -0.05F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                ModBiomes.POLLUTED_SNOWFIELD
        );

        // 汚染枯れ森（メイン）
        addBiome(
                mapper,
                Climate.Parameter.span(-0.04F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                ModBiomes.POLLUTED_DEAD_FOREST
        );

        // オアシス（確認用に今だけ広め）
        addBiome(
                mapper,
                Climate.Parameter.span(0.20F, 1.0F),
                Climate.Parameter.span(0.20F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-0.40F, 0.40F),
                ModBiomes.CLEAN_OASIS
        );
    }

    private static void addBiome(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
            Climate.Parameter temperature,
            Climate.Parameter humidity,
            Climate.Parameter continentalness,
            Climate.Parameter erosion,
            Climate.Parameter weirdness,
            ResourceKey<Biome> biome
    ) {
        mapper.accept(Pair.of(
                Climate.parameters(
                        temperature,
                        humidity,
                        continentalness,
                        erosion,
                        Climate.Parameter.point(0.0F),
                        weirdness,
                        0.0F
                ),
                biome
        ));
    }
}