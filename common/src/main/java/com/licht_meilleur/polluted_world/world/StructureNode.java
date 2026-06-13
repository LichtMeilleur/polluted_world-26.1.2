package com.licht_meilleur.polluted_world.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public record StructureNode(
        String structureName,
        StructureTemplate template,
        BlockPos origin,
        Rotation rotation,
        List<StructureTemplate.StructureBlockInfo> jigsaws,
        List<StructureTemplate.StructureBlockInfo> barriers
) {
    public int jigsawCount() {
        return jigsaws.size();
    }

    public int barrierCount() {
        return barriers.size();
    }

    public BlockPos marker(String name) {
        String available = jigsaws.stream()
                .map(StructureNode::getJigsawName)
                .toList()
                .toString();

        return jigsaws.stream()
                .filter(info -> name.equals(getJigsawName(info)))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Marker not found: " + name + " / available: " + available
                ));
    }

    public Optional<BlockPos> firstBarrierPos() {
        return barriers.stream()
                .map(StructureTemplate.StructureBlockInfo::pos)
                .findFirst();
    }

    public void removeMarkers(ServerLevel level) {
        for (StructureTemplate.StructureBlockInfo info : jigsaws) {
            level.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        for (StructureTemplate.StructureBlockInfo info : barriers) {
            level.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static String getJigsawName(StructureTemplate.StructureBlockInfo info) {
        if (info.nbt() == null) {
            return "";
        }

        return info.nbt().getString("name").orElse("");
    }
}