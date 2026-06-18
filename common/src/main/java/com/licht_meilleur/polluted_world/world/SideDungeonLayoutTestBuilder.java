package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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

public class SideDungeonLayoutTestBuilder {

    private static final int MIN_LANE_GAP = 16;
    private static final int MAX_LANE_GAP = 160;
    private static final int GAP_STEP = 4;
    private static final int COLLISION_MARGIN = 4;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureTemplate sideRailTemplate = load(level, "rail_with_side_street");
        StructureTemplate collapseRailTemplate = load(level, "collapse_rail");
        StructureTemplate westDungeonTemplate = load(level, "west_side_dungeon_01");
        StructureTemplate eastDungeonTemplate = load(level, "east_side_dungeon_01");


        RailChoice firstRail = randomRailChoice(level);
        RailChoice secondRail = randomRailChoice(level);

        // 1本目
        List<StructureNode> firstCluster = placeSideRailCluster(
                level,
                origin,
                Rotation.NONE,
                firstRail,
                westDungeonTemplate,
                eastDungeonTemplate
        );
        nodes.addAll(firstCluster);

        // 2本目は east 方向へずらしながら、干渉しない距離を探す
        BlockPos secondOrigin = null;

        for (int gap = MIN_LANE_GAP; gap <= MAX_LANE_GAP; gap += GAP_STEP) {
            BlockPos candidate = origin.east(gap);

            List<StructureNode> planned = planSideRailCluster(
                    secondRail,
                    westDungeonTemplate,
                    eastDungeonTemplate,
                    candidate,
                    Rotation.NONE
            );

            boolean collides = false;

            for (StructureNode existing : nodes) {
                for (StructureNode plan : planned) {
                    if (existing.intersects(plan.origin(), plan.size(), COLLISION_MARGIN)) {
                        collides = true;
                        break;
                    }
                }

                if (collides) {
                    break;
                }
            }

            if (!collides) {
                secondOrigin = candidate;

                break;
            }
        }

        if (secondOrigin == null) {
            throw new IllegalStateException("No valid side rail gap found.");
        }

        List<StructureNode> secondCluster = placeSideRailCluster(
                level,
                secondOrigin,
                Rotation.NONE,
                secondRail,
                westDungeonTemplate,
                eastDungeonTemplate
        );

        nodes.addAll(secondCluster);

        int barrierCount = nodes.stream()
                .mapToInt(StructureNode::barrierCount)
                .sum();

        for (StructureNode node : nodes) {
            node.removeMarkers(level);
        }

        return new PollutedStructurePlacer.NetworkResult(
                nodes.size(),
                barrierCount,
                false
        );
    }

    private static RailChoice randomRailChoice(ServerLevel level) {
        boolean useCollapse = level.getRandom().nextBoolean();

        if (useCollapse && exists(level, "collapse_rail")) {
            return new RailChoice("collapse_rail", load(level, "collapse_rail"), false);
        }

        return new RailChoice("rail_with_side_street", load(level, "rail_with_side_street"), true);
    }

    private record RailChoice(
            String name,
            StructureTemplate template,
            boolean hasSideDungeon
    ) {
    }

    private static boolean exists(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);
        return level.getStructureManager().get(id).isPresent();
    }

    private static List<StructureNode> placeSideRailCluster(
            ServerLevel level,
            BlockPos railInWorldPos,
            Rotation rotation,
            RailChoice railChoice,
            StructureTemplate westDungeonTemplate,
            StructureTemplate eastDungeonTemplate
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureNode rail = placeConnectedAbsolute(
                level,
                railChoice.name(),
                railChoice.template(),
                "polluted_world:rail_in",
                railInWorldPos,
                rotation
        );
        nodes.add(rail);

            if (rail.hasMarker("polluted_world:west_side_street")) {
                StructureNode westDungeon = placeConnectedAbsolute(
                        level,
                        "west_side_dungeon_01",
                        westDungeonTemplate,
                        "polluted_world:west_side_street",
                        rail.marker("polluted_world:west_side_street"),
                        rotation
                );
                nodes.add(westDungeon);
            }

            if (rail.hasMarker("polluted_world:east_side_street")) {
                StructureNode eastDungeon = placeConnectedAbsolute(
                        level,
                        "east_side_dungeon_01",
                        eastDungeonTemplate,
                        "polluted_world:east_side_street",
                        rail.marker("polluted_world:east_side_street"),
                        rotation
                );
                nodes.add(eastDungeon);
            }

        

        return nodes;
    }

    private static List<StructureNode> planSideRailCluster(
            RailChoice railChoice,
            StructureTemplate westDungeonTemplate,
            StructureTemplate eastDungeonTemplate,
            BlockPos railInWorldPos,
            Rotation rotation
    ) {
        List<StructureNode> nodes = new ArrayList<>();

        StructureNode rail = virtualConnectedNode(
                railChoice.name(),
                railChoice.template(),
                "polluted_world:rail_in",
                railInWorldPos,
                rotation
        );
        nodes.add(rail);
        if (railChoice.hasSideDungeon()) {
            if (rail.hasMarker("polluted_world:west_side_street")) {
                StructureNode westDungeon = virtualConnectedNode(
                        "west_side_dungeon_01",
                        westDungeonTemplate,
                        "polluted_world:west_side_street",
                        rail.marker("polluted_world:west_side_street"),
                        rotation
                );
                nodes.add(westDungeon);
            }

            if (rail.hasMarker("polluted_world:east_side_street")) {
                StructureNode eastDungeon = virtualConnectedNode(
                        "east_side_dungeon_01",
                        eastDungeonTemplate,
                        "polluted_world:east_side_street",
                        rail.marker("polluted_world:east_side_street"),
                        rotation
                );
                nodes.add(eastDungeon);
            }

        }

        return nodes;
    }

    private static StructureNode virtualConnectedNode(
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

        List<StructureTemplate.StructureBlockInfo> jigsaws =
                getSortedMarkers(template, origin, settings, Blocks.JIGSAW);

        List<StructureTemplate.StructureBlockInfo> barriers =
                getSortedMarkers(template, origin, settings, Blocks.BARRIER);

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

    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return optionalTemplate.get();
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
}