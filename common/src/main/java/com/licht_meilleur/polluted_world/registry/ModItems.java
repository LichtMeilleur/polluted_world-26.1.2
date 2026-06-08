package com.licht_meilleur.polluted_world.registry;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final Item GAS_MASK = new Item(props("gas_mask"));
    public static final Item FILTER = new Item(props("filter"));

    private ModItems() {
    }

    private static Item.Properties props(String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id(name)));
    }

    public static void init() {
    }
}