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

    private static final int HALF_WIDTH = 4;
    private static final int AIR_HEIGHT = 5;

    public static void generateTunnel(ServerLevel level, BlockPos startRailPos, BlockPos endRailPos) {
        List<BlockPos> path = new ArrayList<>();

        BlockPos current = startRailPos;

        while (current.getX() != endRailPos.getX()) {
            path.add(current);
            int step = Integer.compare(endRailPos.getX(), current.getX());
            current = current.offset(step, 0, 0);
        }

        while (current.getZ() != endRailPos.getZ()) {
            path.add(current);
            int step = Integer.compare(endRailPos.getZ(), current.getZ());
            current = current.offset(0, 0, step);
        }

        path.add(endRailPos);

        for (BlockPos pos : path) {
            carveTunnelCell(level, pos);
        }

        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);

            boolean eastWest = i + 1 < path.size()
                    ? path.get(i + 1).getX() != pos.getX()
                    : i > 0 && path.get(i - 1).getX() != pos.getX();

            placeRail(level, pos, i, eastWest);
        }
    }

    private static BlockPos digLineX(ServerLevel level, BlockPos start, int targetX) {
        int step = Integer.compare(targetX, start.getX());
        BlockPos current = start;
        int index = 0;

        while (current.getX() != targetX) {
            carveTunnelCell(level, current);
            placeRail(level, current, index, true);

            current = current.offset(step, 0, 0);
            index++;
        }

        return current;
    }

    private static BlockPos digLineZ(ServerLevel level, BlockPos start, int targetZ) {
        int step = Integer.compare(targetZ, start.getZ());
        BlockPos current = start;
        int index = 0;

        while (current.getZ() != targetZ) {
            carveTunnelCell(level, current);
            placeRail(level, current, index, false);

            current = current.offset(0, 0, step);
            index++;
        }

        return current;
    }

    private static void carveTunnelCell(ServerLevel level, BlockPos railPos) {
        // 床
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_WIDTH; dz <= HALF_WIDTH; dz++) {
                BlockPos floorPos = railPos.offset(dx, -1, dz);
                level.setBlock(floorPos, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // 空洞
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dy = 0; dy <= AIR_HEIGHT; dy++) {
                for (int dz = -HALF_WIDTH; dz <= HALF_WIDTH; dz++) {
                    BlockPos airPos = railPos.offset(dx, dy, dz);
                    level.setBlock(airPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
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

    public static void placeConnectorRail(ServerLevel level, BlockPos a, BlockPos b) {
        boolean eastWest = a.getZ() == b.getZ();

        RailShape shape = eastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;

        BlockState rail = Blocks.RAIL.defaultBlockState()
                .setValue(RailBlock.SHAPE, shape);

        level.setBlock(a, rail, Block.UPDATE_ALL);
        level.setBlock(b, rail, Block.UPDATE_ALL);
    }
}