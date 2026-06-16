package com.licht_meilleur.polluted_world.pollution;

import com.licht_meilleur.polluted_world.world.ModBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SurfacePollutionTransformer {

    private SurfacePollutionTransformer() {
    }

    public static void transformChunk(ServerLevel level, LevelChunk chunk) {
        transformSomeColumns(level, chunk, 0, 256);
    }

    public static int transformSomeColumns(
            ServerLevel level,
            LevelChunk chunk,
            int startColumn,
            int maxColumns
    ) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return maxColumns;
        }

        ChunkPos chunkPos = chunk.getPos();

        int processed = 0;

        for (int index = startColumn; index < 256 && processed < maxColumns; index++) {
            int localX = index & 15;
            int localZ = index >> 4;

            int x = chunkPos.getMinBlockX() + localX;
            int z = chunkPos.getMinBlockZ() + localZ;

            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos surface = new BlockPos(x, surfaceY, z);

            if (!level.getBiome(surface).is(ModBiomeTags.POLLUTED_BIOMES)) {
                processed++;
                continue;
            }

            transformColumn(level, surface, x, z);
            processed++;
        }

        return processed;
    }

    private static void transformColumn(ServerLevel level, BlockPos surface, int x, int z) {
        for (int dy = -6; dy <= 12; dy++) {
            BlockPos pos = surface.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);

            int seed = hash(pos.getX(), pos.getY(), pos.getZ());

            if (isSurfaceWaterOrIce(state)) {
                if (surface.getY() >= 48) {
                    level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
                }
                continue;
            }

            if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(
                        pos,
                        randomBool(seed, 70)
                                ? Blocks.COARSE_DIRT.defaultBlockState()
                                : Blocks.DIRT.defaultBlockState(),
                        2
                );
                continue;
            }

            if (state.is(Blocks.DIRT)) {
                if (randomBool(seed, 20)) {
                    level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
                continue;
            }

            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
                if (randomBool(seed, 45)) {
                    level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
                }
                continue;
            }

            if (state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SAPLINGS)) {

                if (randomBool(seed, 35)) {
                    level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                } else {
                    level.removeBlock(pos, false);
                }
                continue;
            }

            if (state.is(BlockTags.LEAVES)) {
                if (randomBool(seed, 85)) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    private static boolean isSurfaceWaterOrIce(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(Blocks.ICE)
                || state.is(Blocks.FROSTED_ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE);
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