package com.licht_meilleur.polluted_world.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DryGrassPatchFeature extends Feature<NoneFeatureConfiguration> {

    public DryGrassPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        boolean placed = false;

        for (int i = 0; i < 16; i++) {
            int x = origin.getX() + random.nextInt(16) - random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16) - random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos ground = pos.below();

            if (!level.isEmptyBlock(pos)) continue;
            if (!isValidGround(level, ground)) continue;

            if (random.nextFloat() < 0.75F) {
                level.setBlock(pos, Blocks.SHORT_DRY_GRASS.defaultBlockState(), 2);
            } else {
                level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
            }

            placed = true;
        }

        return placed;
    }

    private static boolean isValidGround(WorldGenLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND);
    }
}