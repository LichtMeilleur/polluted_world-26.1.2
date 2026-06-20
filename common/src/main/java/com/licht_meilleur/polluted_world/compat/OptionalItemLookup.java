package com.licht_meilleur.polluted_world.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class OptionalItemLookup {
    private OptionalItemLookup() {
    }

    public static Item itemOrAir(String id) {
        Identifier identifier = Identifier.tryParse(id);

        if (identifier == null) {
            return Items.AIR;
        }

        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
    }

    public static boolean exists(String id) {
        return itemOrAir(id) != Items.AIR;
    }
}