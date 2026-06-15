package com.licht_meilleur.polluted_world.world.layout;

import com.licht_meilleur.polluted_world.world.StructureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.List;

public record UnitBounds(
        String name,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int margin
) {
    public boolean intersects(UnitBounds other) {
        return minX - margin < other.maxX + other.margin
                && maxX + margin > other.minX - other.margin
                && minY - margin < other.maxY + other.margin
                && maxY + margin > other.minY - other.margin
                && minZ - margin < other.maxZ + other.margin
                && maxZ + margin > other.minZ - other.margin;
    }

    public static UnitBounds fromOriginSize(String name, BlockPos origin, Vec3i size, int margin) {
        return new UnitBounds(
                name,
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + size.getX(),
                origin.getY() + size.getY(),
                origin.getZ() + size.getZ(),
                margin
        );
    }

    public static UnitBounds fromNodes(String name, List<StructureNode> nodes, int margin) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Cannot create UnitBounds from empty nodes.");
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (StructureNode node : nodes) {
            minX = Math.min(minX, node.minX());
            minY = Math.min(minY, node.minY());
            minZ = Math.min(minZ, node.minZ());

            maxX = Math.max(maxX, node.maxX());
            maxY = Math.max(maxY, node.maxY());
            maxZ = Math.max(maxZ, node.maxZ());
        }

        return new UnitBounds(name, minX, minY, minZ, maxX, maxY, maxZ, margin);
    }
}