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

public class PollutedStructurePlacer {

    public record PlaceResult(int jigsawMarkers, int barrierMarkers) {
    }

    public record StartResult(int entranceJigsaws, int villageJigsaws, int barrierMarkers, boolean teleported) {
    }

    public record NetworkResult(int railCount, int barrierMarkers, boolean teleported) {
    }

    private record StationPair(String entrance, String village) {
        boolean sameAs(StationPair other) {
            return entrance.equals(other.entrance)
                    && village.equals(other.village);
        }
    }

    private static final List<String> ADDITIONAL_ENTRANCES = List.of(
            "station_entrance_02",
            "station_entrance_03"
    );

    private static final List<String> ADDITIONAL_VILLAGES = List.of(
            "station_village_02",
            "station_village_03"
    );

    private static final List<String> ENTRANCE_DECK = new ArrayList<>();
    private static final List<String> VILLAGE_DECK = new ArrayList<>();



    public static PlaceResult placeAndScan(ServerLevel level, String structureName, BlockPos origin) {
        StructureTemplate template = load(level, structureName);
        StructureNode node = placeRoot(level, structureName, template, origin, Rotation.NONE);

        int jigsaws = node.jigsawCount();
        int barriers = node.barrierCount();

        node.removeMarkers(level);

        return new PlaceResult(jigsaws, barriers);
    }

    public static StartResult placeEntranceAndVillage(ServerLevel level, ServerPlayer player, BlockPos origin) {
        StructureTemplate entranceTemplate = load(level, "station_entrance_01");
        StructureTemplate villageTemplate = load(level, "station_village_01");

        StructureNode entrance = placeRoot(level, "station_entrance_01", entranceTemplate, origin, Rotation.NONE);

        StructureNode village = placeChild(
                level,
                entrance,
                "station_village_01",
                villageTemplate,
                "polluted_world:entrance",
                entrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );

        boolean teleported = teleportToFirstBarrier(player, village, entrance);

        int barrierCount = entrance.barrierCount() + village.barrierCount();

        entrance.removeMarkers(level);
        village.removeMarkers(level);

        return new StartResult(
                entrance.jigsawCount(),
                village.jigsawCount(),
                barrierCount,
                teleported
        );
    }

    public static NetworkResult placeTwoStationNetwork(ServerLevel level, ServerPlayer player, BlockPos origin) {
        resetStationDecksForNetwork();

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");
        List<RailPiece> railPool = createRailPool(level, normalRailTemplate);

        List<StructureNode> nodes = new ArrayList<>();

        // =========================
        // 初期駅 fixed
        // =========================
        StructureTemplate entranceTemplate = load(level, "station_entrance_01");
        StructureTemplate villageTemplate = load(level, "station_village_01");

        StructureNode currentEntrance = placeRoot(
                level,
                "station_entrance_01",
                entranceTemplate,
                origin,
                Rotation.NONE
        );
        nodes.add(currentEntrance);

        StructureNode currentVillage = placeConnectedAbsolute(
                level,
                "station_village_01",
                villageTemplate,
                "polluted_world:entrance",
                currentEntrance.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(currentVillage);

        boolean teleported = teleportToFirstBarrier(player, currentVillage, currentEntrance);

        int totalRailCount = 0;

        // =========================
        // 追加駅を手札が尽きるまで接続
        // =========================
        while (hasNextStationPair()) {
            StationPair nextPair = drawAdditionalStationPair(level);

            ConnectResult result = connectNextStation(
                    level,
                    nodes,
                    currentVillage,
                    nextPair,
                    railGateTemplate,
                    normalRailTemplate,
                    railPool
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

        return new NetworkResult(totalRailCount, barrierCount, teleported);
    }

    private static ConnectResult connectNextStation(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode currentVillage,
            StationPair nextPair,
            StructureTemplate railGateTemplate,
            StructureTemplate normalRailTemplate,
            List<RailPiece> railPool
    ) {
        StructureTemplate nextEntranceTemplate = load(level, nextPair.entrance());
        StructureTemplate nextVillageTemplate = load(level, nextPair.village());

        int railCount = 6 + level.getRandom().nextInt(5);

        // =========================
        // west_up -> west_down
        // =========================
        StructureNode westStartGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                currentVillage.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(westStartGate);

        StructureNode westLast = westStartGate;
        BlockPos westNextConnect = westStartGate.marker("polluted_world:rail");

        for (int i = 0; i < railCount; i++) {
            RailPiece rail = i < 3
                    ? new RailPiece("normal_rail", normalRailTemplate)
                    : randomRail(level, railPool);

            westLast = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    westNextConnect,
                    Rotation.CLOCKWISE_180
            );

            nodes.add(westLast);

            if (i >= 3) {
                tryPlaceSideDungeon(level, nodes, westLast);
            }

            westNextConnect = westLast.marker("polluted_world:rail_out");
        }

        StructureNode westEndGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail",
                westLast.marker("polluted_world:rail_out"),
                Rotation.NONE
        );
        nodes.add(westEndGate);

        StructureNode nextVillage = placeConnectedAbsolute(
                level,
                nextPair.village(),
                nextVillageTemplate,
                "polluted_world:west_down",
                westEndGate.marker("polluted_world:rail_gate"),
                Rotation.NONE
        );
        nodes.add(nextVillage);

        StructureNode nextEntrance = placeConnectedAbsolute(
                level,
                nextPair.entrance(),
                nextEntranceTemplate,
                "polluted_world:entrance",
                nextVillage.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(nextEntrance);

        // =========================
        // east_up -> east_down
        // =========================
        StructureNode eastStartGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                currentVillage.marker("polluted_world:east_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(eastStartGate);

        StructureNode eastEndGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                nextVillage.marker("polluted_world:east_down"),
                Rotation.NONE
        );
        nodes.add(eastEndGate);

        int halfRailCount = railCount / 2;

        StructureNode eastFromStart = eastStartGate;
        BlockPos eastStartNext = eastStartGate.marker("polluted_world:rail");

        for (int i = 0; i < halfRailCount; i++) {
            RailPiece rail = i < 3
                    ? new RailPiece("normal_rail", normalRailTemplate)
                    : randomRail(level, railPool);

            eastFromStart = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    eastStartNext,
                    Rotation.CLOCKWISE_180
            );

            nodes.add(eastFromStart);

            if (i >= 3) {
                tryPlaceSideDungeon(level, nodes, eastFromStart);
            }

            eastStartNext = eastFromStart.marker("polluted_world:rail_out");
        }

        StructureNode eastFromEnd = eastEndGate;
        BlockPos eastEndNext = eastEndGate.marker("polluted_world:rail");

        for (int i = 0; i < halfRailCount; i++) {
            RailPiece rail = i < 3
                    ? new RailPiece("normal_rail", normalRailTemplate)
                    : randomRail(level, railPool);

            eastFromEnd = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    eastEndNext,
                    Rotation.NONE
            );

            nodes.add(eastFromEnd);

            if (i >= 3) {
                tryPlaceSideDungeon(level, nodes, eastFromEnd);
            }

            eastEndNext = eastFromEnd.marker("polluted_world:rail_out");
        }

        CodeRailTunnelBuilder.generateTunnel(
                level,
                eastStartNext,
                eastEndNext
        );

        return new ConnectResult(nextVillage, railCount);
    }

    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return optionalTemplate.get();
    }

    private static void resetStationDecksForNetwork() {
        ENTRANCE_DECK.clear();
        VILLAGE_DECK.clear();

        ENTRANCE_DECK.addAll(ADDITIONAL_ENTRANCES);
        VILLAGE_DECK.addAll(ADDITIONAL_VILLAGES);
    }

    private static boolean hasNextStationPair() {
        return !ENTRANCE_DECK.isEmpty() && !VILLAGE_DECK.isEmpty();
    }

    private static StationPair drawAdditionalStationPair(ServerLevel level) {
        String entrance = drawRandom(level, ENTRANCE_DECK);
        String village = drawRandom(level, VILLAGE_DECK);

        System.out.println(
                "[PollutedWorld] Draw station = "
                        + entrance
                        + " + "
                        + village
                        + " / entranceLeft=" + ENTRANCE_DECK.size()
                        + " / villageLeft=" + VILLAGE_DECK.size()
        );

        return new StationPair(entrance, village);
    }

    private static String drawRandom(ServerLevel level, List<String> deck) {
        int index = level.getRandom().nextInt(deck.size());
        return deck.remove(index);
    }

    private static List<RailPiece> createRailPool(ServerLevel level, StructureTemplate normalRailTemplate) {
        List<RailPiece> pool = new ArrayList<>();

        // 基本レール。必ず入れる。
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));

        // 今後NBTを追加したらここに増やす。
        addOptionalRail(level, pool, "rail_with_side_street");
        addOptionalRail(level, pool, "collapse_rail");
        addOptionalRail(level, pool, "rail_collapsed");
        addOptionalRail(level, pool, "rail_flooded");
        addOptionalRail(level, pool, "rail_maintenance");
        addOptionalRail(level, pool, "rail_abandoned");

        return pool;
    }

    private static void addOptionalRail(ServerLevel level, List<RailPiece> pool, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isPresent()) {
            pool.add(new RailPiece(structureName, optionalTemplate.get()));
        }
    }

    private static RailPiece randomRail(ServerLevel level, List<RailPiece> pool) {
        return pool.get(level.getRandom().nextInt(pool.size()));
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

    private static StructureNode placeChild(
            ServerLevel level,
            StructureNode parent,
            String childName,
            StructureTemplate childTemplate,
            String childMarkerName,
            BlockPos parentConnectPos,
            Rotation localRotation
    ) {
        Rotation childRotation = RotationUtil.add(parent.rotation(), localRotation);

        StructurePlaceSettings settings = settings(childRotation);

        List<StructureTemplate.StructureBlockInfo> localMarkers =
                getSortedMarkers(childTemplate, BlockPos.ZERO, settings, Blocks.JIGSAW);

        BlockPos localMarkerPos = localMarkers.stream()
                .filter(info -> childMarkerName.equals(getJigsawName(info)))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Local marker not found: " + childMarkerName));

        BlockPos childOrigin = new BlockPos(
                parentConnectPos.getX() - localMarkerPos.getX(),
                parentConnectPos.getY() - localMarkerPos.getY(),
                parentConnectPos.getZ() - localMarkerPos.getZ()
        );

        return placeAt(level, childName, childTemplate, childOrigin, childRotation);
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

    private static BlockPos getConnectedAbsoluteOrigin(
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

        return new BlockPos(
                connectToWorldPos.getX() - localMarkerPos.getX(),
                connectToWorldPos.getY() - localMarkerPos.getY(),
                connectToWorldPos.getZ() - localMarkerPos.getZ()
        );
    }

    private static boolean collidesWithPlacedStructures(
            BlockPos candidateOrigin,
            StructureTemplate candidateTemplate,
            List<StructureNode> nodes,
            StructureNode ignoreNode
    ) {
        for (StructureNode node : nodes) {
            if (node == ignoreNode) {
                continue;
            }

            if (node.intersects(candidateOrigin, candidateTemplate.getSize(), 0)) {
                return true;
            }
        }

        return false;
    }

    private static StructureNode placeConnectedAbsoluteOffset(
            ServerLevel level,
            String childName,
            StructureTemplate childTemplate,
            String childMarkerName,
            BlockPos connectToWorldPos,
            Rotation absoluteRotation,
            BlockPos offset
    ) {
        StructureNode node = placeConnectedAbsolute(
                level,
                childName,
                childTemplate,
                childMarkerName,
                connectToWorldPos.offset(offset),
                absoluteRotation
        );

        return node;
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

    private static boolean teleportToFirstBarrier(ServerPlayer player, StructureNode first, StructureNode second) {
        Optional<BlockPos> spawnPos = first.firstBarrierPos().or(second::firstBarrierPos);

        if (spawnPos.isEmpty()) {
            return false;
        }

        BlockPos pos = spawnPos.get();
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return true;
    }

    private static String getJigsawName(StructureTemplate.StructureBlockInfo info) {
        if (info.nbt() == null) {
            return "";
        }

        return info.nbt().getString("name").orElse("");
    }

    public static NetworkResult placeTwoStationNetworkOnSurface(ServerLevel level, ServerPlayer player, BlockPos nearPos) {
        StructureTemplate entranceTemplate = load(level, "station_entrance_02");

        BlockPos surfacePos = findFlatSurface(level, nearPos, 64, 32)
                .orElseThrow(() -> new IllegalStateException("No valid surface found near " + nearPos));

        StructurePlaceSettings settings = settings(Rotation.NONE);

        BlockPos anchorLocalPos = getLocalMarkerPos(
                entranceTemplate,
                "polluted_world:surface_anchor",
                Rotation.NONE
        );

        BlockPos origin = new BlockPos(
                surfacePos.getX() - anchorLocalPos.getX(),
                surfacePos.getY() - anchorLocalPos.getY(),
                surfacePos.getZ() - anchorLocalPos.getZ()
        );

        return placeTwoStationNetwork(level, player, origin);
    }
    private static Optional<BlockPos> findFlatSurface(ServerLevel level, BlockPos center, int targetY, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    BlockPos ground = new BlockPos(
                            center.getX() + dx,
                            targetY,
                            center.getZ() + dz
                    );

                    if (isValidSurfaceAnchor(level, ground)) {
                        return Optional.of(ground.above());
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isValidSurfaceAnchor(ServerLevel level, BlockPos ground) {
        if (level.getBlockState(ground).isAir()) {
            return false;
        }

        for (int y = 1; y <= 20; y++) {
            if (!level.getBlockState(ground.above(y)).isAir()) {
                return false;
            }
        }

        return true;
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

    private record RailPiece(String name, StructureTemplate template) {
    }

    private record ConnectResult(StructureNode nextVillage, int railCount) {
    }

    private static void tryPlaceSideDungeon(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode railNode
    ) {
        boolean hasWest = railNode.hasMarker("polluted_world:west_side_street");
        boolean hasEast = railNode.hasMarker("polluted_world:east_side_street");

        if (!hasWest && !hasEast) {
            return;
        }

        boolean eastFirst = level.getRandom().nextBoolean();

        if (eastFirst) {
            if (tryPlaceEastSideDungeon(level, nodes, railNode)) {
                return;
            }

            tryPlaceWestSideDungeon(level, nodes, railNode);
        } else {
            if (tryPlaceWestSideDungeon(level, nodes, railNode)) {
                return;
            }

            tryPlaceEastSideDungeon(level, nodes, railNode);
        }
    }

    private static boolean tryPlaceWestSideDungeon(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode railNode
    ) {
        if (!railNode.hasMarker("polluted_world:west_side_street")) {
            return false;
        }

        StructureTemplate template = load(level, "west_side_dungeon_01");

        Rotation rotation = railNode.rotation();

        BlockPos origin = getConnectedAbsoluteOrigin(
                template,
                "polluted_world:west_side_street",
                railNode.marker("polluted_world:west_side_street"),
                rotation
        );

        if (collidesWithPlacedStructures(origin, template, nodes, railNode)) {
            return false;
        }

        StructureNode dungeon = placeAt(
                level,
                "west_side_dungeon_01",
                template,
                origin,
                rotation
        );

        nodes.add(dungeon);
        return true;
    }

    private static boolean tryPlaceEastSideDungeon(
            ServerLevel level,
            List<StructureNode> nodes,
            StructureNode railNode
    ) {
        if (!railNode.hasMarker("polluted_world:east_side_street")) {
            return false;
        }

        StructureTemplate template = load(level, "east_side_dungeon_01");

        Rotation rotation = railNode.rotation();

        BlockPos origin = getConnectedAbsoluteOrigin(
                template,
                "polluted_world:east_side_street",
                railNode.marker("polluted_world:east_side_street"),
                rotation
        );

        if (collidesWithPlacedStructures(origin, template, nodes, railNode)) {
            return false;
        }

        StructureNode dungeon = placeAt(
                level,
                "east_side_dungeon_01",
                template,
                origin,
                rotation
        );

        nodes.add(dungeon);
        return true;
    }


}