package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MiddleRailNetworkBuilder {

    private static final int NORMAL_RAILS_NEAR_STATION = 3;
    private static final int SPACER_LENGTH = 20;
    private static final int BASE_SEGMENT_LENGTH = 90;
    private static final int SAFETY_MARGIN = 24;

    private static final List<String> ENTRANCE_DECK = new ArrayList<>();
    private static final List<String> VILLAGE_DECK = new ArrayList<>();

    private record StationPair(String entrance, String village) {
    }

    private record RailPlan(String structureName, StructureTemplate template, int estimatedLength) {
    }

    private record MiddleConnectResult(StructureNode nextVillage, int railCount) {
    }

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        resetDecks();

        StructureTemplate entrance01 = load(level, "station_entrance_01");
        StructureTemplate village01 = load(level, "station_village_01");

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");

        List<StructureNode> nodes = new ArrayList<>();

        StructureNode currentEntrance = placeRoot(
                level,
                "station_entrance_01",
                entrance01,
                origin,
                Rotation.NONE
        );
        nodes.add(currentEntrance);

        StructureNode currentVillage = placeConnectedAbsolute(
                level,
                "station_village_01",
                village01,
                "polluted_world:entrance",
                currentEntrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(currentVillage);

        boolean teleported = teleportToFirstBarrier(player, currentVillage, currentEntrance);

        int totalRailCount = 0;

        while (hasNextStationPair()) {
            StationPair pair = drawPair(level);

            RailPlan westPlan = drawRailPlan(level);
            RailPlan eastPlan = drawRailPlan(level);

            int estimatedDistance = BASE_SEGMENT_LENGTH
                    + Math.max(westPlan.estimatedLength(), eastPlan.estimatedLength())
                    + SAFETY_MARGIN;

            System.out.println("[PollutedWorld] MiddleRail plan west="
                    + westPlan.structureName()
                    + " east=" + eastPlan.structureName()
                    + " distance=" + estimatedDistance);

            MiddleConnectResult result = connectNextStation(
                    level,
                    nodes,
                    currentVillage,
                    pair,
                    railGateTemplate,
                    normalRailTemplate,
                    westPlan,
                    eastPlan,
                    estimatedDistance
            );

            currentVillage = result.nextVillage();
            totalRailCount += result.railCount();
        }

        int barrierCount = nodes.stream()
                .mapToInt(StructureNode::barrierCount)
                .sum();

        for (StructureNode node : nodes) {
            node.removeMarkers(level);
        }

        return new PollutedStructurePlacer.NetworkResult(
                totalRailCount,
                barrierCount,
                teleported
        );
    }

    private static MiddleConnectResult connectNextStation(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode currentVillage,
            StationPair pair,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate,
            RailPlan westPlan,
            RailPlan eastPlan,
            int segmentDistance
    ) {
        StructureTemplate nextEntranceTemplate = load(level, pair.entrance());
        StructureTemplate nextVillageTemplate = load(level, pair.village());

        // west start gate
        StructureNode westStartGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                currentVillage.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(westStartGate);

        Direction mainForward = directionFromTo(
                westStartGate.marker("polluted_world:rail_gate"),
                westStartGate.marker("polluted_world:rail")
        );

        BlockPos provisionalWestEnd = westStartGate
                .marker("polluted_world:rail")
                .relative(mainForward, segmentDistance);

        // west end gate first, then station B
        StructureNode westEndGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail",
                provisionalWestEnd,
                Rotation.NONE
        );
        nodes.add(westEndGate);

        StructureNode nextVillage = placeConnectedAbsolute(
                level,
                pair.village(),
                nextVillageTemplate,
                "polluted_world:west_down",
                westEndGate.marker("polluted_world:rail_gate"),
                Rotation.NONE
        );
        nodes.add(nextVillage);

        StructureNode nextEntrance = placeConnectedAbsolute(
                level,
                pair.entrance(),
                nextEntranceTemplate,
                "polluted_world:entrance",
                nextVillage.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(nextEntrance);

        // west lane
        connectLaneWithArms(
                level,
                nodes,
                currentVillage.marker("polluted_world:west_up"),
                nextVillage.marker("polluted_world:west_down"),
                Rotation.CLOCKWISE_180,
                Rotation.NONE,
                railGateTemplate,
                normalRailTemplate,
                westPlan,
                true
        );

        // east lane
        connectLaneWithArms(
                level,
                nodes,
                currentVillage.marker("polluted_world:east_up"),
                nextVillage.marker("polluted_world:east_down"),
                Rotation.CLOCKWISE_180,
                Rotation.NONE,
                railGateTemplate,
                normalRailTemplate,
                eastPlan,
                true
        );

        return new MiddleConnectResult(nextVillage, 2);
    }

    private static void connectLaneWithArms(
            ServerLevel level,
            List<StructureNode> nodes,
            BlockPos startMarker,
            BlockPos endMarker,
            Rotation startRotation,
            Rotation endRotation,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate,
            RailPlan railPlan,
            boolean allowEventRail
    ) {
        StructureNode startGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                startMarker,
                startRotation
        );
        nodes.add(startGate);

        StructureNode endGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                endMarker,
                endRotation
        );
        nodes.add(endGate);

        BlockPos startTip = buildRailArm(
                level,
                nodes,
                startGate.marker("polluted_world:rail"),
                startRotation,
                normalRailTemplate,
                railPlan,
                allowEventRail
        );

        BlockPos endTip = buildRailArm(
                level,
                nodes,
                endGate.marker("polluted_world:rail"),
                endRotation,
                normalRailTemplate,
                railPlan,
                false
        );

        CodeRailTunnelBuilder.generateTunnel(
                level,
                startTip.below(),
                endTip.below()
        );
    }

    private static BlockPos buildRailArm(
            ServerLevel level,
            List<StructureNode> nodes,
            BlockPos startConnect,
            Rotation rotation,
            StructureTemplate normalRailTemplate,
            RailPlan railPlan,
            boolean placeEventRail
    ) {
        BlockPos nextConnect = startConnect;
        StructureNode last = null;

        for (int i = 0; i < NORMAL_RAILS_NEAR_STATION; i++) {
            last = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(last);

            nextConnect = last.marker("polluted_world:rail_out");
        }

        Direction forward = directionFromTo(
                last.marker("polluted_world:rail_in"),
                last.marker("polluted_world:rail_out")
        );

        BlockPos spacerEnd = CodeRailTunnelBuilder.generateForwardSpacer(
                level,
                nextConnect.below(),
                forward,
                SPACER_LENGTH
        );

        nextConnect = spacerEnd.above();

        if (placeEventRail) {
            StructureNode eventRail = placeConnectedAbsolute(
                    level,
                    railPlan.structureName(),
                    railPlan.template(),
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(eventRail);

            nextConnect = eventRail.marker("polluted_world:rail_out");

            forward = directionFromTo(
                    eventRail.marker("polluted_world:rail_in"),
                    eventRail.marker("polluted_world:rail_out")
            );

            spacerEnd = CodeRailTunnelBuilder.generateForwardSpacer(
                    level,
                    nextConnect.below(),
                    forward,
                    SPACER_LENGTH
            );

            nextConnect = spacerEnd.above();
        }

        for (int i = 0; i < NORMAL_RAILS_NEAR_STATION; i++) {
            last = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    nextConnect,
                    rotation
            );
            nodes.add(last);

            nextConnect = last.marker("polluted_world:rail_out");
        }

        return nextConnect;
    }

    private static RailPlan drawRailPlan(ServerLevel level) {
        List<String> candidates = new ArrayList<>();

        candidates.add("rail_with_side_street");

        if (exists(level, "collapse_rail")) {
            candidates.add("collapse_rail");
        }

        String name = candidates.get(level.getRandom().nextInt(candidates.size()));
        StructureTemplate template = load(level, name);

        int estimatedLength = estimateTemplateFootprint(template);

        return new RailPlan(name, template, estimatedLength);
    }

    private static int estimateTemplateFootprint(StructureTemplate template) {
        Vec3i size = template.getSize();

        return Math.max(size.getX(), size.getZ());
    }

    private static boolean exists(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);
        return level.getStructureManager().get(id).isPresent();
    }

    private static void resetDecks() {
        ENTRANCE_DECK.clear();
        VILLAGE_DECK.clear();

        ENTRANCE_DECK.add("station_entrance_02");
        ENTRANCE_DECK.add("station_entrance_03");

        VILLAGE_DECK.add("station_village_02");
        VILLAGE_DECK.add("station_village_03");
    }

    private static boolean hasNextStationPair() {
        return !ENTRANCE_DECK.isEmpty() && !VILLAGE_DECK.isEmpty();
    }

    private static StationPair drawPair(ServerLevel level) {
        String entrance = drawRandom(level, ENTRANCE_DECK);
        String village = drawRandom(level, VILLAGE_DECK);

        System.out.println("[PollutedWorld] MiddleRail draw station="
                + entrance + " + " + village);

        return new StationPair(entrance, village);
    }

    private static String drawRandom(ServerLevel level, List<String> deck) {
        int index = level.getRandom().nextInt(deck.size());
        return deck.remove(index);
    }

    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> template = level.getStructureManager().get(id);

        if (template.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return template.get();
    }

    private static StructureNode placeRoot(
            ServerLevel level,
            String structureName,
            StructureTemplate template,
            BlockPos origin,
            Rotation rotation
    ) {
        return placeAt(level, structureName, template, origin, rotation);
    }

    private static StructureNode placeConnectedAbsolute(
            ServerLevel level,
            String childName,
            StructureTemplate childTemplate,
            String childMarkerName,
            BlockPos connectToWorldPos,
            Rotation absoluteRotation
    ) {
        StructurePlaceSettings settings = settings(absoluteRotation);

        List<StructureTemplate.StructureBlockInfo> localMarkers =
                getSortedMarkers(childTemplate, BlockPos.ZERO, settings, Blocks.JIGSAW);

        BlockPos localMarkerPos = localMarkers.stream()
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

    private static Direction directionFromTo(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());

        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        if (dz < 0) return Direction.NORTH;

        return Direction.NORTH;
    }
}