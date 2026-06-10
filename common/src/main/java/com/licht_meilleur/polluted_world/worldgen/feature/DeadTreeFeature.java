package com.licht_meilleur.polluted_world.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {

    public DeadTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockPos ground = origin.below();

        if (random.nextFloat() > 0.35F) {
            return false;
        }

        if (!isValidGround(level, ground)) {
            return false;
        }

        int height = 2 + random.nextInt(4); // 2〜5ブロック

        for (int y = 0; y < height; y++) {
            BlockPos pos = origin.above(y);

            if (!level.isEmptyBlock(pos) && !level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }

        for (int y = 0; y < height; y++) {
            level.setBlock(
                    origin.above(y),
                    Blocks.PALE_OAK_LOG.defaultBlockState(),
                    2
            );
        }

        // 低確率で横枝
        if (height >= 3 && random.nextFloat() < 0.45F) {
            BlockPos branchBase = origin.above(1 + random.nextInt(height - 1));
            BlockPos branch = branchBase.relative(
                    net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random)
            );

            if (level.isEmptyBlock(branch) || level.getBlockState(branch).canBeReplaced()) {
                level.setBlock(branch, Blocks.PALE_OAK_LOG.defaultBlockState(), 2);
            }
        }

        return true;
    }

    private static boolean isValidGround(WorldGenLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.SNOW_BLOCK);
    }
}