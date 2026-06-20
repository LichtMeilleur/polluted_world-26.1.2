package com.licht_meilleur.polluted_world.world.loot;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.compat.JustEnoughGunsLootCompat;
import com.licht_meilleur.polluted_world.world.StructureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Set;

public final class PollutedLootMarkerProcessor {
    private static final Set<String> LOOT_MARKERS = Set.of(
            "start_supply",
            "food",
            "civilian",
            "industrial",
            "maintenance",
            "medical",
            "military",
            "research",
            "rare",
            "legendary"
    );

    private PollutedLootMarkerProcessor() {
    }

    public static void process(ServerLevel level, StructureNode node) {
        for (StructureTemplate.StructureBlockInfo info : node.jigsaws()) {
            String path = normalizeMarker(getMarkerName(info));

            if (!LOOT_MARKERS.contains(path)) {
                continue;
            }

            BlockPos containerPos = findNearestContainer(level, info.pos(), 3);
            if (containerPos == null) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(containerPos);
            if (be instanceof RandomizableContainerBlockEntity container) {
                ResourceKey<LootTable> key = ResourceKey.create(
                        Registries.LOOT_TABLE,
                        PollutedWorldMod.id(path)
                );

                container.setLootTable(key, level.getRandom().nextLong());

                // JEG連携対象だけ、ルートテーブル展開後にボーナス追加
                if (path.equals("start_supply") || path.equals("military") || path.equals("rare") || path.equals("legendary")) {
                    container.unpackLootTable(null);
                    JustEnoughGunsLootCompat.addBonusLoot(
                            container,
                            level.getRandom(),
                            categoryFor(path)
                    );
                }

                container.setChanged();
            }
        }
    }

    private static JustEnoughGunsLootCompat.Category categoryFor(String path) {
        return switch (path) {
            case "start_supply" -> JustEnoughGunsLootCompat.Category.START_SUPPLY;
            case "rare" -> JustEnoughGunsLootCompat.Category.RARE;
            case "legendary" -> JustEnoughGunsLootCompat.Category.LEGENDARY;
            default -> JustEnoughGunsLootCompat.Category.MILITARY;
        };
    }

    private static BlockPos findNearestContainer(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius)
        )) {
            BlockEntity be = level.getBlockEntity(pos);

            if (!(be instanceof RandomizableContainerBlockEntity)) {
                continue;
            }

            double distance = pos.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }

        return best;
    }

    private static String normalizeMarker(String marker) {
        if (marker.startsWith("pw:")) {
            return marker.substring("pw:".length());
        }

        if (marker.startsWith("polluted_world:")) {
            return marker.substring("polluted_world:".length());
        }

        return marker;
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