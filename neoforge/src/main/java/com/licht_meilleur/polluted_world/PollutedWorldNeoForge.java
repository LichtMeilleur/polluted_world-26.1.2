package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.command.PollutedWorldCommands;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import com.licht_meilleur.polluted_world.registry.PollutedBlocks;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeBlocks;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeFeatures;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItemGroups;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItems;
import com.licht_meilleur.polluted_world.world.PollutedStartManager;
import com.licht_meilleur.polluted_world.world.spawn.PollutedSpawnMarkerProcessor;
import com.licht_meilleur.polluted_world.worldgen.PollutedRegion;
import com.licht_meilleur.polluted_world.network.ChangeFilterPayload;
import com.licht_meilleur.polluted_world.pollution.GasMaskFilterHandler;
import com.licht_meilleur.polluted_world.world.spawn.PollutedSurfaceMobSpawner;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import terrablender.api.Regions;



@Mod(PollutedWorldMod.MOD_ID)
@EventBusSubscriber(modid = PollutedWorldMod.MOD_ID)
public class PollutedWorldNeoForge {

    public PollutedWorldNeoForge(IEventBus modBus) {
        NeoForgeItems.register(modBus);
        NeoForgeItemGroups.register(modBus);
        NeoForgeFeatures.register(modBus);
        NeoForgeBlocks.register(modBus);
        PollutedBlocks.setCorpseChest01(NeoForgeBlocks.CORPSE_CHEST_01::get);
        PollutedBlocks.setCorpseChest02(NeoForgeBlocks.CORPSE_CHEST_02::get);
        PollutedBlocks.setCorpseChest03(NeoForgeBlocks.CORPSE_CHEST_03::get);


        Regions.register(new PollutedRegion());

        // NeoForge側では今は呼ばない
        // PollutedWorldMod.init();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PollutedStartManager.tick(event.getServer());

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PollutionLogic.tickPlayer(player);
            PollutedSurfaceMobSpawner.tick(player);

        }
    }


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PollutedWorldCommands.register(event.getDispatcher());
    }
    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(PollutedWorldMod.MOD_ID)
                .playToServer(
                        ChangeFilterPayload.TYPE,
                        ChangeFilterPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() ->
                                GasMaskFilterHandler.changeFilter((ServerPlayer) context.player())
                        )
                );
    }


}