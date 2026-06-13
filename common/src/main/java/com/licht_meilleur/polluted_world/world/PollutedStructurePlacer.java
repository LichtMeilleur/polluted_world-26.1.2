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
        StructureTemplate entranceATemplate = load(level, "station_entrance_02");
        StructureTemplate villageATemplate = load(level, "station_village_02");

        StructureTemplate entranceBTemplate = load(level, "station_entrance_01");
        StructureTemplate villageBTemplate = load(level, "station_village_01");

        StructureTemplate railGateTemplate = load(level, "rail_gate");
        StructureTemplate normalRailTemplate = load(level, "normal_rail");

        List<RailPiece> railPool = createRailPool(level, normalRailTemplate);

        int railCount = 6 + level.getRandom().nextInt(5);

        List<StructureNode> nodes = new ArrayList<>();

        StructureNode entranceA = placeRoot(level, "station_entrance_02", entranceATemplate, origin, Rotation.NONE);
        nodes.add(entranceA);

        StructureNode villageA = placeConnectedAbsolute(
                level,
                "station_village_02",
                villageATemplate,
                "polluted_world:entrance",
                entranceA.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(villageA);

        // west_up → west_down
        // up側ゲートは180度
        StructureNode westStartGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                villageA.marker("polluted_world:west_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(westStartGate);

        StructureNode westLast = westStartGate;
        BlockPos westNextConnect = westStartGate.marker("polluted_world:rail");

        for (int i = 0; i < railCount; i++) {
            RailPiece rail = randomRail(level, railPool);

            westLast = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    westNextConnect,
                    Rotation.CLOCKWISE_180
            );
            nodes.add(westLast);

            westNextConnect = westLast.marker("polluted_world:rail_out");
        }

        // down側ゲートは0度
        StructureNode westEndGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail",
                westLast.marker("polluted_world:rail_out"),
                Rotation.NONE
        );
        nodes.add(westEndGate);

        StructureNode villageB = placeConnectedAbsolute(
                level,
                "station_village_01",
                villageBTemplate,
                "polluted_world:west_down",
                westEndGate.marker("polluted_world:rail_gate"),
                Rotation.NONE
        );
        nodes.add(villageB);

        StructureNode entranceB = placeConnectedAbsolute(
                level,
                "station_entrance_01",
                entranceBTemplate,
                "polluted_world:entrance",
                villageB.marker("polluted_world:entrance"),
                Rotation.NONE
        );
        nodes.add(entranceB);

        // =========================
// east_up -> east_down 専用
// 駅A側と駅B側から半分ずつNBTレールを伸ばし、残りだけコード掘削
// =========================

        StructureNode eastStartGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                villageA.marker("polluted_world:east_up"),
                Rotation.CLOCKWISE_180
        );
        nodes.add(eastStartGate);

        StructureNode eastEndGate = placeConnectedAbsolute(
                level,
                "rail_gate",
                railGateTemplate,
                "polluted_world:rail_gate",
                villageB.marker("polluted_world:east_down"),
                Rotation.NONE
        );
        nodes.add(eastEndGate);

        int halfRailCount = railCount / 2;

// 駅A側から半分伸ばす
        StructureNode eastFromStart = eastStartGate;
        BlockPos eastStartNext = eastStartGate.marker("polluted_world:rail");

        for (int i = 0; i < halfRailCount; i++) {
            RailPiece rail = randomRail(level, railPool);

            eastFromStart = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    eastStartNext,
                    Rotation.CLOCKWISE_180
            );
            nodes.add(eastFromStart);

            eastStartNext = eastFromStart.marker("polluted_world:rail_out");
        }

// 駅B側から半分伸ばす
        StructureNode eastFromEnd = eastEndGate;
        BlockPos eastEndNext = eastEndGate.marker("polluted_world:rail");

        for (int i = 0; i < halfRailCount; i++) {
            RailPiece rail = randomRail(level, railPool);

            eastFromEnd = placeConnectedAbsolute(
                    level,
                    rail.name(),
                    rail.template(),
                    "polluted_world:rail_in",
                    eastEndNext,
                    Rotation.NONE
            );
            nodes.add(eastFromEnd);

            eastEndNext = eastFromEnd.marker("polluted_world:rail_out");
        }

// 残ったズレだけコードで掘る
        CodeRailTunnelBuilder.generateTunnel(
                level,
                eastStartNext,
                eastEndNext
        );

        boolean teleported = teleportToFirstBarrier(player, villageA, entranceA);

        int barrierCount = nodes.stream()
                .mapToInt(StructureNode::barrierCount)
                .sum();

        for (StructureNode node : nodes) {
            node.removeMarkers(level);
        }

        return new NetworkResult(railCount, barrierCount, teleported);
    }

    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return optionalTemplate.get();
    }

    private static List<RailPiece> createRailPool(ServerLevel level, StructureTemplate normalRailTemplate) {
        List<RailPiece> pool = new ArrayList<>();

        // 基本レール。必ず入れる。
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));
        pool.add(new RailPiece("normal_rail", normalRailTemplate));

        // 今後NBTを追加したらここに増やす。
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
}