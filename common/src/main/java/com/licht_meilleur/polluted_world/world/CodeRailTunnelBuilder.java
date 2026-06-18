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




    private static Direction directionFromTo(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());

        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        if (dz < 0) return Direction.NORTH;

        return Direction.NORTH;
    }


    private static void placeRailByMarker(ServerLevel level, BlockPos prev, BlockPos pos, BlockPos next) {
        BlockState floor = level.getBlockState(pos.below());

        if (!floor.is(Blocks.REDSTONE_BLOCK) && !floor.is(Blocks.SMOOTH_STONE)) {
            return;
        }

        BlockPos fixedPrev = prev;
        BlockPos fixedNext = next;

        if (fixedPrev == null) {
            fixedPrev = findAdjacentRail(level, pos, fixedNext);
        }

        if (fixedNext == null) {
            fixedNext = findAdjacentRail(level, pos, fixedPrev);
        }

        RailShape shape = getRailShape(fixedPrev, pos, fixedNext);

        boolean endpoint = prev == null || next == null;

        if (!endpoint && floor.is(Blocks.REDSTONE_BLOCK) && isStraight(shape)) {
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

    private static BlockPos findAdjacentRail(ServerLevel level, BlockPos pos, BlockPos ignore) {
        BlockPos[] candidates = new BlockPos[] {
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };

        for (BlockPos candidate : candidates) {
            if (ignore != null && candidate.equals(ignore)) {
                continue;
            }

            BlockState state = level.getBlockState(candidate);

            if (state.is(Blocks.RAIL) || state.is(Blocks.POWERED_RAIL)) {
                return candidate;
            }
        }

        return null;
    }

    private static RailShape getRailShape(BlockPos prev, BlockPos pos, BlockPos next) {
        if (prev == null || next == null) {
            boolean eastWest = next != null
                    ? next.getX() != pos.getX()
                    : prev != null && prev.getX() != pos.getX();

            return eastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

        int dx1 = Integer.compare(prev.getX() - pos.getX(), 0);
        int dz1 = Integer.compare(prev.getZ() - pos.getZ(), 0);
        int dx2 = Integer.compare(next.getX() - pos.getX(), 0);
        int dz2 = Integer.compare(next.getZ() - pos.getZ(), 0);

        boolean hasEast = dx1 > 0 || dx2 > 0;
        boolean hasWest = dx1 < 0 || dx2 < 0;
        boolean hasSouth = dz1 > 0 || dz2 > 0;
        boolean hasNorth = dz1 < 0 || dz2 < 0;

        if (hasEast && hasWest) {
            return RailShape.EAST_WEST;
        }

        if (hasNorth && hasSouth) {
            return RailShape.NORTH_SOUTH;
        }

        if (hasSouth && hasEast) {
            return RailShape.SOUTH_EAST;
        }

        if (hasSouth && hasWest) {
            return RailShape.SOUTH_WEST;
        }

        if (hasNorth && hasEast) {
            return RailShape.NORTH_EAST;
        }

        if (hasNorth && hasWest) {
            return RailShape.NORTH_WEST;
        }

        return RailShape.NORTH_SOUTH;
    }

    private static boolean isStraight(RailShape shape) {
        return shape == RailShape.EAST_WEST
                || shape == RailShape.NORTH_SOUTH;
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





    public static void generateSeparatedLaneConnectors(
            ServerLevel level,
            BlockPos westStart,
            BlockPos westEnd,
            BlockPos eastStart,
            BlockPos eastEnd,
            int sideOffset
    ) {
        if (westStart.getY() != westEnd.getY()
                || eastStart.getY() != eastEnd.getY()
                || westStart.getY() != eastStart.getY()) {
            throw new IllegalStateException(
                    "Lane connector Y mismatch: "
                            + westStart + " -> " + westEnd
                            + " / "
                            + eastStart + " -> " + eastEnd
            );
        }

        int westMaxX = Math.max(westStart.getX(), westEnd.getX());
        int eastMinX = Math.min(eastStart.getX(), eastEnd.getX());


        int dividerX;

        if (westEnd.getX() < eastEnd.getX()) {
            dividerX = (westEnd.getX() + eastEnd.getX()) / 2;
        } else if (westStart.getX() < eastStart.getX()) {
            dividerX = (westStart.getX() + eastStart.getX()) / 2;
        } else {
            dividerX = (Math.min(westStart.getX(), westEnd.getX())
                    + Math.max(eastStart.getX(), eastEnd.getX())) / 2;
        }

        int westRouteX = Math.min(westStart.getX(), westEnd.getX()) - sideOffset;
        int eastRouteX = Math.max(eastStart.getX(), eastEnd.getX()) + sideOffset;

        generateDividerSafePath(
                level,
                westStart,
                westEnd,
                westRouteX,
                dividerX,
                true
        );

        generateDividerSafePath(
                level,
                eastStart,
                eastEnd,
                eastRouteX,
                dividerX,
                false
        );


    }

    private static void generateDividerSafePath(
            ServerLevel level,
            BlockPos start,
            BlockPos end,
            int routeX,
            int dividerX,
            boolean westLane
    ) {
        if (westLane && routeX >= dividerX) {
            throw new IllegalStateException("West route crosses divider: routeX=" + routeX + " dividerX=" + dividerX);
        }

        if (!westLane && routeX <= dividerX) {
            throw new IllegalStateException("East route crosses divider: routeX=" + routeX + " dividerX=" + dividerX);
        }

        List<BlockPos> path = new ArrayList<>();

        BlockPos current = start;
        addUnique(path, current);

        current = lineX(path, current, routeX);
        current = lineZ(path, current, end.getZ());
        lineX(path, current, end.getX());

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

    public static void debugDrawDividerLine(
            ServerLevel level,
            BlockPos westA,
            BlockPos westB,
            BlockPos eastA,
            BlockPos eastB
    ) {
        int dividerX = (
                Math.max(westA.getX(), westB.getX())
                        + Math.min(eastA.getX(), eastB.getX())
        ) / 2;

        int minZ = Math.min(
                Math.min(westA.getZ(), westB.getZ()),
                Math.min(eastA.getZ(), eastB.getZ())
        );

        int maxZ = Math.max(
                Math.max(westA.getZ(), westB.getZ()),
                Math.max(eastA.getZ(), eastB.getZ())
        );

        int y = westA.getY();

        for (int z = minZ - 32; z <= maxZ + 32; z++) {
            level.setBlock(
                    new BlockPos(dividerX, y, z),
                    Blocks.RED_CONCRETE.defaultBlockState(),
                    Block.UPDATE_ALL
            );
        }


    }



}