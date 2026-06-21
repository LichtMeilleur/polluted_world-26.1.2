package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.registry.PollutedBlocks;
import com.licht_meilleur.polluted_world.world.definition.RailDefinition;
import com.licht_meilleur.polluted_world.world.definition.SideDungeonDefinition;
import com.licht_meilleur.polluted_world.world.definition.StationDefinition;
import com.licht_meilleur.polluted_world.world.layout.UnitBounds;
import com.licht_meilleur.polluted_world.world.registry.*;
import com.licht_meilleur.polluted_world.world.registry.CorpseLootRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UnitChainTestBuilder {

    private static final int STATION_MARGIN = 64;
    private static final int MIDDLE_RAIL_PAIR_MARGIN = 32;
    private static final int SEARCH_STEP_X = 16;
    private static final int SEARCH_STEP_Z = 32;
    private static final int MAX_X_ATTEMPTS = 12;
    private static final int MAX_Z_ATTEMPTS = 32;
    private static final int NORMAL_RAILS_FROM_STATION = 2;

    private static final int MIDDLE_LANE_SPREAD_STEP = 4;
    private static final int MIDDLE_LANE_SPREAD_MAX = 256;

    public static PollutedStructurePlacer.NetworkResult place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        List<StructureNode> nodes = new ArrayList<>();
        List<UnitBounds> units = new ArrayList<>();
        List<UnitBounds> surfaceUnits = new ArrayList<>();
        List<StationDefinition> stationDeck = createStationDeck();
        List<RailDefinition> railDeck = createRailDeck(level);
        List<SideDungeonDefinition> westDungeonDeck = new ArrayList<>(SideDungeonRegistry.west());
        List<SideDungeonDefinition> eastDungeonDeck = new ArrayList<>(SideDungeonRegistry.east());

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate railEndTemplate = load(level, "rail_end");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");

        StationDefinition startStationDef = StationRegistry.START_STATION;



        // スタート駅：前はない、先はある
        StationUnit currentStation = placeStationUnit(
                level,
                nodes,
                origin,
                startStationDef.entranceName(),
                load(level, startStationDef.entranceName()),
                startStationDef.villageName(),
                load(level, startStationDef.villageName()),
                railGateTemplate,
                railEndTemplate,
                normalRailTemplate,
                false,
                true
        );

        units.add(currentStation.bounds("station_start", STATION_MARGIN));

        boolean teleported = teleportToFirstBarrier(
                player,
                currentStation.village(),
                currentStation.entrance()
        );

        int stationIndex = 1;

        while (!stationDeck.isEmpty()) {
            StationDefinition nextStationDef = drawStationFromDeck(level, stationDeck);

            boolean hasNextStation = !stationDeck.isEmpty();

            MiddleRailPairPlan middlePlan = drawMiddleRailPairPlan(
                    level,
                    railDeck,
                    westDungeonDeck,
                    eastDungeonDeck
            );

            BlockPos pairStart = new BlockPos(
                    currentStation.westOutConnect().getX(),
                    currentStation.westOutConnect().getY(),
                    Math.max(
                            currentStation.westOutConnect().getZ(),
                            currentStation.eastOutConnect().getZ()
                    ) + SEARCH_STEP_Z
            );

            BlockPos pairOrigin = findSafeMiddleRailPairOrigin(
                    level,
                    units,
                    pairStart,
                    currentStation,
                    middlePlan
            );

            MiddleRailPairUnit middlePair = placeMiddleRailPairUnit(
                    level,
                    nodes,
                    pairOrigin,
                    currentStation,
                    middlePlan
            );

            units.add(middlePair.bounds());

            BlockPos nextStationStart = new BlockPos(
                    origin.getX(),
                    origin.getY(),
                    Math.max(
                            middlePair.westOutConnect().getZ(),
                            middlePair.eastOutConnect().getZ()
                    ) + SEARCH_STEP_Z
            );

            StructureTemplate nextEntranceTemplate = load(level, nextStationDef.entranceName());
            StructureTemplate nextVillageTemplate = load(level, nextStationDef.villageName());

            Vec3i nextStationSize = combinedSize(
                    nextEntranceTemplate,
                    nextVillageTemplate
            );

            BlockPos nextStationOrigin = findSafeUnitOriginZOnly(
                    units,
                    nextStationStart,
                    nextStationSize,
                    STATION_MARGIN,
                    SEARCH_STEP_Z,
                    MAX_Z_ATTEMPTS
            );

            StationUnit nextStation = placeStationUnit(
                    level,
                    nodes,
                    nextStationOrigin,
                    nextStationDef.entranceName(),
                    nextEntranceTemplate,
                    nextStationDef.villageName(),
                    nextVillageTemplate,
                    railGateTemplate,
                    railEndTemplate,
                    normalRailTemplate,
                    true,
                    hasNextStation
            );

            units.add(nextStation.bounds("station_" + stationIndex, STATION_MARGIN));

            CodeRailTunnelBuilder.generateSeparatedLaneConnectors(
                    level,
                    currentStation.westOutConnect().below(),
                    middlePair.westInConnect().below(),
                    currentStation.eastOutConnect().below(),
                    middlePair.eastInConnect().below(),
                    12
            );

            CodeRailTunnelBuilder.generateSeparatedLaneConnectors(
                    level,
                    middlePair.westOutConnect().below(),
                    nextStation.westInConnect().below(),
                    middlePair.eastOutConnect().below(),
                    nextStation.eastInConnect().below(),
                    12
            );

            currentStation = nextStation;
            stationIndex++;
        }

        placeSurfaceRuinsAroundSurfaceCenters(level, nodes, surfaceUnits);
        /*
        placeSurfaceRuinsAroundSurfaceAnchors(level, nodes, units);
        placeFarSurfaceRuins(level, nodes, units, origin);


         */

        placeFixedCorpseLootMarkers(level, nodes);


        placeRandomCorpseLoots(level, nodes, units);


        replantCropsNearFarmland(level, nodes);
        cleanupCropDropsNearVillages(level, nodes);



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

    private record LaneConnects(
            BlockPos west,
            BlockPos east
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
            StructureTemplate railEndTemplate,
            StructureTemplate normalRailTemplate,
            boolean hasPreviousStation,
            boolean hasNextStation
    ){
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
                railEndTemplate,
                normalRailTemplate,
                hasPreviousStation,
                hasNextStation
        );

        LaneEnds east = buildStationLaneEnds(
                level,
                nodes,
                unitNodes,
                village,
                "polluted_world:east_up",
                "polluted_world:east_down",
                railGateTemplate,
                railEndTemplate,
                normalRailTemplate,
                hasPreviousStation,
                hasNextStation
        );

        LaneEnds physicalWest;
        LaneEnds physicalEast;

        if (west.outConnect().getX() <= east.outConnect().getX()) {
            physicalWest = west;
            physicalEast = east;
        } else {
            physicalWest = east;
            physicalEast = west;
        }



        return new StationUnit(
                entrance,
                village,
                unitNodes,
                physicalWest.inConnect(),
                physicalWest.outConnect(),
                physicalEast.inConnect(),
                physicalEast.outConnect()
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
            StructureTemplate railEndTemplate,
            StructureTemplate normalRailTemplate,
            boolean hasPreviousStation,
            boolean hasNextStation
    ) {
        BlockPos inConnect;
        BlockPos outConnect;

        if (hasPreviousStation) {
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

            inConnect = downGate.marker("polluted_world:rail");
        } else {
            StructureNode end = placeConnectedAbsolute(
                    level,
                    "rail_end",
                    railEndTemplate,
                    "polluted_world:rail_in",
                    village.marker(downMarker),
                    Rotation.NONE
            );
            nodes.add(end);
            unitNodes.add(end);

            inConnect = village.marker(downMarker);
        }

        if (hasNextStation) {
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

            outConnect = nextConnect;
        } else {
            StructureNode end = placeConnectedAbsolute(
                    level,
                    "rail_end",
                    railEndTemplate,
                    "polluted_world:rail_in",
                    village.marker(upMarker),
                    Rotation.CLOCKWISE_180
            );
            nodes.add(end);
            unitNodes.add(end);

            outConnect = village.marker(upMarker);
        }

        return new LaneEnds(inConnect, outConnect);
    }

    private static MiddleRailPairPlan drawMiddleRailPairPlan(
            ServerLevel level,
            List<RailDefinition> railDeck,
            List<SideDungeonDefinition> westDungeonDeck,
            List<SideDungeonDefinition> eastDungeonDeck
    ) {
        return new MiddleRailPairPlan(
                drawRailClusterPlan(level, railDeck, westDungeonDeck, eastDungeonDeck),
                drawRailClusterPlan(level, railDeck, westDungeonDeck, eastDungeonDeck)
        );
    }

    private static RailClusterPlan drawRailClusterPlan(
            ServerLevel level,
            List<RailDefinition> railDeck,
            List<SideDungeonDefinition> westDungeonDeck,
            List<SideDungeonDefinition> eastDungeonDeck
    ) {
        RailChoice rail = drawRailChoice(level, railDeck, westDungeonDeck, eastDungeonDeck);

        SideDungeonDefinition westDungeon = null;
        SideDungeonDefinition eastDungeon = null;

        if (rail.hasSideDungeon()) {
            westDungeon = drawSideDungeon(level, westDungeonDeck);
            eastDungeon = drawSideDungeon(level, eastDungeonDeck);

            if (westDungeon == null && eastDungeon == null) {
                rail = normalRailChoice(level);
            }
        }

        return new RailClusterPlan(
                rail,
                westDungeon,
                eastDungeon
        );
    }

    private static RailChoice drawRailChoice(
            ServerLevel level,
            List<RailDefinition> railDeck,
            List<SideDungeonDefinition> westDungeonDeck,
            List<SideDungeonDefinition> eastDungeonDeck
    ) {
        boolean hasSideDungeonCards = !westDungeonDeck.isEmpty() || !eastDungeonDeck.isEmpty();

        List<RailDefinition> available = railDeck.stream()
                .filter(def -> exists(level, def.structureName()))
                .filter(def -> !def.allowSideDungeon() || hasSideDungeonCards)
                .toList();

        if (available.isEmpty()) {
            return normalRailChoice(level);
        }

        RailDefinition def = WeightedPicker.pick(
                level,
                available,
                RailDefinition::weight
        );

        railDeck.remove(def);

        return new RailChoice(
                def.structureName(),
                load(level, def.structureName()),
                def.allowSideDungeon()
        );
    }

    private static RailChoice normalRailChoice(ServerLevel level) {
        return new RailChoice(
                "normal_rail",
                load(level, "normal_rail"),
                false
        );
    }

    private static SideDungeonDefinition drawSideDungeon(
            ServerLevel level,
            List<SideDungeonDefinition> deck
    ) {
        if (deck.isEmpty()) {
            return null;
        }

        SideDungeonDefinition def = WeightedPicker.pick(
                level,
                deck,
                SideDungeonDefinition::weight
        );

        deck.remove(def);
        return def;
    }

    private static List<RailDefinition> createRailDeck(ServerLevel level) {
        return RailRegistry.RAILS.stream()
                .filter(def -> exists(level, def.structureName()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
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

        LaneConnects connects = findSeparatedMiddleLaneConnects(
                level,
                pairOrigin,
                referenceStation,
                plan,
                Rotation.CLOCKWISE_180
        );

        List<StructureNode> firstLaneNodes = new ArrayList<>();
        List<StructureNode> secondLaneNodes = new ArrayList<>();

        StructureNode firstRail = placeRailCluster(
                level,
                nodes,
                firstLaneNodes,
                plan.west(),
                connects.west(),
                Rotation.CLOCKWISE_180
        );

        StructureNode secondRail = placeRailCluster(
                level,
                nodes,
                secondLaneNodes,
                plan.east(),
                connects.east(),
                Rotation.CLOCKWISE_180
        );



        pairNodes.addAll(firstLaneNodes);
        pairNodes.addAll(secondLaneNodes);

        UnitBounds firstBounds = UnitBounds.fromNodes(
                "middle_lane_first",
                firstLaneNodes,
                MIDDLE_RAIL_PAIR_MARGIN
        );

        UnitBounds secondBounds = UnitBounds.fromNodes(
                "middle_lane_second",
                secondLaneNodes,
                MIDDLE_RAIL_PAIR_MARGIN
        );

        if (firstBounds.intersects(secondBounds)) {
            throw new IllegalStateException(
                    "Middle lane collision after placement: "
                            + " first=" + firstBounds
                            + " second=" + secondBounds
            );
        }

        StructureNode physicalWestRail;
        StructureNode physicalEastRail;

        if (firstRail.marker("polluted_world:rail_in").getX()
                <= secondRail.marker("polluted_world:rail_in").getX()) {
            physicalWestRail = firstRail;
            physicalEastRail = secondRail;
        } else {
            physicalWestRail = secondRail;
            physicalEastRail = firstRail;
        }

        LaneEnds westEnds = physicalLaneEndsByZ(physicalWestRail);
        LaneEnds eastEnds = physicalLaneEndsByZ(physicalEastRail);




        return new MiddleRailPairUnit(
                UnitBounds.fromNodes(
                        "middle_rail_pair",
                        pairNodes,
                        MIDDLE_RAIL_PAIR_MARGIN
                ),
                westEnds.inConnect(),
                westEnds.outConnect(),
                eastEnds.inConnect(),
                eastEnds.outConnect()
        );
    }

    private static LaneEnds physicalLaneEndsByZ(StructureNode rail) {
        BlockPos a = rail.marker("polluted_world:rail_in");
        BlockPos b = rail.marker("polluted_world:rail_out");

        if (a.getZ() <= b.getZ()) {
            return new LaneEnds(a, b);
        }

        return new LaneEnds(b, a);
    }

    private static UnitBounds createVirtualMiddleRailPairBounds(
            ServerLevel level,
            BlockPos pairOrigin,
            StationUnit referenceStation,
            MiddleRailPairPlan plan,
            int margin
    ) {
        List<StructureNode> westNodes = new ArrayList<>();
        List<StructureNode> eastNodes = new ArrayList<>();
        List<StructureNode> allNodes = new ArrayList<>();

        LaneConnects connects = findSeparatedMiddleLaneConnects(
                level,
                pairOrigin,
                referenceStation,
                plan,
                Rotation.CLOCKWISE_180
        );

        virtualRailCluster(
                level,
                westNodes,
                plan.west(),
                connects.west(),
                Rotation.CLOCKWISE_180
        );

        virtualRailCluster(
                level,
                eastNodes,
                plan.east(),
                connects.east(),
                Rotation.CLOCKWISE_180
        );

        UnitBounds westBounds = UnitBounds.fromNodes(
                "virtual_middle_west_lane",
                westNodes,
                margin
        );

        UnitBounds eastBounds = UnitBounds.fromNodes(
                "virtual_middle_east_lane",
                eastNodes,
                margin
        );

        if (westBounds.intersects(eastBounds)) {
            throw new IllegalStateException(
                    "Virtual middle lanes collide: west="
                            + westBounds
                            + " east="
                            + eastBounds
            );
        }

        allNodes.addAll(westNodes);
        allNodes.addAll(eastNodes);

        return UnitBounds.fromNodes(
                "middle_rail_pair_candidate",
                allNodes,
                margin
        );
    }

    private static LaneConnects findSeparatedMiddleLaneConnects(
            ServerLevel level,
            BlockPos pairOrigin,
            StationUnit referenceStation,
            MiddleRailPairPlan plan,
            Rotation rotation
    ) {
        int baseWestX = referenceStation.westOutConnect().getX();
        int baseEastX = referenceStation.eastOutConnect().getX();

        int y = referenceStation.westOutConnect().getY();
        int z = pairOrigin.getZ();

        for (int spread = 0; spread <= MIDDLE_LANE_SPREAD_MAX; spread += MIDDLE_LANE_SPREAD_STEP) {
            BlockPos westConnect = new BlockPos(
                    baseWestX - spread,
                    y,
                    z
            );

            BlockPos eastConnect = new BlockPos(
                    baseEastX + spread,
                    y,
                    z
            );

            List<StructureNode> westNodes = new ArrayList<>();
            List<StructureNode> eastNodes = new ArrayList<>();

            virtualRailCluster(
                    level,
                    westNodes,
                    plan.west(),
                    westConnect,
                    rotation
            );

            virtualRailCluster(
                    level,
                    eastNodes,
                    plan.east(),
                    eastConnect,
                    rotation
            );

            UnitBounds westBounds = UnitBounds.fromNodes(
                    "middle_west_lane_candidate",
                    westNodes,
                    MIDDLE_RAIL_PAIR_MARGIN
            );

            UnitBounds eastBounds = UnitBounds.fromNodes(
                    "middle_east_lane_candidate",
                    eastNodes,
                    MIDDLE_RAIL_PAIR_MARGIN
            );

            if (!westBounds.intersects(eastBounds)) {


                return new LaneConnects(westConnect, eastConnect);
            }


        }

        throw new IllegalStateException("No safe middle lane spread found.");
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

        clearReplaceableBlocksInTemplateArea(level, origin, template.getSize());

        boolean placed = template.placeInWorld(
                level,
                origin,
                origin,
                settings,
                level.getRandom(),
                Block.UPDATE_CLIENTS
        );

        if (!placed) {
            throw new IllegalStateException("Failed to place structure: " + structureName);
        }


        StructureNode node = com.licht_meilleur.polluted_world.world.spawn.PollutedSpawnMarkerProcessor.processAndReturn(
                level,
                new StructureNode(
                        structureName,
                        template,
                        origin,
                        rotation,
                        template.getSize(),
                        jigsaws,
                        barriers
                )
        );

        com.licht_meilleur.polluted_world.world.loot.PollutedLootMarkerProcessor.process(level, node);

        return node;

    }

    private static void clearReplaceableBlocksInTemplateArea(
            ServerLevel level,
            BlockPos origin,
            Vec3i size
    ) {
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);

                    if (isReplaceableForStructure(level, pos)) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    private static boolean isReplaceableForStructure(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.is(Blocks.FARMLAND)
                || state.is(Blocks.WHEAT)
                || state.is(Blocks.CARROTS)
                || state.is(Blocks.POTATOES)
                || state.is(Blocks.BEETROOTS)) {
            return false;
        }

        return state.isAir()
                || state.canBeReplaced()
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS);
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


    private static List<StationDefinition> createStationDeck() {
        return new ArrayList<>(StationRegistry.ADDITIONAL_STATIONS);
    }

    private static StationDefinition drawStationFromDeck(
            ServerLevel level,
            List<StationDefinition> deck
    ) {
        if (deck.isEmpty()) {
            throw new IllegalStateException("Station deck is empty.");
        }

        int totalWeight = deck.stream()
                .mapToInt(StationDefinition::weight)
                .sum();

        int roll = level.getRandom().nextInt(totalWeight);
        int cursor = 0;

        for (int i = 0; i < deck.size(); i++) {
            StationDefinition definition = deck.get(i);
            cursor += definition.weight();

            if (roll < cursor) {
                deck.remove(i);
                return definition;
            }
        }

        return deck.remove(deck.size() - 1);
    }

    private static void placeSurfaceRuinsAroundSurfaceAnchors(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> units
    ) {
        List<StructureNode> anchorNodes = nodes.stream()
                .filter(node -> node.hasMarker("polluted_world:surface_anchor"))
                .toList();

        for (StructureNode anchorNode : anchorNodes) {
            BlockPos anchor = anchorNode.marker("polluted_world:surface_anchor");

            for (int i = 0; i < 4; i++) {
                tryPlaceSurfaceRuinNear(level, nodes, units, anchor);
            }
        }
    }

    private static void tryPlaceSurfaceRuinNear(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> units,
            BlockPos anchor
    ) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int distance = 96 + level.getRandom().nextInt(320);
            int dx = level.getRandom().nextBoolean() ? distance : -distance;
            int dz = level.getRandom().nextInt(distance * 2 + 1) - distance;

            if (level.getRandom().nextBoolean()) {
                int t = dx;
                dx = dz;
                dz = t;
            }

            BlockPos near = anchor.offset(dx, 0, dz);



            var definition = WeightedPicker.pick(
                    level,
                    com.licht_meilleur.polluted_world.world.registry.SurfaceStructureRegistry.ALL,
                    com.licht_meilleur.polluted_world.world.definition.SurfaceStructureDefinition::weight
            );

            StructureTemplate template = load(level, definition.structureName());

            int y = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    near.getX(),
                    near.getZ()
            );

            BlockPos surfacePos = new BlockPos(near.getX(), y, near.getZ());

            if (!level.getBlockState(surfacePos.below()).isSolid()) {
                continue;
            }

            BlockPos anchorLocalPos = getLocalMarkerPos(
                    template,
                    definition.anchorMarker(),
                    Rotation.NONE
            );

            BlockPos origin = new BlockPos(
                    surfacePos.getX() - anchorLocalPos.getX(),
                    surfacePos.getY() - anchorLocalPos.getY(),
                    surfacePos.getZ() - anchorLocalPos.getZ()
            );

            UnitBounds candidate = UnitBounds.fromOriginSize(
                    "surface_ruin_candidate",
                    origin,
                    template.getSize(),
                    48
            );


            boolean collides = false;

            for (UnitBounds unit : units) {

                if (candidate.intersects(unit)) {
                    collides = true;
                    break;
                }
            }

            if (collides) {
                continue;
            }

            StructureNode node = placeAt(
                    level,
                    definition.structureName(),
                    template,
                    origin,
                    Rotation.NONE
            );

            nodes.add(node);
            units.add(UnitBounds.fromNodes(
                    "surface_ruin_" + definition.structureName(),
                    List.of(node),
                    48
            ));

            PollutedStructurePlacer.SurfaceCorpsePlacer.placeAroundSurfaceStructure(level, node);



            return;
        }

    }

    private static BlockPos getLocalMarkerPos(
            StructureTemplate template,
            String markerName,
            Rotation rotation
    ) {
        StructurePlaceSettings settings = settings(rotation);

        return getSortedMarkers(template, BlockPos.ZERO, settings, Blocks.JIGSAW)
                .stream()
                .filter(info -> markerName.equals(getJigsawName(info)))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Local marker not found: " + markerName));
    }






    private static void tryPlaceSurfaceRuinNear(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> units,
            BlockPos center,
            int minDistance,
            int maxDistance,
            int margin
    ) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;

            int distance = minDistance
                    + level.getRandom().nextInt(maxDistance - minDistance + 1);

            int dx = (int)Math.round(Math.cos(angle) * distance);
            int dz = (int)Math.round(Math.sin(angle) * distance);

            BlockPos near = center.offset(dx, 0, dz);

            var definition = WeightedPicker.pick(
                    level,
                    SurfaceStructureRegistry.ALL,
                    com.licht_meilleur.polluted_world.world.definition.SurfaceStructureDefinition::weight
            );

            StructureTemplate template = load(level, definition.structureName());

            int y = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    near.getX(),
                    near.getZ()
            );

            BlockPos surfacePos = new BlockPos(near.getX(), y - 1, near.getZ());

            BlockPos localAnchor = getLocalMarkerPos(
                    template,
                    definition.anchorMarker(),
                    Rotation.NONE
            );

            BlockPos structureOrigin = new BlockPos(
                    surfacePos.getX() - localAnchor.getX(),
                    surfacePos.getY() - localAnchor.getY(),
                    surfacePos.getZ() - localAnchor.getZ()
            );

            UnitBounds candidate = UnitBounds.fromOriginSize(
                    "surface_ruin_candidate",
                    structureOrigin,
                    template.getSize(),
                    margin
            );



            if (collidesWithProtectedNode(nodes, candidate)) {
                continue;
            }

            boolean collides = false;

            for (UnitBounds unit : units) {
                if (candidate.intersects(unit)) {
                    collides = true;
                    break;
                }
            }

            if (collides) {
                continue;
            }

            if (collides) {
                continue;
            }

            StructureNode node = placeAt(
                    level,
                    definition.structureName(),
                    template,
                    structureOrigin,
                    Rotation.NONE
            );

            nodes.add(node);
            units.add(UnitBounds.fromNodes(
                    "surface_ruin_" + definition.structureName(),
                    List.of(node),
                    margin
            ));

            PollutedStructurePlacer.SurfaceCorpsePlacer.placeAroundSurfaceStructure(level, node);

            return;
        }
    }
    private static void placeRandomCorpseLoots(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> units
    ) {
        int placed = 0;
        int targetCount = Math.max(1, units.size() * 2);

        for (StructureNode node : new ArrayList<>(nodes)) {
            if (placed >= targetCount) {
                return;
            }

            if (node.structureName().startsWith("station_village")) {
                continue;
            }

            // 地表構造物周辺の死体は SurfaceCorpsePlacer に任せる
            if (isSurfaceStructureNode(node)) {
                continue;
            }

            // 駅入口の屋根上配置を避けるなら一旦これも除外
            if (node.structureName().startsWith("station_entrance")) {
                continue;
            }

            int attemptsPerNode = corpseRollsForNode(level, node);

            for (int i = 0; i < attemptsPerNode && placed < targetCount; i++) {

                boolean surface = isSurfaceStructureNode(node);

                if (!surface && level.getRandom().nextInt(3) != 0) {
                    continue;
                }

                if (tryPlaceRandomCorpseNearNode(level, nodes, units, node)) {
                    placed++;
                }
            }
        }
    }

    private static boolean tryPlaceRandomCorpseNearNode(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> units,
            StructureNode node
    ) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = node.minX() + level.getRandom().nextInt(Math.max(1, node.maxX() - node.minX()));
            int z = node.minZ() + level.getRandom().nextInt(Math.max(1, node.maxZ() - node.minZ()));
            int y = node.minY() + level.getRandom().nextInt(Math.max(1, node.maxY() - node.minY()));

            BlockPos base = new BlockPos(x, y, z);

            BlockPos pos = findCorpsePlacePos(level, base);
            if (pos == null) {
                continue;
            }

            if (isTooCloseToImportantMarker(nodes, pos)) {
                continue;
            }

            if (isTooCloseToCorpse(level, pos)) {
                continue;
            }

            // unitsは駅/レール全体なので、ここで全体衝突を見ると常に弾く可能性があります。
            // なので死体同士の衝突だけ見たい場合は、corpse用boundsリストを別にするのが理想。
            // まずは重要マーカー回避＋設置可能判定で置きます。

            level.setBlock(
                    pos,
                    randomCorpseState(level, node),
                    Block.UPDATE_CLIENTS
            );

            return true;
        }

        return false;
    }

    private static BlockPos findCorpsePlacePos(ServerLevel level, BlockPos base) {
        for (int dy = -12; dy <= 12; dy++) {
            BlockPos pos = base.offset(0, dy, 0);

            if (!level.getBlockState(pos.below()).isSolid()) {
                continue;
            }

            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            return pos;
        }

        return null;
    }
    private static boolean isTooCloseToImportantMarker(List<StructureNode> nodes, BlockPos pos) {
        for (StructureNode node : nodes) {
            for (StructureTemplate.StructureBlockInfo info : node.jigsaws()) {
                String marker = getJigsawName(info);

                if (marker.contains("rail_in")
                        || marker.contains("rail_out")
                        || marker.contains("rail_gate")
                        || marker.contains("west_")
                        || marker.contains("east_")
                        || marker.contains("entrance")
                        || marker.contains("surface_anchor")) {

                    if (info.pos().distSqr(pos) < 25.0D) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void replantCropsNearFarmland(ServerLevel level, List<StructureNode> nodes) {
        for (StructureNode node : nodes) {
            if (!node.structureName().startsWith("station_village")) {
                continue;
            }

            BlockPos min = new BlockPos(node.minX() - 2, node.minY() - 2, node.minZ() - 2);
            BlockPos max = new BlockPos(node.maxX() + 2, node.maxY() + 2, node.maxZ() + 2);

            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
                    continue;
                }

                BlockPos cropPos = pos.above();

                if (!level.getBlockState(cropPos).isAir()) {
                    continue;
                }

                level.setBlock(
                        cropPos,
                        randomCrop(level),
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }

    private static net.minecraft.world.level.block.state.BlockState randomCrop(ServerLevel level) {
        int roll = level.getRandom().nextInt(4);

        return switch (roll) {
            case 0 -> Blocks.WHEAT.defaultBlockState();
            case 1 -> Blocks.CARROTS.defaultBlockState();
            case 2 -> Blocks.POTATOES.defaultBlockState();
            default -> Blocks.BEETROOTS.defaultBlockState();
        };
    }

    private static void cleanupCropDropsNearVillages(ServerLevel level, List<StructureNode> nodes) {
        for (StructureNode node : nodes) {
            if (!node.structureName().startsWith("station_village")) {
                continue;
            }

            AABB area = new AABB(
                    node.minX() - 4,
                    node.minY() - 4,
                    node.minZ() - 4,
                    node.maxX() + 4,
                    node.maxY() + 4,
                    node.maxZ() + 4
            );

            for (net.minecraft.world.entity.item.ItemEntity itemEntity
                    : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) {

                ItemStack stack = itemEntity.getItem();

                if (isCropDrop(stack)) {
                    itemEntity.discard();
                }
            }
        }
    }

    private static boolean isCropDrop(ItemStack stack) {
        return stack.is(Items.WHEAT)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.BEETROOT_SEEDS);
    }

    private static BlockState randomCorpseState(ServerLevel level, StructureNode node) {
        var def = WeightedPicker.pick(
                level,
                com.licht_meilleur.polluted_world.world.registry.CorpseLootRegistry.ALL,
                com.licht_meilleur.polluted_world.world.definition.CorpseLootDefinition::weight
        );

        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());

        return switch (def.blockName()) {
            case "corpse_chest_01" ->
                    PollutedBlocks.corpseChest01().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, facing);
            case "corpse_chest_02" ->
                    PollutedBlocks.corpseChest02().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, facing);
            case "corpse_chest_03" ->
                    PollutedBlocks.corpseChest03().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, facing);

            default ->
                    PollutedBlocks.corpseChest01().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, facing);
        };
    }

    private static void placeSurfaceRuinsAroundSurfaceCenters(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> surfaceUnits
    ) {
        List<StructureNode> centers = nodes.stream()
                .filter(UnitChainTestBuilder::isSurfaceRuinCenter)
                .filter(node -> node.hasMarker("polluted_world:surface_anchor"))
                .toList();

        for (StructureNode centerNode : centers) {
            BlockPos center = centerNode.marker("polluted_world:surface_anchor");

            // 内側：密
            placeSurfaceRuinsInRing(level, nodes, surfaceUnits, center, 12, 48, 40, 10);

            // 中間：普通
            placeSurfaceRuinsInRing(level, nodes, surfaceUnits, center, 48, 100, 20, 18);

            // 外側：まばら
            placeSurfaceRuinsInRing(level, nodes, surfaceUnits, center, 100, 200, 10, 28);
        }
    }

    private static boolean isSurfaceRuinCenter(StructureNode node) {
        String name = node.structureName();

        // 駅入口
        if (name.startsWith("station_entrance")) {
            return true;
        }

        // 地上出口つきサイドダンジョン
        if (name.startsWith("side_dungeon")
                || name.contains("side_dungeon")
                || name.contains("side")) {
            return node.hasMarker("polluted_world:surface_anchor");
        }

        return false;
    }

    private static void placeSurfaceRuinsInRing(
            ServerLevel level,
            List<StructureNode> nodes,
            List<UnitBounds> surfaceUnits,
            BlockPos center,
            int minDistance,
            int maxDistance,
            int count,
            int margin
    ) {
        for (int i = 0; i < count; i++) {
            tryPlaceSurfaceRuinNear(level, nodes, surfaceUnits, center, minDistance, maxDistance, margin);
        }
    }

    private static int corpseRollsForNode(ServerLevel level, StructureNode node) {
        String name = node.structureName();

        if (name.startsWith("station_village")) {
            return 0;
        }

        if (isSurfaceStructureNode(node)) {
            return 0;
        }

        if (name.startsWith("station_entrance")) {
            return 0;
        }

        if (name.contains("rail")) {
            return level.getRandom().nextInt(2); // 0〜1
        }

        return 0;
    }

    private static boolean isTooCloseToCorpse(
            ServerLevel level,
            BlockPos pos
    ) {
        int radius = 6;

        for (BlockPos check : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius),
                pos.offset(radius, 1, radius)
        )) {

            Block block = level.getBlockState(check).getBlock();

            if (block == PollutedBlocks.corpseChest01()
                    || block == PollutedBlocks.corpseChest02()
                    || block == PollutedBlocks.corpseChest03()) {
                return true;
            }
        }

        return false;
    }

    private static void placeFixedCorpseLootMarkers(
            ServerLevel level,
            List<StructureNode> nodes
    ) {
        for (StructureNode node : new ArrayList<>(nodes)) {
            if (node.structureName().startsWith("station_village")) {
                continue;
            }

            for (StructureTemplate.StructureBlockInfo info : node.jigsaws()) {
                String marker = getMarkerName(info);

                if (!CorpseLootRegistry.isCorpseMarker(marker)) {
                    continue;
                }

                String lootTable = CorpseLootRegistry.lootTableFromMarker(marker);
                if (lootTable == null) {
                    continue;
                }

                BlockPos pos = info.pos();

                level.setBlock(
                        pos,
                        corpseStateForLootTable(level, lootTable),
                        Block.UPDATE_CLIENTS
                );
            }
        }
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

    private static BlockState corpseStateForLootTable(ServerLevel level, String lootTable) {
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());

        Block block = switch (lootTable) {
            case "corpse/military" -> PollutedBlocks.corpseChest01();
            case "corpse/research" -> PollutedBlocks.corpseChest02();
            case "corpse/civilian" -> PollutedBlocks.corpseChest03();
            default -> PollutedBlocks.corpseChest03();
        };

        return block.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static boolean isSurfaceStructureNode(StructureNode node) {
        String name = node.structureName();

        return SurfaceStructureRegistry.ALL.stream()
                .anyMatch(def -> def.structureName().equals(name));
    }

    private static boolean collidesWithProtectedNode(List<StructureNode> nodes, UnitBounds candidate) {
        for (StructureNode node : nodes) {
            String name = node.structureName();

            boolean protectedNode =
                    name.startsWith("station_entrance")
                            || name.startsWith("station_village")
                            || name.contains("side_dungeon");

            if (!protectedNode) {
                continue;
            }

            UnitBounds protectedBounds = UnitBounds.fromNodes(
                    "protected_" + name,
                    List.of(node),
                    24
            );

            if (candidate.intersects(protectedBounds)) {
                return true;
            }
        }

        return false;
    }

}