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





    private static StructureTemplate load(ServerLevel level, String structureName) {
        Identifier id = PollutedWorldMod.id(structureName);

        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(id);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalStateException("Structure not found: " + id);
        }

        return optionalTemplate.get();
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



    public static NetworkResult placeUnitChainOnSurface(
            ServerLevel level,
            ServerPlayer player,
            BlockPos nearPos
    ) {
        StructureTemplate entranceTemplate = load(level, "station_entrance_01");

        BlockPos surfacePos = findFlatSurface(level, nearPos, 64, 96)
                .orElseThrow(() -> new IllegalStateException("No valid surface found near " + nearPos));

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

        System.out.println(
                "[PollutedWorld] Start unit chain surface="
                        + surfacePos
                        + " origin="
                        + origin
        );

        return UnitChainTestBuilder.place(level, player, origin);
    }

    private static Optional<BlockPos> findFlatSurface(ServerLevel level, BlockPos center, int targetY, int radius) {
        for (int r = 0; r <= radius; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
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
        if (!isSolidDryGround(level, ground)) {
            return false;
        }

        if (hasWaterNearby(level, ground, 48, 8)) {
            return false;
        }

        // 駅・入口用に広めの平坦チェック
        int checkRadius = 24;
        int maxHeightDiff = 2;

        for (int dx = -checkRadius; dx <= checkRadius; dx += 4) {
            for (int dz = -checkRadius; dz <= checkRadius; dz += 4) {
                BlockPos sample = ground.offset(dx, 0, dz);

                if (!isSolidDryGround(level, sample)) {
                    return false;
                }

                // 低すぎ・高すぎを弾く
                for (int dy = 1; dy <= maxHeightDiff; dy++) {
                    if (isSolidDryGround(level, sample.above(dy))) {
                        return false;
                    }
                }

                for (int dy = 1; dy <= maxHeightDiff; dy++) {
                    if (!isSolidDryGround(level, sample.below(dy))) {
                        return false;
                    }
                }

                // 上空クリアランス
                for (int y = 1; y <= 32; y++) {
                    if (!level.getBlockState(sample.above(y)).isAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean isSolidDryGround(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        if (state.is(Blocks.WATER)
                || state.is(Blocks.ICE)
                || state.is(Blocks.FROSTED_ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.LAVA)) {
            return false;
        }

        return state.isSolid();
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



    public static NetworkResult placeSideDungeonLayoutTest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        return SideDungeonLayoutTestBuilder.place(level, player, origin);
    }

    public static NetworkResult placeStationUnitTest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        return StationUnitTestBuilder.place(level, player, origin);
    }


    public static NetworkResult placeUnitChainTest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin
    ) {
        return UnitChainTestBuilder.place(level, player, origin);
    }

    public static NetworkResult placeSurfaceRuinTest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos nearPos
    ) {
        var definition = com.licht_meilleur.polluted_world.world.registry.WeightedPicker.pick(
                level,
                com.licht_meilleur.polluted_world.world.registry.SurfaceStructureRegistry.ALL,
                com.licht_meilleur.polluted_world.world.definition.SurfaceStructureDefinition::weight
        );

        StructureTemplate template = load(level, definition.structureName());

        BlockPos surfacePos = findFlatSurface(level, nearPos, 64, 96)
                .orElseThrow(() -> new IllegalStateException("No valid surface found near " + nearPos));

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

        StructureNode node = placeAt(
                level,
                definition.structureName(),
                template,
                origin,
                Rotation.NONE
        );

        int barrierCount = node.barrierCount();
        node.removeMarkers(level);

        return new NetworkResult(
                1,
                barrierCount,
                false
        );
    }

    private static boolean hasWaterNearby(ServerLevel level, BlockPos center, int radius, int yRange) {
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                for (int dy = -yRange; dy <= yRange; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    var state = level.getBlockState(pos);

                    if (state.is(Blocks.WATER)
                            || state.is(Blocks.ICE)
                            || state.is(Blocks.FROSTED_ICE)
                            || state.is(Blocks.PACKED_ICE)
                            || state.is(Blocks.BLUE_ICE)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


}