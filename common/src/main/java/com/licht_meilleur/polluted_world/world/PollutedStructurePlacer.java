package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PollutedStructurePlacer {

    public record PlaceResult(int jigsawMarkers, int barrierMarkers) {
    }

    public record StartResult(int entranceJigsaws, int villageJigsaws, int barrierMarkers, boolean teleported) {
    }

    private record LoadedStructure(StructureTemplate template, StructurePlaceSettings settings) {
    }

    public static PlaceResult placeAndScan(ServerLevel level, String structureName, BlockPos origin) {
        LoadedStructure loaded = load(level, structureName);

        List<StructureTemplate.StructureBlockInfo> jigsaws =
                getSortedMarkers(loaded.template(), origin, loaded.settings(), Blocks.JIGSAW);

        boolean placed = loaded.template().placeInWorld(
                level,
                origin,
                origin,
                loaded.settings(),
                level.getRandom(),
                Block.UPDATE_ALL
        );

        if (!placed) {
            throw new IllegalStateException("Failed to place structure: " + structureName);
        }

        MarkerCleanupResult cleanup = scanAndRemoveMarkers(level, origin, loaded.template().getSize());

        return new PlaceResult(jigsaws.size(), cleanup.barrierCount());
    }

    public static StartResult placeEntranceAndVillage(ServerLevel level, ServerPlayer player, BlockPos entranceOrigin) {
        LoadedStructure entrance = load(level, "station_entrance_01");
        LoadedStructure village = load(level, "station_village_01");

        List<StructureTemplate.StructureBlockInfo> entranceJigsaws =
                getSortedMarkers(entrance.template(), entranceOrigin, entrance.settings(), Blocks.JIGSAW);

        if (entranceJigsaws.isEmpty()) {
            throw new IllegalStateException("station_entrance_01 has no Jigsaw marker.");
        }

        boolean entrancePlaced = entrance.template().placeInWorld(
                level,
                entranceOrigin,
                entranceOrigin,
                entrance.settings(),
                level.getRandom(),
                Block.UPDATE_ALL
        );

        if (!entrancePlaced) {
            throw new IllegalStateException("Failed to place station_entrance_01");
        }

        BlockPos entranceConnectionPos = entranceJigsaws.get(0).pos();

        List<StructureTemplate.StructureBlockInfo> villageLocalJigsaws =
                getSortedMarkers(village.template(), BlockPos.ZERO, village.settings(), Blocks.JIGSAW);

        if (villageLocalJigsaws.isEmpty()) {
            throw new IllegalStateException("station_village_01 has no Jigsaw marker.");
        }

        BlockPos villageConnectionLocalPos = villageLocalJigsaws.get(0).pos();

        BlockPos villageOrigin = new BlockPos(
                entranceConnectionPos.getX() - villageConnectionLocalPos.getX(),
                entranceConnectionPos.getY() - villageConnectionLocalPos.getY(),
                entranceConnectionPos.getZ() - villageConnectionLocalPos.getZ()
        );

        List<StructureTemplate.StructureBlockInfo> villageJigsaws =
                getSortedMarkers(village.template(), villageOrigin, village.settings(), Blocks.JIGSAW);

        boolean villagePlaced = village.template().placeInWorld(
                level,
                villageOrigin,
                villageOrigin,
                village.settings(),
                level.getRandom(),
                Block.UPDATE_ALL
        );

        if (!villagePlaced) {
            throw new IllegalStateException("Failed to place station_village_01");
        }

        MarkerCleanupResult entranceCleanup =
                scanAndRemoveMarkers(level, entranceOrigin, entrance.template().getSize());

        MarkerCleanupResult villageCleanup =
                scanAndRemoveMarkers(level, villageOrigin, village.template().getSize());

        Optional<BlockPos> spawnPos = villageCleanup.firstBarrierPos().or(entranceCleanup::firstBarrierPos);

        boolean teleported = false;

        if (spawnPos.isPresent()) {
            BlockPos pos = spawnPos.get();
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            teleported = true;
        }

        return new StartResult(
                entranceJigsaws.size(),
                villageJigsaws.size(),
                entranceCleanup.barrierCount() + villageCleanup.barrierCount(),
                teleported
        );
    }

    private static LoadedStructure load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return new LoadedStructure(optionalTemplate.get(), new StructurePlaceSettings());
    }

    private static List<StructureTemplate.StructureBlockInfo> getSortedMarkers(
            StructureTemplate template,
            BlockPos origin,
            StructurePlaceSettings settings,
            net.minecraft.world.level.block.Block markerBlock
    ) {
        return template.filterBlocks(origin, settings, markerBlock)
                .stream()
                .sorted(Comparator
                        .comparingInt((StructureTemplate.StructureBlockInfo info) -> info.pos().getY())
                        .thenComparingInt(info -> info.pos().getZ())
                        .thenComparingInt(info -> info.pos().getX()))
                .toList();
    }

    private record MarkerCleanupResult(int barrierCount, Optional<BlockPos> firstBarrierPos) {
    }

    private static MarkerCleanupResult scanAndRemoveMarkers(ServerLevel level, BlockPos origin, Vec3i size) {
        int barrierCount = 0;
        BlockPos firstBarrierPos = null;

        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);

                    if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                        barrierCount++;

                        if (firstBarrierPos == null) {
                            firstBarrierPos = pos.immutable();
                        }

                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }

                    if (level.getBlockState(pos).is(Blocks.JIGSAW)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        return new MarkerCleanupResult(barrierCount, Optional.ofNullable(firstBarrierPos));
    }
}