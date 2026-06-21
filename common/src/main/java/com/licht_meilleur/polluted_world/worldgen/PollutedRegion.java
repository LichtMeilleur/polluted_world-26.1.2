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
        super(PollutedWorldMod.id("polluted_region"), RegionType.OVERWORLD, 1000);
    }


    @Override
    public void addBiomes(
            Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper
    ) {
        // 汚染雪原：寒冷域メイン
        addBiome(
                mapper,
                Climate.Parameter.span(-1.0F, -0.35F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                ModBiomes.POLLUTED_SNOWFIELD
        );

        // 汚染枯れ森：通常域メイン
        addBiome(
                mapper,
                Climate.Parameter.span(-0.34F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                Climate.Parameter.span(-1.0F, 1.0F),
                ModBiomes.POLLUTED_DEAD_FOREST
        );

        // オアシス：かなりレア
        addBiome(
                mapper,
                Climate.Parameter.span(0.82F, 1.0F),    // 暑い// temperature 温度
                Climate.Parameter.span(0.88F, 1.0F),    // 湿った// humidity 湿度
                Climate.Parameter.span(-0.10F, 0.15F),  // continentalness 大陸性
                Climate.Parameter.span(-0.12F, 0.12F),  // erosion 侵食
                Climate.Parameter.span(0.02F, 1.0F),    // weirdness 変則性
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