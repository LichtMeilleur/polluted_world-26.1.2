package com.licht_meilleur.polluted_world.pollution;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class SurfacePollutionTransformer {

    private SurfacePollutionTransformer() {
    }

    public static void transformChunk(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getMinBlockX() + localX;
                int z = chunkPos.getMinBlockZ() + localZ;

                int surfaceY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

                transformColumn(level, new BlockPos(x, surfaceY, z), x, z);
            }
        }
    }

    private static void transformColumn(ServerLevel level, BlockPos surface, int x, int z) {
        for (int dy = -3; dy <= 16; dy++) {
            BlockPos pos = surface.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);

            int seed = hash(pos.getX(), pos.getY(), pos.getZ());

            if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, randomBool(seed, 75)
                        ? Blocks.COARSE_DIRT.defaultBlockState()
                        : Blocks.DIRT.defaultBlockState(), 2);
                continue;
            }

            if (state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SAPLINGS)) {

                if (randomBool(seed, 25)) {
                    level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                } else {
                    level.removeBlock(pos, false);
                }
                continue;
            }

            if (state.is(BlockTags.LEAVES)) {
                if (randomBool(seed, 80)) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    private static int hash(int x, int y, int z) {
        int h = x * 73428767 ^ y * 912931 ^ z * 42317861;
        h ^= h << 13;
        h ^= h >>> 17;
        h ^= h << 5;
        return h;
    }

    private static boolean randomBool(int seed, int percent) {
        return Math.floorMod(seed, 100) < percent;
    }
}