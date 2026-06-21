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
    private static void placeSurfaceRuinsAroundStart(ServerLevel level, ServerPlayer player, BlockPos center) {
        // 駅周辺：密
        placeSurfaceRuinsInRing(level, player, center, 48, 160, 10);

        // 中距離：普通
        placeSurfaceRuinsInRing(level, player, center, 160, 360, 6);

        // 遠距離：まばら
        placeSurfaceRuinsInRing(level, player, center, 360, 720, 4);
    }

    private static void placeSurfaceRuinsInRing(
            ServerLevel level,
            ServerPlayer player,
            BlockPos center,
            int minRadius,
            int maxRadius,
            int count
    ) {
        var random = level.getRandom();

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = minRadius + random.nextInt(maxRadius - minRadius + 1);

            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);

            BlockPos around = new BlockPos(x, center.getY(), z);

            try {
                PollutedStructurePlacer.placeSurfaceRuinTest(level, player, around);
            } catch (Exception ignored) {
            }
        }
    }
}