package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.command.PollutedWorldCommands;
import com.licht_meilleur.polluted_world.registry.ModItems;
import com.licht_meilleur.polluted_world.registry.fabric.FabricItemGroups;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import com.licht_meilleur.polluted_world.world.PollutedStartManager;
import com.licht_meilleur.polluted_world.worldgen.ModFeatures;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import com.licht_meilleur.polluted_world.pollution.SurfacePollutionTransformer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import com.licht_meilleur.polluted_world.worldgen.PollutedRegion;
import terrablender.api.Regions;



public class PollutedWorldFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[PollutedWorld] Fabric init");

        /*ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (!generated) return;

            SurfacePollutionTransformer.transformChunk(level, chunk);
        });

         */
        //Regions.register(new PollutedRegion());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PollutedWorldCommands.register(dispatcher);
        });

        ModFeatures.register();

        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("gas_mask"), ModItems.GAS_MASK);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("activated_charcoal"), ModItems.ACTIVATED_CHARCOAL);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("poor_filter"), ModItems.POOR_FILTER);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("filter"), ModItems.FILTER);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("high_filter"), ModItems.HIGH_FILTER);

        FabricItemGroups.register();

        PollutedWorldMod.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PollutedStartManager.tick(server);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PollutionLogic.tickPlayer(player);
            }
        });
    }
}