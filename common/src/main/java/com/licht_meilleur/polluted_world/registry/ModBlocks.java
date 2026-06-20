package com.licht_meilleur.polluted_world.registry;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.block.CorpseLootBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final Block CORPSE_CHEST_01 = new CorpseLootBlock(
            BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, PollutedWorldMod.id("corpse_chest")))
                    .strength(0.8F)
                    .noOcclusion()
    );
}