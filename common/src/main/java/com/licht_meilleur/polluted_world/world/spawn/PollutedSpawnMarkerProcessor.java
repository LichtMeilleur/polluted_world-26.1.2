package com.licht_meilleur.polluted_world.world.spawn;

import com.licht_meilleur.polluted_world.world.StructureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public final class PollutedSpawnMarkerProcessor {

    public static final String NO_SUN_BURN_TAG = "polluted_world_no_sun_burn";

    private PollutedSpawnMarkerProcessor() {
    }

    public static StructureNode processAndReturn(ServerLevel level, StructureNode node) {
        process(level, node);
        return node;
    }

    public static void process(ServerLevel level, StructureNode node) {
        if (node.structureName().startsWith("station_village")) {
            return;
        }

        for (StructureTemplate.StructureBlockInfo info : node.jigsaws()) {
            String marker = getMarkerName(info);

            if (isMarker(marker, "spawn_common")) {
                spawnCommon(level, info.pos());
            } else if (isMarker(marker, "spawn_aquatic")) {
                spawnAquatic(level, info.pos());
            } else if (isMarker(marker, "spawn_rare")) {
                spawnRare(level, info.pos());
            }
        }
    }

    private static void spawnCommon(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();

        if (random.nextInt(4) == 0) return;

        List<EntityType<?>> types = List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.SPIDER
        );

        spawn(level, pos, types.get(random.nextInt(types.size())));
    }

    private static void spawnAquatic(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();

        if (random.nextInt(5) == 0) return;

        spawn(level, pos, EntityType.DROWNED);
    }

    private static void spawnRare(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();

        if (random.nextBoolean()) return;

        List<EntityType<?>> types = List.of(
                EntityType.WITCH,
                EntityType.ZOMBIE_VILLAGER
        );

        spawn(level, pos, types.get(random.nextInt(types.size())));
    }

    private static void spawn(ServerLevel level, BlockPos pos, EntityType<?> type) {
        Entity entity = type.create(level, EntitySpawnReason.STRUCTURE);

        entity.snapTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F,
                0.0F
        );

        entity.addTag(NO_SUN_BURN_TAG);

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        level.addFreshEntity(entity);
    }

    private static boolean isMarker(String marker, String target) {
        return marker.equals(target)
                || marker.equals("polluted_world:" + target);
    }

    private static String getMarkerName(StructureTemplate.StructureBlockInfo info) {
        if (info.nbt() == null) {
            return "";
        }

        return info.nbt().getString("name").orElse("");
    }
}