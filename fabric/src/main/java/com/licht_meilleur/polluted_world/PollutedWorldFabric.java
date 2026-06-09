package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.registry.ModItems;
import com.licht_meilleur.polluted_world.registry.fabric.FabricItemGroups;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import com.licht_meilleur.polluted_world.pollution.SurfacePollutionTransformer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;



public class PollutedWorldFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[PollutedWorld] Fabric init");

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (!generated) return;

            SurfacePollutionTransformer.transformChunk(level, chunk);
        });

        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("gas_mask"), ModItems.GAS_MASK);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("filter"), ModItems.FILTER);

        FabricItemGroups.register();

        PollutedWorldMod.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PollutionLogic.tickPlayer(player);
            }
        });
    }
}