package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.command.PollutedWorldCommands;
import com.licht_meilleur.polluted_world.network.ChangeFilterPayload;
import com.licht_meilleur.polluted_world.pollution.GasMaskFilterHandler;
import com.licht_meilleur.polluted_world.registry.ModBlocks;
import com.licht_meilleur.polluted_world.registry.ModItems;
import com.licht_meilleur.polluted_world.registry.PollutedBlocks;
import com.licht_meilleur.polluted_world.registry.fabric.FabricItemGroups;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import com.licht_meilleur.polluted_world.world.PollutedStartManager;
import com.licht_meilleur.polluted_world.world.spawn.PollutedSurfaceMobSpawner;
import com.licht_meilleur.polluted_world.worldgen.ModFeatures;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;


public class PollutedWorldFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[PollutedWorld] Fabric init");

        ModFeatures.register();

        Registry.register(BuiltInRegistries.BLOCK, PollutedWorldMod.id("corpse_chest_01"), ModBlocks.CORPSE_CHEST_01);

        Registry.register(
                BuiltInRegistries.ITEM,
                PollutedWorldMod.id("corpse_chest_01"),
                new BlockItem(
                        ModBlocks.CORPSE_CHEST_01,
                        new Item.Properties()
                                .setId(ResourceKey.create(Registries.ITEM, PollutedWorldMod.id("corpse_chest_01")))
                )
        );

        PollutedBlocks.setCorpseChest01(() -> ModBlocks.CORPSE_CHEST_01);

        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("gas_mask"), ModItems.GAS_MASK);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("activated_charcoal"), ModItems.ACTIVATED_CHARCOAL);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("poor_filter"), ModItems.POOR_FILTER);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("filter"), ModItems.FILTER);
        Registry.register(BuiltInRegistries.ITEM, PollutedWorldMod.id("high_filter"), ModItems.HIGH_FILTER);

        FabricItemGroups.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PollutedWorldCommands.register(dispatcher);
        });

        PayloadTypeRegistry.serverboundPlay().register(
                ChangeFilterPayload.TYPE,
                ChangeFilterPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ChangeFilterPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        GasMaskFilterHandler.changeFilter(context.player())
                )
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PollutedStartManager.tick(server);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PollutionLogic.tickPlayer(player);
                PollutedSurfaceMobSpawner.tick(player);
            }
        });
    }
}