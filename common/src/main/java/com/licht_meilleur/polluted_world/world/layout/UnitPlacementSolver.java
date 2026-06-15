package com.licht_meilleur.polluted_world.world.layout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.List;

public class UnitPlacementSolver {

    public static BlockPos findSafeOrigin2D(
            List<UnitBounds> placedUnits,
            String name,
            BlockPos start,
            Vec3i size,
            int margin,
            int stepX,
            int stepZ,
            int maxZAttempts,
            int maxXAttempts
    ) {
        for (int z = 0; z < maxZAttempts; z++) {
            int dz = stepZ * z;

            for (int x = 0; x <= maxXAttempts; x++) {
                int dx = stepX * x;

                BlockPos right = start.offset(dx, 0, dz);
                if (isSafe(placedUnits, name, right, size, margin)) {
                    return right;
                }

                if (dx != 0) {
                    BlockPos left = start.offset(-dx, 0, dz);
                    if (isSafe(placedUnits, name, left, size, margin)) {
                        return left;
                    }
                }
            }
        }

        throw new IllegalStateException("No safe origin found for unit: " + name);
    }

    private static boolean isSafe(
            List<UnitBounds> placedUnits,
            String name,
            BlockPos origin,
            Vec3i size,
            int margin
    ) {
        UnitBounds candidate = UnitBounds.fromOriginSize(name, origin, size, margin);

        for (UnitBounds placed : placedUnits) {
            if (candidate.intersects(placed)) {
                return false;
            }
        }

        return true;
    }
}