package com.licht_meilleur.polluted_world.registry.neoforge;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.item.FilterItem;
import com.licht_meilleur.polluted_world.item.GasMaskItem;
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
            ITEMS.register("gas_mask", () -> new GasMaskItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("gas_mask")))));

    public static final DeferredHolder<Item, Item> ACTIVATED_CHARCOAL =
            ITEMS.register("activated_charcoal", () -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("activated_charcoal")))));

    public static final DeferredHolder<Item, Item> POOR_FILTER =
            ITEMS.register("poor_filter", () -> new FilterItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("poor_filter")))
                    .durability(400)));

    public static final DeferredHolder<Item, Item> FILTER =
            ITEMS.register("filter", () -> new FilterItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("filter")))
                    .durability(1200)));

    public static final DeferredHolder<Item, Item> HIGH_FILTER =
            ITEMS.register("high_filter", () -> new FilterItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("high_filter")))
                    .durability(3000)));

    private NeoForgeItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}