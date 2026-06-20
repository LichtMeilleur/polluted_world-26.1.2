package com.licht_meilleur.polluted_world.world.spawn;

import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class PollutedSurfaceMobSpawner {

    private static final int SURFACE_INTERVAL = 20;
    private static final int RAIL_INTERVAL = 40;

    private PollutedSurfaceMobSpawner() {
    }

    public static void tick(ServerPlayer player) {
        // テスト中はCreativeでも湧かせる
        if (player.isSpectator()) return;

        ServerLevel level = player.level();

        if (player.tickCount % SURFACE_INTERVAL == 0) {
            trySpawnSurfaceInLoadedChunks(level, player);
        }

        if (player.tickCount % RAIL_INTERVAL == 0) {
            trySpawnNearRail(level, player);
        }
    }

    private static void trySpawnSurfaceInLoadedChunks(ServerLevel level, ServerPlayer player) {
        if (isMonsterCapReached(level, player.blockPosition(), 96, 10)) {
            return;
        }

        RandomSource random = level.getRandom();

        // テスト中は確率を緩める
        // if (random.nextInt(3) != 0) return;

        BlockPos playerPos = player.blockPosition();

        for (int i = 0; i < 32; i++) {
            int chunkDx = random.nextInt(9) - 4; // -4〜+4チャンク
            int chunkDz = random.nextInt(9) - 4;

            int chunkX = (playerPos.getX() >> 4) + chunkDx;
            int chunkZ = (playerPos.getZ() >> 4) + chunkDz;

            if (!level.hasChunk(chunkX, chunkZ)) {
                continue;
            }

            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos spawnPos = new BlockPos(x, y, z);

            if (!level.canSeeSky(spawnPos)) continue;
            if (isNearVillageArea(level, spawnPos)) continue;

            // 汚染判定は一旦コメントアウト推奨
            // if (!PollutionLogic.isPollutedPosition(level, spawnPos)) continue;

            spawnCommon(level, spawnPos);
            return;
        }
    }



    private static void trySpawnNearRail(ServerLevel level, ServerPlayer player) {
        if (isMonsterCapReached(level, player.blockPosition(), 96, 10)) {
            return;
        }

        RandomSource random = level.getRandom();

        if (random.nextInt(5) != 0) return;

        BlockPos center = player.blockPosition();

        for (int i = 0; i < 24; i++) {
            BlockPos pos = randomAround(center, random, 12, 36);

            // 地下レール想定なので、プレイヤーのY付近を見る
            BlockPos spawnPos = new BlockPos(
                    pos.getX(),
                    center.getY(),
                    pos.getZ()
            );

            if (isNearVillageArea(level, spawnPos)) continue;
            if (!isNearRail(level, spawnPos)) continue;
            if (!canSpawnAt(level, spawnPos)) continue;

            spawnCommon(level, spawnPos);
            return;
        }
    }

    private static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
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

        mob.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(pos),
                EntitySpawnReason.NATURAL,
                null
        );

        equipSkeletonBow(mob);

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

    private static void equipSkeletonBow(Mob mob) {
        if (mob instanceof AbstractSkeleton) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }

    private static boolean isMonsterCapReached(
            ServerLevel level,
            BlockPos center,
            int radius,
            int max
    ) {
        AABB area = new AABB(center).inflate(radius, 48.0D, radius);

        int count = level.getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class,
                area
        ).size();

        return count >= max;
    }

}