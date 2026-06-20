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

public class StationUnitTestBuilder {

    private static final int NORMAL_RAILS = 3;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureTemplate entranceTemplate = load(level, "station_entrance_01");
        StructureTemplate villageTemplate = load(level, "station_village_01");
        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");

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

        // west_up 側：駅から外へ出る側
        StructureNode westUpGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                village.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(westUpGate);

        BlockPos westUpConnect = westUpGate.marker("polluted_world:rail");

        for (int i = 0; i < NORMAL_RAILS; i++) {
            StructureNode rail = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    westUpConnect,
                    Rotation.CLOCKWISE_180
            );
            nodes.add(rail);

            westUpConnect = rail.marker("polluted_world:rail_out");
        }

        // west_down 側：駅へ入ってくる側
        StructureNode westDownGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                village.marker("polluted_world:west_down"),
                Rotation.NONE
        );
        nodes.add(westDownGate);



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

        BlockPos childOrigin = new BlockPos(
                connectToWorldPos.getX() - localMarkerPos.getX(),
                connectToWorldPos.getY() - localMarkerPos.getY(),
                connectToWorldPos.getZ() - localMarkerPos.getZ()
        );

        return placeAt(level, structureName, template, childOrigin, rotation);
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