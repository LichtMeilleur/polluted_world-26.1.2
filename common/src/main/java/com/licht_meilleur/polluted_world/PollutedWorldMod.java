package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.registry.ModItemGroups;
import com.licht_meilleur.polluted_world.registry.ModItems;
import net.minecraft.resources.Identifier;

public final class PollutedWorldMod {

    public static final String MOD_ID = "polluted_world";

    private PollutedWorldMod() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        ModItems.init();
        ModItemGroups.init();
    }
}