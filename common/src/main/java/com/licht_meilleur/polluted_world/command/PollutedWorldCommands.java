package com.licht_meilleur.polluted_world.command;

import com.licht_meilleur.polluted_world.world.PollutedStructurePlacer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class PollutedWorldCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("polluted_place")
                        .requires(source -> true)

                        .then(Commands.literal("entrance")
                                .executes(ctx -> place(ctx.getSource(), "station_entrance_01")))

                        .then(Commands.literal("village")
                                .executes(ctx -> place(ctx.getSource(), "station_village_01")))

                        .then(Commands.literal("start")
                                .executes(ctx -> placeStart(ctx.getSource())))


                        // 新方式の最小テスト：サイドレール + サイドダンジョン干渉確認
                        .then(Commands.literal("side_layout")
                                .executes(ctx -> placeSideLayout(ctx.getSource())))


                        .then(Commands.literal("station_unit")
                                .executes(ctx -> placeStationUnit(ctx.getSource())))


                        .then(Commands.literal("unit_chain")
                                .executes(ctx -> placeUnitChain(ctx.getSource())))

                        .then(Commands.literal("surface_ruin")
                                .executes(ctx -> placeSurfaceRuin(ctx.getSource())))




        );
    }

    private static int place(CommandSourceStack source, String structureName) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();

        try {
            PollutedStructurePlacer.PlaceResult result =
                    PollutedStructurePlacer.placeAndScan(level, structureName, origin);

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed " + structureName
                                    + " / jigsaw: " + result.jigsawMarkers()
                                    + " / barrier: " + result.barrierMarkers()
                    ),
                    true
            );

            return result.jigsawMarkers();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to place " + structureName + ": " + e.getMessage()));
            return 0;
        }
    }

    private static int placeStart(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        try {
            PollutedStructurePlacer.StartResult result =
                    PollutedStructurePlacer.placeEntranceAndVillage(
                            player.level(),
                            player,
                            player.blockPosition()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed start station"
                                    + " / entrance jigsaw: " + result.entranceJigsaws()
                                    + " / village jigsaw: " + result.villageJigsaws()
                                    + " / barrier: " + result.barrierMarkers()
                                    + " / teleported: " + result.teleported()
                    ),
                    true
            );

            return result.villageJigsaws();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to place start station: " + e.getMessage()));
            return 0;
        }
    }



    private static int placeSideLayout(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        try {
            PollutedStructurePlacer.NetworkResult result =
                    PollutedStructurePlacer.placeSideDungeonLayoutTest(
                            player.level(),
                            player,
                            player.blockPosition()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed side dungeon layout"
                                    + " / parts: " + result.railCount()
                                    + " / barrier: " + result.barrierMarkers()
                                    + " / teleported: " + result.teleported()
                    ),
                    true
            );

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed side dungeon layout: " + e.getMessage()));
            return 0;
        }
    }

    private static int placeStationUnit(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        try {
            PollutedStructurePlacer.NetworkResult result =
                    PollutedStructurePlacer.placeStationUnitTest(
                            player.level(),
                            player,
                            player.blockPosition()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed station unit"
                                    + " / parts: " + result.railCount()
                                    + " / barrier: " + result.barrierMarkers()
                                    + " / teleported: " + result.teleported()
                    ),
                    true
            );

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed station unit: " + e.getMessage()));
            return 0;
        }
    }

    private static int placeUnitChain(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        try {
            PollutedStructurePlacer.NetworkResult result =
                    PollutedStructurePlacer.placeUnitChainTest(
                            player.level(),
                            player,
                            player.blockPosition()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed unit chain"
                                    + " / parts: " + result.railCount()
                                    + " / barrier: " + result.barrierMarkers()
                                    + " / teleported: " + result.teleported()
                    ),
                    true
            );

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed unit chain: " + e.getMessage()));
            return 0;
        }
    }

    private static int placeSurfaceRuin(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        try {
            PollutedStructurePlacer.NetworkResult result =
                    PollutedStructurePlacer.placeSurfaceRuinTest(
                            player.level(),
                            player,
                            player.blockPosition()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "Placed surface ruin"
                                    + " / parts: " + result.railCount()
                                    + " / barrier: " + result.barrierMarkers()
                    ),
                    true
            );

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed surface ruin: " + e.getMessage()));
            return 0;
        }
    }
}