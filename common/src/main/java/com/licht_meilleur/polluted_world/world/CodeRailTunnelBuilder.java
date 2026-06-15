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

    private static void carveTunnelCell(ServerLevel level, BlockPos railPos) {
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_WIDTH; dz <= HALF_WIDTH; dz++) {
                BlockPos floorPos = railPos.offset(dx, -1, dz);
                level.setBlock(floorPos, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

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

    public static BlockPos generateForwardSpacer(
            ServerLevel level,
            BlockPos startRailPos,
            Direction forward,
            int length
    ) {
        BlockPos current = startRailPos;

        for (int i = 0; i < length; i++) {
            current = current.relative(forward);

            carveTunnelCellKeepRails(level, current, forward);
            placeRail(level, current, i, forward == Direction.EAST || forward == Direction.WEST);
        }

        return current;
    }

    private static void carveTunnelCellKeepRails(ServerLevel level, BlockPos railPos, Direction forward) {
        Direction side = forward.getClockWise();

        for (int w = -HALF_WIDTH; w <= HALF_WIDTH; w++) {
            BlockPos center = railPos.relative(side, w);

            level.setBlock(center.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);

            for (int dy = 0; dy <= AIR_HEIGHT; dy++) {
                BlockPos airPos = center.above(dy);
                BlockState existing = level.getBlockState(airPos);

                if (existing.is(Blocks.RAIL)
                        || existing.is(Blocks.POWERED_RAIL)
                        || existing.is(Blocks.REDSTONE_BLOCK)) {
                    continue;
                }

                level.setBlock(airPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public static void generateSafeConnector(ServerLevel level, BlockPos startRailPos, BlockPos endRailPos) {
        if (startRailPos.getY() != endRailPos.getY()) {
            throw new IllegalStateException(
                    "Rail connector Y mismatch: start=" + startRailPos + " end=" + endRailPos
            );
        }

        List<BlockPos> path = new ArrayList<>();

        BlockPos current = startRailPos;
        path.add(current);

        while (current.getX() != endRailPos.getX()) {
            int step = Integer.compare(endRailPos.getX(), current.getX());
            current = current.offset(step, 0, 0);
            path.add(current);
        }

        while (current.getZ() != endRailPos.getZ()) {
            int step = Integer.compare(endRailPos.getZ(), current.getZ());
            current = current.offset(0, 0, step);
            path.add(current);
        }

        for (int i = 1; i < path.size() - 1; i++) {
            carveTunnelCell(level, path.get(i));
        }

        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);

            boolean eastWest = i + 1 < path.size()
                    ? path.get(i + 1).getX() != pos.getX()
                    : i > 0 && path.get(i - 1).getX() != pos.getX();

            placeRail(level, pos, i, eastWest);

            System.out.println(
                    "[PollutedWorld] Rail="
                            + pos
            );
        }
        System.out.println(
                "[PollutedWorld] Connector start="
                        + startRailPos
                        + " end="
                        + endRailPos
                        + " length="
                        + path.size()
        );
    }
    public static void generateSafeConnectorExtended(
            ServerLevel level,
            BlockPos startRailPos,
            BlockPos endRailPos,
            int extra
    ) {
        Direction startToEnd = directionFromTo(startRailPos, endRailPos);
        Direction endToStart = directionFromTo(endRailPos, startRailPos);

        BlockPos fixedStart = startRailPos.relative(endToStart, extra);
        BlockPos fixedEnd = endRailPos.relative(startToEnd, extra);

        generateSafeConnector(level, fixedStart, fixedEnd);
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

    public static void generateDebugConnector(ServerLevel level, BlockPos startRailPos, BlockPos endRailPos) {
        if (startRailPos.getY() != endRailPos.getY()) {
            throw new IllegalStateException("Y mismatch: " + startRailPos + " -> " + endRailPos);
        }

        List<BlockPos> path = new ArrayList<>();

        BlockPos current = startRailPos;
        path.add(current);

        while (current.getX() != endRailPos.getX()) {
            int step = Integer.compare(endRailPos.getX(), current.getX());
            current = current.offset(step, 0, 0);
            path.add(current);
        }

        while (current.getZ() != endRailPos.getZ()) {
            int step = Integer.compare(endRailPos.getZ(), current.getZ());
            current = current.offset(0, 0, step);
            path.add(current);
        }

        // 1. 先に全部掘る
        for (BlockPos pos : path) {
            carveTunnelCell(level, pos);
        }

        // 2. 掘り終わってから中央床だけ目印にする
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);

            if (i % 12 == 0) {
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // 3. 最後に目印床の上へレール
        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            placeRailByMarker(level, prev, pos, next);
        }

        System.out.println("[PollutedWorld] DebugConnector start=" + startRailPos
                + " end=" + endRailPos
                + " length=" + path.size());
    }

    private static void placeRailByMarker(ServerLevel level, BlockPos prev, BlockPos pos, BlockPos next) {
        BlockState floor = level.getBlockState(pos.below());

        if (!floor.is(Blocks.REDSTONE_BLOCK) && !floor.is(Blocks.SMOOTH_STONE)) {
            return;
        }

        boolean eastWest;

        if (next != null) {
            eastWest = next.getX() != pos.getX();
        } else if (prev != null) {
            eastWest = prev.getX() != pos.getX();
        } else {
            eastWest = false;
        }

        RailShape shape = eastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;

        // 端は必ず通常レールにする
        boolean endpoint = prev == null || next == null;

        if (!endpoint && floor.is(Blocks.REDSTONE_BLOCK)) {
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

    private static final int CONNECTOR_LEAD = 8;

    public static void generateFlatConnector(ServerLevel level, BlockPos startRailPos, BlockPos endRailPos) {
        if (startRailPos.getY() != endRailPos.getY()) {
            throw new IllegalStateException("Y mismatch: " + startRailPos + " -> " + endRailPos);
        }

        List<BlockPos> path = new ArrayList<>();

        Direction mainForward = getMainZDirection(startRailPos, endRailPos);

        BlockPos startLead = startRailPos.relative(mainForward, CONNECTOR_LEAD);
        BlockPos endLead = endRailPos.relative(mainForward.getOpposite(), CONNECTOR_LEAD);

        BlockPos current = startRailPos;
        addUnique(path, current);

        current = lineZ(path, current, startLead.getZ());
        current = lineX(path, current, endLead.getX());
        current = lineZ(path, current, endLead.getZ());
        current = lineX(path, current, endRailPos.getX());
        lineZ(path, current, endRailPos.getZ());

        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            Direction forward = getForwardDirection(prev, pos, next);
            carveFlatTunnelSlice(level, pos, forward);
        }

        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);
            boolean endpoint = i == 0 || i == path.size() - 1;

            if (!endpoint && i % 12 == 0) {
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        for (int i = 0; i < path.size(); i++) {
            BlockPos prev = i > 0 ? path.get(i - 1) : null;
            BlockPos pos = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : null;

            placeRailByMarker(level, prev, pos, next);
        }
    }

    private static void addUnique(List<BlockPos> path, BlockPos pos) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(pos)) {
            path.add(pos);
        }
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

    private static Direction getMainZDirection(BlockPos start, BlockPos end) {
        return end.getZ() >= start.getZ() ? Direction.SOUTH : Direction.NORTH;
    }

    private static void carveFlatTunnelSlice(ServerLevel level, BlockPos railPos, Direction forward) {
        Direction side = forward.getClockWise();

        for (int w = -HALF_WIDTH; w <= HALF_WIDTH; w++) {
            BlockPos center = railPos.relative(side, w);

            level.setBlock(center.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);

            for (int dy = 0; dy <= AIR_HEIGHT; dy++) {
                BlockPos airPos = center.above(dy);

                BlockState existing = level.getBlockState(airPos);

                if (existing.is(Blocks.RAIL)
                        || existing.is(Blocks.POWERED_RAIL)
                        || existing.is(Blocks.REDSTONE_BLOCK)) {
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



}