package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LayoutTestNetworkBuilder {

    private static final int NORMAL_BEFORE_EVENT = 3;
    private static final int NORMAL_AFTER_EVENT = 3;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureTemplate entranceTemplate = load(level, "station_entrance_01");
        StructureTemplate villageTemplate = load(level, "station_village_01");

        StructureTemplate nextEntranceTemplate = load(level, "station_entrance_02");
        StructureTemplate nextVillageTemplate = load(level, "station_village_02");

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");
        StructureTemplate sideRailTemplate = load(level, "rail_with_side_street");

        StructureNode entrance = placeAt(
                level,
                "station_entrance_01",
                entranceTemplate,
                origin,
                Rotation.NONE
        );
        nodes.add(entrance);

        StructureNode village = placeConnectedAbsolute(
                level,
                "station_village_01",
                villageTemplate,
                "polluted_world:entrance",
                entrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(village);

        boolean teleported = teleportToFirstBarrier(player, village, entrance);

        // west 側を先に生成して、駅B位置を決める
        StructureNode westEndGate = buildLaneFromStart(
                level,
                nodes,
                village.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180,
                railGateTemplate,
                normalRailTemplate,
                sideRailTemplate,
                true
        );

        StructureNode nextVillage = placeConnectedAbsolute(
                level,
                "station_village_02",
                nextVillageTemplate,
                "polluted_world:west_down",
                westEndGate.marker("polluted_world:rail_gate"),
                Rotation.NONE
        );
        nodes.add(nextVillage);

        StructureNode nextEntrance = placeConnectedAbsolute(
                level,
                "station_entrance_02",
                nextEntranceTemplate,
                "polluted_world:entrance",
                nextVillage.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(nextEntrance);

        // east 側も同じ構成で生成する
        buildLaneFromStart(
                level,
                nodes,
                village.marker("polluted_world:east_up"),
                Rotation.CLOCKWISE_180,
                railGateTemplate,
                normalRailTemplate,
                sideRailTemplate,
                true
        );

        int barrierCount = nodes.stream()
                .mapToInt(StructureNode::barrierCount)
                .sum();

        for (StructureNode node : nodes) {
            node.removeMarkers(level);
        }

        return new PollutedStructurePlacer.NetworkResult(
                nodes.size(),
                barrierCount,
                teleported
        );
    }

    private static StructureNode buildLaneFromStart(
            ServerLevel level,
            List<StructureNode> nodes,
            BlockPos stationMarker,
            Rotation rotation,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate,
            StructureTemplate sideRailTemplate,
            boolean placeSideDungeons
    ) {
        StructureNode startGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                stationMarker,
                rotation
        );
        nodes.add(startGate);

        BlockPos nextConnect = startGate.marker("polluted_world:rail");

        for (int i = 0; i < NORMAL_BEFORE_EVENT; i++) {
            StructureNode normal = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(normal);

            nextConnect = normal.marker("polluted_world:rail_out");
        }

        StructureNode sideRail = placeConnectedAbsolute(
                level,
                "rail_with_side_street",
                sideRailTemplate,
                "polluted_world:rail_in",
                nextConnect,
                rotation
        );
        nodes.add(sideRail);

        if (placeSideDungeons) {
            placeSideDungeons(level, nodes, sideRail);
        }

        nextConnect = sideRail.marker("polluted_world:rail_out");

        for (int i = 0; i < NORMAL_AFTER_EVENT; i++) {
            StructureNode normal = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(normal);

            nextConnect = normal.marker("polluted_world:rail_out");
        }

        StructureNode endGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail",
                nextConnect,
                Rotation.NONE
        );
        nodes.add(endGate);

        return endGate;
    }

    private static void placeSideDungeons(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode railNode
    ) {
        if (railNode.hasMarker("polluted_world:west_side_street")) {
            StructureTemplate westTemplate = load(level, "west_side_dungeon_01");

            StructureNode westDungeon = placeConnectedAbsolute(
                    level,
                    "west_side_dungeon_01",
                    westTemplate,
                    "polluted_world:west_side_street",
                    railNode.marker("polluted_world:west_side_street"),
                    railNode.rotation()
            );

            nodes.add(westDungeon);
        }

        if (railNode.hasMarker("polluted_world:east_side_street")) {
            StructureTemplate eastTemplate = load(level, "east_side_dungeon_01");

            StructureNode eastDungeon = placeConnectedAbsolute(
                    level,
                    "east_side_dungeon_01",
                    eastTemplate,
                    "polluted_world:east_side_street",
                    railNode.marker("polluted_world:east_side_street"),
                    railNode.rotation()
            );

            nodes.add(eastDungeon);
        }
    }

    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return optionalTemplate.get();
    }

    private static StructureNode placeConnectedAbsolute(
            ServerLevel level,
            String childName,
            StructureTemplate childTemplate,
            String childMarkerName,
            BlockPos connectToWorldPos,
            Rotation absoluteRotation
    ) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(absoluteRotation);

        BlockPos localMarkerPos = childTemplate
                .filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW)
                .stream()
                .filter(info -> childMarkerName.equals(getJigsawName(info)))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Local marker not found: " + childMarkerName));

        BlockPos childOrigin = new BlockPos(
                connectToWorldPos.getX() - localMarkerPos.getX(),
                connectToWorldPos.getY() - localMarkerPos.getY(),
                connectToWorldPos.getZ() - localMarkerPos.getZ()
        );

        return placeAt(level, childName, childTemplate, childOrigin, absoluteRotation);
    }

    private static StructureNode placeAt(
            ServerLevel level,
            String structureName,
            StructureTemplate template,
            BlockPos origin,
            Rotation rotation
    ) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);

        List<StructureTemplate.StructureBlockInfo> jigsaws =
                getSortedMarkers(template, origin, settings, Blocks.JIGSAW);

        List<StructureTemplate.StructureBlockInfo> barriers =
                getSortedMarkers(template, origin, settings, Blocks.BARRIER);

        boolean placed = template.placeInWorld(
                level,
                origin,
                origin,
                settings,
                level.getRandom(),
                Block.UPDATE_ALL
        );

        if (!placed) {
            throw new IllegalStateException("Failed to place structure: " + structureName);
        }

        return new StructureNode(
                structureName,
                template,
                origin,
                rotation,
                template.getSize(),
                jigsaws,
                barriers
        );
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

    private static String getJigsawName(StructureTemplate.StructureBlockInfo info) {
        if (info.nbt() == null) {
            return "";
        }

        return info.nbt().getString("name").orElse("");
    }

    private static boolean teleportToFirstBarrier(ServerPlayer player, StructureNode first, StructureNode second) {
        Optional<BlockPos> spawnPos = first.firstBarrierPos().or(second::firstBarrierPos);

        if (spawnPos.isEmpty()) {
            return false;
        }

        BlockPos pos = spawnPos.get();
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return true;
    }
}