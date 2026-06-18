package com.licht_meilleur.polluted_world.world.spawn;

import com.licht_meilleur.polluted_world.world.StructureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                popcornSpawn(level, info.pos(), List.of(
                        EntityType.ZOMBIE,
                        EntityType.SKELETON,
                        EntityType.SPIDER
                ), 6);
            } else if (isMarker(marker, "spawn_aquatic")) {
                popcornSpawn(level, info.pos(), List.of(
                        EntityType.DROWNED
                ), 4);
            } else if (isMarker(marker, "spawn_rare")) {
                popcornSpawn(level, info.pos(), List.of(
                        EntityType.WITCH,
                        EntityType.ZOMBIE_VILLAGER
                ), 3);
            }
        }
    }

    private static void popcornSpawn(
            ServerLevel level,
            BlockPos center,
            List<EntityType<?>> types,
            int count
    ) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < count; i++) {
            EntityType<?> type = types.get(random.nextInt(types.size()));

            BlockPos pos = center.offset(
                    random.nextInt(7) - 3,
                    0,
                    random.nextInt(7) - 3
            );

            BlockPos spawnPos = findSpawnPos(level, pos);



            spawn(level, spawnPos, type);
        }
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos pos) {
        for (int dy = -2; dy <= 4; dy++) {
            BlockPos check = pos.above(dy);

            if (level.getBlockState(check.below()).isSolid()
                    && level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                return check;
            }
        }

        return pos.above();
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
        if (entity == null) {

            return;
        }

        BlockPos spawnPos = pos;

        if (!level.getBlockState(spawnPos).isAir()) {
            spawnPos = spawnPos.above();
        }

        entity.snapTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F,
                0.0F
        );

        entity.addTag(NO_SUN_BURN_TAG);

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    EntitySpawnReason.STRUCTURE,
                    null
            );
        }

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();

            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    EntitySpawnReason.STRUCTURE,
                    null
            );

            equipSkeletonBow(mob);
        }

        boolean added = level.addFreshEntity(entity);



    }

    private static void equipSkeletonBow(Mob mob) {
        if (mob instanceof AbstractSkeleton) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }

    private static boolean isMarker(String marker, String target) {
        return marker.equals(target)
                || marker.equals("polluted_world:" + target);
    }

    private static String getMarkerName(StructureTemplate.StructureBlockInfo info) {
        if (info.nbt() == null) {
            return "";
        }

        String name = info.nbt().getString("name").orElse("");
        if (!name.isEmpty()) {
            return name;
        }

        return info.nbt().getString("target").orElse("");
    }
}