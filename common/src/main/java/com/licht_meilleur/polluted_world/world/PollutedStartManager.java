package com.licht_meilleur.polluted_world.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PollutedStartManager {

    private static final Set<UUID> TRIED_PLAYERS = new HashSet<>();

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (TRIED_PLAYERS.contains(player.getUUID())) {
                continue;
            }

            TRIED_PLAYERS.add(player.getUUID());

            try {
                PollutedStructurePlacer.placeTwoStationNetworkOnSurface(
                        player.level(),
                        player,
                        player.blockPosition()
                );
            } catch (Exception e) {
                System.out.println("[PollutedWorld] Start generation skipped: " + e.getMessage());
            }
        }
    }
}