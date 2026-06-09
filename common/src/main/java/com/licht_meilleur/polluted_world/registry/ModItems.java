package com.licht_meilleur.polluted_world.registry;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {


    public static final Item ACTIVATED_CHARCOAL =
            new Item(props("activated_charcoal"));
    public static final Item GAS_MASK = new GasMaskItem(props("gas_mask"));
    public static final Item POOR_FILTER =
            new FilterItem(
                    props("poor_filter")
                            .durability(400)
            );
    public static final Item FILTER =
            new FilterItem(
                    props("filter")
                            .durability(1200)
            );
    public static final Item HIGH_FILTER =
            new FilterItem(
                    props("high_filter")
                            .durability(3000)
            );

    private ModItems() {
    }

    private static Item.Properties props(String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id(name)));
    }

    public static void init() {
    }
}