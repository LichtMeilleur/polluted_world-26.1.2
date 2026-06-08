package com.licht_meilleur.polluted_world.registry.neoforge;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, PollutedWorldMod.MOD_ID);

    public static final DeferredHolder<Item, Item> GAS_MASK =
            ITEMS.register("gas_mask", () -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("gas_mask")))));

    public static final DeferredHolder<Item, Item> FILTER =
            ITEMS.register("filter", () -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("filter")))));

    private NeoForgeItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}