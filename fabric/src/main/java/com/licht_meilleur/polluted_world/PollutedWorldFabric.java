package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.registry.ModItems;
import com.licht_meilleur.polluted_world.registry.fabric.FabricItemGroups;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class PollutedWorldFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("gas_mask"), ModItems.GAS_MASK);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("filter"), ModItems.FILTER);

        PollutedWorldMod.init();

        FabricItemGroups.register();
    }
}