package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItemGroups;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.licht_meilleur.polluted_world.pollution.SurfacePollutionTransformer;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

@Mod(PollutedWorldMod.MOD_ID)
@EventBusSubscriber(modid = PollutedWorldMod.MOD_ID)
public class PollutedWorldNeoForge {

    public PollutedWorldNeoForge(IEventBus modBus) {
        NeoForgeItems.register(modBus);
        NeoForgeItemGroups.register(modBus);

        // NeoForge側では今は呼ばない
        // PollutedWorldMod.init();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PollutionLogic.tickPlayer(player);
        }
    }
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        SurfacePollutionTransformer.transformChunk(serverLevel, chunk);
    }
}