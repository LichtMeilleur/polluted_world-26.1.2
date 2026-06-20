package com.licht_meilleur.polluted_world.registry.neoforge;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.block.CorpseLootBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PollutedWorldMod.MOD_ID);

    public static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(PollutedWorldMod.MOD_ID);

    public static final DeferredBlock<Block> CORPSE_CHEST_01 =
            BLOCKS.register("corpse_chest_01", registryName ->
                    new CorpseLootBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(ResourceKey.create(Registries.BLOCK, registryName))
                                    .strength(0.8F)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> CORPSE_CHEST_01_ITEM =
            BLOCK_ITEMS.register("corpse_chest_01", registryName ->
                    new BlockItem(
                            CORPSE_CHEST_01.get(),
                            new Item.Properties()
                                    .setId(ResourceKey.create(Registries.ITEM, registryName))
                    )
            );

    private NeoForgeBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ITEMS.register(bus);
    }
}