package com.licht_meilleur.polluted_world.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PollutedStartManager {

    private static final Set<UUID> GENERATED_PLAYERS = new HashSet<>();

    public static void tick(MinecraftServer server) {

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            if (GENERATED_PLAYERS.contains(player.getUUID())) {
                continue;
            }

            try {

                PollutedStructurePlacer.placeTwoStationNetworkOnSurface(
                        player.level(),
                        player,
                        player.blockPosition()
                );

                GENERATED_PLAYERS.add(player.getUUID());

            } catch (Exception e) {
                System.out.println("[PollutedWorld] Failed: " + e.getMessage());
            }
        }
    }
}