package com.licht_meilleur.polluted_world.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.List;

public class CodeRailTunnelBuilder {

    public static void generateTunnel(ServerLevel level, BlockPos start, BlockPos end) {
        List<RailStep> path = new ArrayList<>();

        BlockPos current = start;

        current = collectLineX(path, current, end.getX());
        collectLineZ(path, current, end.getZ());

        path.add(new RailStep(end, false));

        for (RailStep step : path) {
            carveRailSpace(level, step.pos());
        }

        for (int i = 0; i < path.size(); i++) {
            RailStep step = path.get(i);
            prepareRailFloor(level, step.pos());
            placeRail(level, step.pos(), i, step.eastWest());
        }
    }

    private static BlockPos collectLineX(List<RailStep> path, BlockPos start, int targetX) {
        int step = Integer.compare(targetX, start.getX());
        BlockPos current = start;

        while (current.getX() != targetX) {
            path.add(new RailStep(current, true));
            current = current.offset(step, 0, 0);
        }

        return current;
    }

    private static BlockPos collectLineZ(List<RailStep> path, BlockPos start, int targetZ) {
        int step = Integer.compare(targetZ, start.getZ());
        BlockPos current = start;

        while (current.getZ() != targetZ) {
            path.add(new RailStep(current, false));
            current = current.offset(0, 0, step);
        }

        return current;
    }

    private static void carveRailSpace(ServerLevel level, BlockPos railPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = railPos.offset(dx, dy, dz);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void prepareRailFloor(ServerLevel level, BlockPos railPos) {
        level.setBlock(railPos.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void placeRail(ServerLevel level, BlockPos pos, int index, boolean eastWest) {
        RailShape shape = eastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;

        if (index % 12 == 0) {
            level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

            BlockState poweredRail = Blocks.POWERED_RAIL.defaultBlockState()
                    .setValue(PoweredRailBlock.SHAPE, shape)
                    .setValue(PoweredRailBlock.POWERED, true);

            level.setBlock(pos, poweredRail, Block.UPDATE_ALL);
        } else {
            BlockState rail = Blocks.RAIL.defaultBlockState()
                    .setValue(RailBlock.SHAPE, shape);

            level.setBlock(pos, rail, Block.UPDATE_ALL);
        }
    }

    private record RailStep(BlockPos pos, boolean eastWest) {
    }
}