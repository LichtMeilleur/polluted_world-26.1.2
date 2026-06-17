package com.licht_meilleur.polluted_world.world.spawn;

import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class PollutedSurfaceMobSpawner {

    private static final int SURFACE_INTERVAL = 100;
    private static final int RAIL_INTERVAL = 200;

    private PollutedSurfaceMobSpawner() {
    }

    public static void tick(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.level();

        if (player.tickCount % SURFACE_INTERVAL == 0) {
            trySpawnSurface(level, player);
        }

        if (player.tickCount % RAIL_INTERVAL == 0) {
            trySpawnNearRail(level, player);
        }
    }

    private static void trySpawnSurface(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        if (random.nextInt(3) != 0) return;

        BlockPos pos = randomAround(player.blockPosition(), random, 28, 64);
        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );

        BlockPos spawnPos = new BlockPos(pos.getX(), y, pos.getZ());

        if (!PollutionLogic.isPollutedPosition(level, spawnPos)) return;
        if (!level.canSeeSky(spawnPos)) return;
        if (isNearVillageArea(level, spawnPos)) return;

        spawnCommon(level, spawnPos);
    }

    private static void trySpawnNearRail(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        if (random.nextInt(5) != 0) return;

        BlockPos center = player.blockPosition();

        for (int i = 0; i < 16; i++) {
            BlockPos pos = randomAround(center, random, 20, 56);

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    pos.getX(),
                    pos.getZ()
            );

            BlockPos spawnPos = new BlockPos(pos.getX(), y, pos.getZ());

            if (isNearVillageArea(level, spawnPos)) continue;
            if (!isNearRail(level, spawnPos)) continue;

            spawnCommon(level, spawnPos);
            return;
        }
    }

    private static BlockPos randomAround(BlockPos center, RandomSource random, int min, int max) {
        int distance = min + random.nextInt(max - min + 1);
        int dx = random.nextBoolean() ? distance : -distance;
        int dz = random.nextInt(max * 2 + 1) - max;

        if (random.nextBoolean()) {
            int t = dx;
            dx = dz;
            dz = t;
        }

        return center.offset(dx, 0, dz);
    }

    private static void spawnCommon(ServerLevel level, BlockPos pos) {
        List<EntityType<? extends Mob>> types = List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.SPIDER
        );

        EntityType<? extends Mob> type = types.get(level.getRandom().nextInt(types.size()));

        Mob mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) return;

        mob.snapTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F,
                0.0F
        );

        mob.addTag(PollutedSpawnMarkerProcessor.NO_SUN_BURN_TAG);
        mob.setPersistenceRequired();

        level.addFreshEntity(mob);
    }

    private static boolean isNearRail(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -3, -8), center.offset(8, 3, 8))) {
            if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.RAIL)
                    || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.POWERED_RAIL)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNearVillageArea(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(48.0D, 24.0D, 48.0D);

        // ひとまず村人がいる場所＝居住区扱い
        return !level.getEntitiesOfClass(
                net.minecraft.world.entity.npc.villager.Villager.class,
                area
        ).isEmpty();
    }
}