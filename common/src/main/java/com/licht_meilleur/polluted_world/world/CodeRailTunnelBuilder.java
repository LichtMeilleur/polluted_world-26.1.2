package com.licht_meilleur.polluted_world.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private static final int LEAD_IN = 4;


    public static void generateTunnel(
            ServerLevel level,
            BlockPos startRailPos,
            Direction startForward,
            BlockPos endRailPos,
            Direction endForward
    ) {
        List<BlockPos> path = new ArrayList<>();

        BlockPos current = startRailPos;
        addUnique(path, current);

        BlockPos startLeadEnd = startRailPos.relative(startForward, LEAD_IN);
        BlockPos endLeadEnd = endRailPos.relative(endForward.getOpposite(), LEAD_IN);

        current = lineTo(path, current, startLeadEnd);

        current = lineX(path, current, endLeadEnd.getX());
        current = lineZ(path, current, endLeadEnd.getZ());

        current = lineTo(path, current, endRailPos);

        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            Direction forward = getForwardDirection(prev, pos, next);

            carveTunnelCell(level, pos, forward, path);
        }

        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            placeRail(level, prev, pos, next, i);
        }
    }

    public static void generateTunnelFromOuts(
            ServerLevel level,
            BlockPos startOutRailPos,
            Direction startForward,
            BlockPos endOutRailPos,
            Direction endForward
    ) {
        System.out.println("[PollutedWorld] CodeRail startOut=" + startOutRailPos
                + " startForward=" + startForward
                + " startTip=" + startOutRailPos.relative(startForward, LEAD_IN));

        System.out.println("[PollutedWorld] CodeRail endOut=" + endOutRailPos
                + " endForward=" + endForward
                + " endTip=" + endOutRailPos.relative(endForward, LEAD_IN));

        BlockPos startTip = startOutRailPos.relative(startForward, LEAD_IN);
        BlockPos endTip = endOutRailPos.relative(endForward, LEAD_IN);

        buildSegment(level, startOutRailPos, startTip);
        buildSegment(level, endOutRailPos, endTip);
        buildSegment(level, startTip, endTip);
    }

    private static void buildSegment(ServerLevel level, BlockPos from, BlockPos to) {
        List<BlockPos> path = new ArrayList<>();

        BlockPos current = from;
        addUnique(path, current);

        current = lineX(path, current, to.getX());
        lineZ(path, current, to.getZ());

        // 先にレールを置く
        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            placeRail(level, prev, pos, next, i);
        }

        // 後から周囲を掘る。ただしレール位置は消さない
        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            Direction forward = getForwardDirection(prev, pos, next);
            carveTunnelCell(level, pos, forward, path);
        }
    }



    private static BlockPos lineTo(List<BlockPos> path, BlockPos start, BlockPos target) {
        BlockPos current = start;

        current = lineX(path, current, target.getX());
        current = lineZ(path, current, target.getZ());

        return current;
    }

    private static BlockPos lineX(List<BlockPos> path, BlockPos start, int targetX) {
        BlockPos current = start;
        int step = Integer.compare(targetX, current.getX());

        while (current.getX() != targetX) {
            current = current.offset(step, 0, 0);
            addUnique(path, current);
        }

        return current;
    }

    private static BlockPos lineZ(List<BlockPos> path, BlockPos start, int targetZ) {
        BlockPos current = start;
        int step = Integer.compare(targetZ, current.getZ());

        while (current.getZ() != targetZ) {
            current = current.offset(0, 0, step);
            addUnique(path, current);
        }

        return current;
    }

    private static void addUnique(List<BlockPos> path, BlockPos pos) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(pos)) {
            path.add(pos);
        }
    }

    private static void carveTunnelCell(ServerLevel level, BlockPos railPos, Direction forward, List<BlockPos> railPath) {
        Direction side = forward.getClockWise();

        for (int w = -HALF_WIDTH; w <= HALF_WIDTH; w++) {
            BlockPos center = railPos.relative(side, w);

            level.setBlock(center.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);

            for (int dy = 0; dy <= AIR_HEIGHT; dy++) {
                BlockPos airPos = center.above(dy);

                // レール本体の位置は絶対に消さない
                if (railPath.contains(airPos)) {
                    continue;
                }

                level.setBlock(airPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Direction getForwardDirection(BlockPos prev, BlockPos pos, BlockPos next) {
        if (next != null) {
            return directionFromTo(pos, next);
        }

        if (prev != null) {
            return directionFromTo(prev, pos);
        }

        return Direction.NORTH;
    }

    private static Direction directionFromTo(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());

        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        if (dz < 0) return Direction.NORTH;

        return Direction.NORTH;
    }

    private static void placeRail(ServerLevel level, BlockPos prev, BlockPos pos, BlockPos next, int index) {
        RailShape shape = getRailShape(prev, pos, next);
        boolean corner = shape != RailShape.EAST_WEST && shape != RailShape.NORTH_SOUTH;

        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (!corner && index % 12 == 0) {
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

    private static RailShape getRailShape(BlockPos prev, BlockPos pos, BlockPos next) {
        if (prev == null && next == null) {
            return RailShape.NORTH_SOUTH;
        }

        if (prev == null) {
            return next.getX() != pos.getX() ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

        if (next == null) {
            return prev.getX() != pos.getX() ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

        boolean north = prev.getZ() < pos.getZ() || next.getZ() < pos.getZ();
        boolean south = prev.getZ() > pos.getZ() || next.getZ() > pos.getZ();
        boolean west = prev.getX() < pos.getX() || next.getX() < pos.getX();
        boolean east = prev.getX() > pos.getX() || next.getX() > pos.getX();

        if (north && east) return RailShape.NORTH_EAST;
        if (north && west) return RailShape.NORTH_WEST;
        if (south && east) return RailShape.SOUTH_EAST;
        if (south && west) return RailShape.SOUTH_WEST;

        return east || west ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }
}