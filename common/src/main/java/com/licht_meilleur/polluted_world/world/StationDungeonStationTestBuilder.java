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

public class StationDungeonStationTestBuilder {

    private static final int NORMAL_RAILS_FROM_STATION = 3;
    private static final int GAP_NORMAL_RAILS = 6;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureTemplate entranceA = load(level, "station_entrance_01");
        StructureTemplate villageA = load(level, "station_village_01");
        StructureTemplate entranceB = load(level, "station_entrance_02");
        StructureTemplate villageB = load(level, "station_village_02");

        StructureTemplate railGate = load(level, "rail_gate");
        StructureTemplate normalRail = load(level, "normal_rail");
        StructureTemplate sideRail = load(level, "rail_with_side_street");

        StructureNode startEntrance = placeAt(level, "station_entrance_01", entranceA, origin, Rotation.NONE);
        nodes.add(startEntrance);

        StructureNode startVillage = placeConnectedAbsolute(
                level,
                "station_village_01",
                villageA,
                "polluted_world:entrance",
                startEntrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(startVillage);

        boolean teleported = teleportToFirstBarrier(player, startVillage, startEntrance);

        // west_up から出発
        StructureNode startGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGate,
                "polluted_world:rail_gate",
                startVillage.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(startGate);

        BlockPos nextConnect = startGate.marker("polluted_world:rail");

        // 駅A側 normal × 3
        nextConnect = placeNormalRails(
                level,
                nodes,
                normalRail,
                nextConnect,
                Rotation.CLOCKWISE_180,
                NORMAL_RAILS_FROM_STATION
        );

        // 間隔
        nextConnect = placeNormalRails(
                level,
                nodes,
                normalRail,
                nextConnect,
                Rotation.CLOCKWISE_180,
                GAP_NORMAL_RAILS
        );

        // ダンジョンレール
        StructureNode dungeonRail = placeConnectedAbsolute(
                level,
                "rail_with_side_street",
                sideRail,
                "polluted_world:rail_in",
                nextConnect,
                Rotation.CLOCKWISE_180
        );
        nodes.add(dungeonRail);

        placeSideDungeons(level, nodes, dungeonRail);

        nextConnect = dungeonRail.marker("polluted_world:rail_out");

        // 間隔
        nextConnect = placeNormalRails(
                level,
                nodes,
                normalRail,
                nextConnect,
                Rotation.CLOCKWISE_180,
                GAP_NORMAL_RAILS
        );

        // 駅B側 normal × 3
        nextConnect = placeNormalRails(
                level,
                nodes,
                normalRail,
                nextConnect,
                Rotation.CLOCKWISE_180,
                NORMAL_RAILS_FROM_STATION
        );

        // 終点ゲート
        StructureNode endGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGate,
                "polluted_world:rail",
                nextConnect,
                Rotation.NONE
        );
        nodes.add(endGate);

        // 駅B配置
        StructureNode endVillage = placeConnectedAbsolute(
                level,
                "station_village_02",
                villageB,
                "polluted_world:west_down",
                endGate.marker("polluted_world:rail_gate"),
                Rotation.NONE
        );
        nodes.add(endVillage);

        StructureNode endEntrance = placeConnectedAbsolute(
                level,
                "station_entrance_02",
                entranceB,
                "polluted_world:entrance",
                endVillage.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(endEntrance);

        int barrierCount = nodes.stream()
                .mapToInt(StructureNode::barrierCount)
                .sum();

        for (StructureNode node : nodes) {
            node.removeMarkers(level);
        }

        return new PollutedStructurePlacer.NetworkResult(nodes.size(), barrierCount, teleported);
    }

    private static BlockPos placeNormalRails(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureTemplate normalRail,
            BlockPos nextConnect,
            Rotation rotation,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            StructureNode rail = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRail,
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(rail);
            nextConnect = rail.marker("polluted_world:rail_out");
        }

        return nextConnect;
    }

    private static void placeSideDungeons(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode railNode
    ) {
        if (railNode.hasMarker("polluted_world:west_side_street")) {
            StructureTemplate west = load(level, "west_side_dungeon_01");

            StructureNode dungeon = placeConnectedAbsolute(
                    level,
                    "west_side_dungeon_01",
                    west,
                    "polluted_world:west_side_street",
                    railNode.marker("polluted_world:west_side_street"),
                    railNode.rotation()
            );
            nodes.add(dungeon);
        }

        if (railNode.hasMarker("polluted_world:east_side_street")) {
            StructureTemplate east = load(level, "east_side_dungeon_01");

            StructureNode dungeon = placeConnectedAbsolute(
                    level,
                    "east_side_dungeon_01",
                    east,
                    "polluted_world:east_side_street",
                    railNode.marker("polluted_world:east_side_street"),
                    railNode.rotation()
            );
            nodes.add(dungeon);
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
            String structureName,
            StructureTemplate template,
            String markerName,
            BlockPos connectToWorldPos,
            Rotation rotation
    ) {
        StructurePlaceSettings settings = settings(rotation);

        BlockPos localMarkerPos = getSortedMarkers(template, BlockPos.ZERO, settings, Blocks.JIGSAW)
                .stream()
                .filter(info -> markerName.equals(getJigsawName(info)))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Local marker not found: " + markerName));

        BlockPos origin = new BlockPos(
                connectToWorldPos.getX() - localMarkerPos.getX(),
                connectToWorldPos.getY() - localMarkerPos.getY(),
                connectToWorldPos.getZ() - localMarkerPos.getZ()
        );

        return placeAt(level, structureName, template, origin, rotation);
    }

    private static StructureNode placeAt(
            ServerLevel level,
            String structureName,
            StructureTemplate template,
            BlockPos origin,
            Rotation rotation
    ) {
        StructurePlaceSettings settings = settings(rotation);

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

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation);
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