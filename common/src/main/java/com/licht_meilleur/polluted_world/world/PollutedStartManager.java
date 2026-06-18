package com.licht_meilleur.polluted_world.world;

import com.licht_meilleur.polluted_world.world.state.PollutedWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PollutedStartManager {

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;

        PollutedWorldState state = PollutedWorldState.get(level);

        if (!state.isGenerated()) {
            ServerPlayer firstPlayer = server.getPlayerList().getPlayers()
                    .stream()
                    .filter(player -> !player.isSpectator())
                    .findFirst()
                    .orElse(null);

            if (firstPlayer == null) {
                return;
            }

            PollutedStructurePlacer.placeUnitChainOnSurface(
                    level,
                    firstPlayer,
                    firstPlayer.blockPosition()
            );

            for (int i = 0; i < 6; i++) {
                BlockPos around = firstPlayer.blockPosition().offset(
                        level.getRandom().nextInt(512) - 256,
                        0,
                        level.getRandom().nextInt(512) - 256
                );

                try {
                    PollutedStructurePlacer.placeSurfaceRuinTest(level, firstPlayer, around);
                } catch (Exception ignored) {
                }
            }

            // placeUnitChainOnSurface 内でバリア位置へテレポート済みなので、その位置を保存
            state.markGenerated(firstPlayer.blockPosition());
        }



        if (state.startSpawnPos() == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;

            if (!state.hasFirstJoined(player.getUUID())) {
                var pos = state.startSpawnPos();

                player.teleportTo(
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D
                );

                player.setDeltaMovement(0, 0, 0);
                player.fallDistance = 0.0F;

                state.markFirstJoined(player.getUUID());
            }
        }
    }
}