package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.world.definition.SideDungeonDefinition;
import com.licht_meilleur.polluted_world.world.definition.StationDefinition;
import com.licht_meilleur.polluted_world.world.layout.UnitBounds;
import com.licht_meilleur.polluted_world.world.registry.SideDungeonRegistry;
import com.licht_meilleur.polluted_world.world.registry.StationRegistry;
import com.licht_meilleur.polluted_world.world.registry.WeightedPicker;
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

public class UnitChainTestBuilder {

    private static final int STATION_MARGIN = 32;
    private static final int MIDDLE_RAIL_PAIR_MARGIN = 8;
    private static final int SEARCH_STEP_X = 16;
    private static final int SEARCH_STEP_Z = 32;
    private static final int MAX_X_ATTEMPTS = 12;
    private static final int MAX_Z_ATTEMPTS = 32;
    private static final int NORMAL_RAILS_FROM_STATION = 2;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();
        List<UnitBounds> units = new ArrayList<>();

        StationDefinition startStationDef = StationRegistry.START_STATION;
        StationDefinition secondStationDef = WeightedPicker.pick(
                level,
                StationRegistry.ADDITIONAL_STATIONS,
                StationDefinition::weight
        );

        StructureTemplate startEntranceTemplate = load(level, startStationDef.entranceName());
        StructureTemplate startVillageTemplate = load(level, startStationDef.villageName());
        StructureTemplate secondEntranceTemplate = load(level, secondStationDef.entranceName());
        StructureTemplate secondVillageTemplate = load(level, secondStationDef.villageName());

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");

        StationUnit firstStation = placeStationUnit(
                level,
                nodes,
                origin,
                startStationDef.entranceName(),
                startEntranceTemplate,
                startStationDef.villageName(),
                startVillageTemplate,
                railGateTemplate,
                normalRailTemplate
        );

        units.add(firstStation.bounds("station_a", STATION_MARGIN));

        boolean teleported = teleportToFirstBarrier(
                player,
                firstStation.village(),
                firstStation.entrance()
        );

        MiddleRailPairPlan middlePlan = randomMiddleRailPairPlan(level);

        BlockPos pairStart = new BlockPos(
                firstStation.westOutConnect().getX(),
                firstStation.westOutConnect().getY(),
                Math.max(firstStation.westOutConnect().getZ(), firstStation.eastOutConnect().getZ()) + SEARCH_STEP_Z
        );

        BlockPos pairOrigin = findSafeMiddleRailPairOrigin(
                level,
                units,
                pairStart,
                firstStation,
                middlePlan
        );

        MiddleRailPairUnit middlePair = placeMiddleRailPairUnit(
                level,
                nodes,
                pairOrigin,
                firstStation,
                middlePlan
        );

        units.add(middlePair.bounds());

        BlockPos secondStationStart = new BlockPos(
                origin.getX(),
                origin.getY(),
                Math.max(middlePair.westOutConnect().getZ(), middlePair.eastOutConnect().getZ()) + SEARCH_STEP_Z
        );

        Vec3i secondStationSize = combinedSize(secondEntranceTemplate, secondVillageTemplate);

        BlockPos secondStationOrigin = findSafeUnitOriginZOnly(
                units,
                secondStationStart,
                secondStationSize,
                STATION_MARGIN,
                SEARCH_STEP_Z,
                MAX_Z_ATTEMPTS
        );

        StationUnit secondStation = placeStationUnit(
                level,
                nodes,
                secondStationOrigin,
                secondStationDef.entranceName(),
                secondEntranceTemplate,
                secondStationDef.villageName(),
                secondVillageTemplate,
                railGateTemplate,
                normalRailTemplate
        );

        units.add(secondStation.bounds("station_b", STATION_MARGIN));

        CodeRailTunnelBuilder.generateFlatConnector(
                level,
                firstStation.westOutConnect().below(),
                middlePair.westInConnect().below()
        );

        CodeRailTunnelBuilder.generateFlatConnector(
                level,
                middlePair.westOutConnect().below(),
                secondStation.westInConnect().below()
        );

        CodeRailTunnelBuilder.generateFlatConnector(
                level,
                firstStation.eastOutConnect().below(),
                middlePair.eastInConnect().below()
        );

        CodeRailTunnelBuilder.generateFlatConnector(
                level,
                middlePair.eastOutConnect().below(),
                secondStation.eastInConnect().below()
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

    private record StationUnit(
            StructureNode entrance,
            StructureNode village,
            List<StructureNode> unitNodes,
            BlockPos westInConnect,
            BlockPos westOutConnect,
            BlockPos eastInConnect,
            BlockPos eastOutConnect
    ) {
        UnitBounds bounds(String name, int margin) {
            return UnitBounds.fromNodes(name, unitNodes, margin);
        }
    }

    private record LaneEnds(
            BlockPos inConnect,
            BlockPos outConnect
    ) {
    }

    private record RailChoice(
            String name,
            StructureTemplate template,
            boolean hasSideDungeon
    ) {
    }

    private record RailClusterPlan(
            RailChoice rail,
            SideDungeonDefinition westDungeon,
            SideDungeonDefinition eastDungeon
    ) {
    }

    private record MiddleRailPairPlan(
            RailClusterPlan west,
            RailClusterPlan east
    ) {
    }

    private record MiddleRailPairUnit(
            UnitBounds bounds,
            BlockPos westInConnect,
            BlockPos westOutConnect,
            BlockPos eastInConnect,
            BlockPos eastOutConnect
    ) {
    }

    private static StationUnit placeStationUnit(
            ServerLevel level,
            List<StructureNode> nodes,
            BlockPos origin,
            String entranceName,
            StructureTemplate entranceTemplate,
            String villageName,
            StructureTemplate villageTemplate,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate
    ) {
        List<StructureNode> unitNodes = new ArrayList<>();

        StructureNode entrance = placeAt(
                level,
                entranceName,
                entranceTemplate,
                origin,
                Rotation.NONE
        );
        nodes.add(entrance);
        unitNodes.add(entrance);

        StructureNode village = placeConnectedAbsolute(
                level,
                villageName,
                villageTemplate,
                "polluted_world:entrance",
                entrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(village);
        unitNodes.add(village);

        LaneEnds west = buildStationLaneEnds(
                level,
                nodes,
                unitNodes,
                village,
                "polluted_world:west_up",
                "polluted_world:west_down",
                railGateTemplate,
                normalRailTemplate
        );

        LaneEnds east = buildStationLaneEnds(
                level,
                nodes,
                unitNodes,
                village,
                "polluted_world:east_up",
                "polluted_world:east_down",
                railGateTemplate,
                normalRailTemplate
        );

        return new StationUnit(
                entrance,
                village,
                unitNodes,
                west.inConnect(),
                west.outConnect(),
                east.inConnect(),
                east.outConnect()
        );
    }

    private static LaneEnds buildStationLaneEnds(
            ServerLevel level,
            List<StructureNode> nodes,
            List<StructureNode> unitNodes,
            StructureNode village,
            String upMarker,
            String downMarker,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate
    ) {
        StructureNode upGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                village.marker(upMarker),
                Rotation.CLOCKWISE_180
        );
        nodes.add(upGate);
        unitNodes.add(upGate);

        BlockPos nextConnect = upGate.marker("polluted_world:rail");

        for (int i = 0; i < NORMAL_RAILS_FROM_STATION; i++) {
            StructureNode rail = placeConnectedAbsolute(
                    level,
                    "normal_rail",
                    normalRailTemplate,
                    "polluted_world:rail_in",
                    nextConnect,
                    Rotation.CLOCKWISE_180
            );
            nodes.add(rail);
            unitNodes.add(rail);

            nextConnect = rail.marker("polluted_world:rail_out");
        }

        StructureNode downGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                village.marker(downMarker),
                Rotation.NONE
        );
        nodes.add(downGate);
        unitNodes.add(downGate);

        return new LaneEnds(
                downGate.marker("polluted_world:rail"),
                nextConnect
        );
    }

    private static MiddleRailPairPlan randomMiddleRailPairPlan(ServerLevel level) {
        return new MiddleRailPairPlan(
                randomRailClusterPlan(level),
                randomRailClusterPlan(level)
        );
    }

    private static RailClusterPlan randomRailClusterPlan(ServerLevel level) {
        RailChoice rail = randomRailChoice(level);

        SideDungeonDefinition westDungeon = null;
        SideDungeonDefinition eastDungeon = null;

        if (rail.hasSideDungeon()) {
            if (!SideDungeonRegistry.west().isEmpty()) {
                westDungeon = WeightedPicker.pick(
                        level,
                        SideDungeonRegistry.west(),
                        SideDungeonDefinition::weight
                );
            }

            if (!SideDungeonRegistry.east().isEmpty()) {
                eastDungeon = WeightedPicker.pick(
                        level,
                        SideDungeonRegistry.east(),
                        SideDungeonDefinition::weight
                );
            }
        }

        return new RailClusterPlan(
                rail,
                westDungeon,
                eastDungeon
        );
    }

    private static RailChoice randomRailChoice(ServerLevel level) {
        boolean useCollapse = level.getRandom().nextBoolean();

        if (useCollapse && exists(level, "collapse_rail")) {
            return new RailChoice(
                    "collapse_rail",
                    load(level, "collapse_rail"),
                    false
            );
        }

        return new RailChoice(
                "rail_with_side_street",
                load(level, "rail_with_side_street"),
                true
        );
    }

    private static BlockPos findSafeMiddleRailPairOrigin(
            ServerLevel level,
            List<UnitBounds> units,
            BlockPos start,
            StationUnit referenceStation,
            MiddleRailPairPlan plan
    ) {
        for (int z = 0; z < MAX_Z_ATTEMPTS; z++) {
            int dz = SEARCH_STEP_Z * z;

            for (int x = 0; x <= MAX_X_ATTEMPTS; x++) {
                int dx = SEARCH_STEP_X * x;

                BlockPos right = start.offset(dx, 0, dz);
                if (isMiddleRailPairSafe(level, units, right, referenceStation, plan)) {
                    return right;
                }

                if (dx != 0) {
                    BlockPos left = start.offset(-dx, 0, dz);
                    if (isMiddleRailPairSafe(level, units, left, referenceStation, plan)) {
                        return left;
                    }
                }
            }
        }

        throw new IllegalStateException("No safe middle rail pair origin found.");
    }

    private static boolean isMiddleRailPairSafe(
            ServerLevel level,
            List<UnitBounds> units,
            BlockPos pairOrigin,
            StationUnit referenceStation,
            MiddleRailPairPlan plan
    ) {
        UnitBounds candidate = createVirtualMiddleRailPairBounds(
                level,
                pairOrigin,
                referenceStation,
                plan,
                MIDDLE_RAIL_PAIR_MARGIN
        );

        for (UnitBounds unit : units) {
            if (candidate.intersects(unit)) {
                return false;
            }
        }

        return true;
    }

    private static MiddleRailPairUnit placeMiddleRailPairUnit(
            ServerLevel level,
            List<StructureNode> nodes,
            BlockPos pairOrigin,
            StationUnit referenceStation,
            MiddleRailPairPlan plan
    ) {
        List<StructureNode> pairNodes = new ArrayList<>();

        BlockPos westConnect = new BlockPos(
                referenceStation.westOutConnect().getX(),
                referenceStation.westOutConnect().getY(),
                pairOrigin.getZ()
        );

        BlockPos eastConnect = new BlockPos(
                referenceStation.eastOutConnect().getX(),
                referenceStation.eastOutConnect().getY(),
                pairOrigin.getZ()
        );

        StructureNode westRail = placeRailCluster(
                level,
                nodes,
                pairNodes,
                plan.west(),
                westConnect,
                Rotation.CLOCKWISE_180
        );

        StructureNode eastRail = placeRailCluster(
                level,
                nodes,
                pairNodes,
                plan.east(),
                eastConnect,
                Rotation.CLOCKWISE_180
        );

        return new MiddleRailPairUnit(
                UnitBounds.fromNodes(
                        "middle_rail_pair",
                        pairNodes,
                        MIDDLE_RAIL_PAIR_MARGIN
                ),
                westRail.marker("polluted_world:rail_in"),
                westRail.marker("polluted_world:rail_out"),
                eastRail.marker("polluted_world:rail_in"),
                eastRail.marker("polluted_world:rail_out")
        );
    }

    private static UnitBounds createVirtualMiddleRailPairBounds(
            ServerLevel level,
            BlockPos pairOrigin,
            StationUnit referenceStation,
            MiddleRailPairPlan plan,
            int margin
    ) {
        List<StructureNode> virtualNodes = new ArrayList<>();

        BlockPos westConnect = new BlockPos(
                referenceStation.westOutConnect().getX(),
                referenceStation.westOutConnect().getY(),
                pairOrigin.getZ()
        );

        BlockPos eastConnect = new BlockPos(
                referenceStation.eastOutConnect().getX(),
                referenceStation.eastOutConnect().getY(),
                pairOrigin.getZ()
        );

        virtualRailCluster(
                level,
                virtualNodes,
                plan.west(),
                westConnect,
                Rotation.CLOCKWISE_180
        );

        virtualRailCluster(
                level,
                virtualNodes,
                plan.east(),
                eastConnect,
                Rotation.CLOCKWISE_180
        );

        return UnitBounds.fromNodes(
                "middle_rail_pair_candidate",
                virtualNodes,
                margin
        );
    }

    private static StructureNode placeRailCluster(
            ServerLevel level,
            List<StructureNode> allNodes,
            List<StructureNode> unitNodes,
            RailClusterPlan plan,
            BlockPos railInWorldPos,
            Rotation rotation
    ) {
        StructureNode rail = placeConnectedAbsolute(
                level,
                plan.rail().name(),
                plan.rail().template(),
                "polluted_world:rail_in",
                railInWorldPos,
                rotation
        );
        allNodes.add(rail);
        unitNodes.add(rail);

        if (plan.rail().hasSideDungeon()) {
            placeSideDungeon(level, allNodes, unitNodes, rail, plan.westDungeon());
            placeSideDungeon(level, allNodes, unitNodes, rail, plan.eastDungeon());
        }

        return rail;
    }

    private static void placeSideDungeon(
            ServerLevel level,
            List<StructureNode> allNodes,
            List<StructureNode> unitNodes,
            StructureNode rail,
            SideDungeonDefinition dungeon
    ) {
        if (dungeon == null) {
            return;
        }

        if (!rail.hasMarker(dungeon.markerName())) {
            return;
        }

        StructureTemplate template = load(level, dungeon.structureName());

        StructureNode dungeonNode = placeConnectedAbsolute(
                level,
                dungeon.structureName(),
                template,
                dungeon.markerName(),
                rail.marker(dungeon.markerName()),
                rail.rotation()
        );

        allNodes.add(dungeonNode);
        unitNodes.add(dungeonNode);
    }

    private static void virtualRailCluster(
            ServerLevel level,
            List<StructureNode> virtualNodes,
            RailClusterPlan plan,
            BlockPos railInWorldPos,
            Rotation rotation
    ) {
        StructureNode rail = virtualConnectedNode(
                plan.rail().name(),
                plan.rail().template(),
                "polluted_world:rail_in",
                railInWorldPos,
                rotation
        );
        virtualNodes.add(rail);

        if (plan.rail().hasSideDungeon()) {
            virtualSideDungeon(level, virtualNodes, rail, plan.westDungeon());
            virtualSideDungeon(level, virtualNodes, rail, plan.eastDungeon());
        }
    }

    private static void virtualSideDungeon(
            ServerLevel level,
            List<StructureNode> virtualNodes,
            StructureNode rail,
            SideDungeonDefinition dungeon
    ) {
        if (dungeon == null) {
            return;
        }

        if (!rail.hasMarker(dungeon.markerName())) {
            return;
        }

        StructureTemplate template = load(level, dungeon.structureName());

        StructureNode dungeonNode = virtualConnectedNode(
                dungeon.structureName(),
                template,
                dungeon.markerName(),
                rail.marker(dungeon.markerName()),
                rail.rotation()
        );

        virtualNodes.add(dungeonNode);
    }

    private static BlockPos findSafeUnitOriginZOnly(
            List<UnitBounds> units,
            BlockPos start,
            Vec3i size,
            int margin,
            int stepZ,
            int maxAttempts
    ) {
        for (int i = 0; i < maxAttempts; i++) {
            BlockPos candidateOrigin = start.offset(0, 0, stepZ * i);

            UnitBounds candidate = UnitBounds.fromOriginSize(
                    "candidate",
                    candidateOrigin,
                    size,
                    margin
            );

            boolean collides = false;

            for (UnitBounds unit : units) {
                if (candidate.intersects(unit)) {
                    collides = true;
                    break;
                }
            }

            if (!collides) {
                return candidateOrigin;
            }
        }

        throw new IllegalStateException("No safe Z-only unit origin found.");
    }

    private static Vec3i combinedSize(StructureTemplate a, StructureTemplate b) {
        Vec3i as = a.getSize();
        Vec3i bs = b.getSize();

        return new Vec3i(
                Math.max(as.getX(), bs.getX()),
                as.getY() + bs.getY(),
                Math.max(as.getZ(), bs.getZ())
        );
    }

    private static boolean exists(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);
        return level.getStructureManager().get(id).isPresent();
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

    private static int calculateLaneSpread(MiddleRailPairPlan plan) {
        int baseGap = 12;
        int reserve = 0;

        if (plan.west().rail().hasSideDungeon()) {
            reserve += 32;
        }

        if (plan.east().rail().hasSideDungeon()) {
            reserve += 32;
        }

        return baseGap + reserve;
    }
}