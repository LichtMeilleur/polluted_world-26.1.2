package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.SideDungeonDefinition;

import java.util.List;

public class SideDungeonRegistry {

    public static final List<SideDungeonDefinition> ALL = List.of(
            new SideDungeonDefinition(
                    "west_side_dungeon_01",
                    "polluted_world:west_side_street",
                    SideDungeonDefinition.Side.WEST,
                    100
            ),
            new SideDungeonDefinition(
                    "east_side_dungeon_01",
                    "polluted_world:east_side_street",
                    SideDungeonDefinition.Side.EAST,
                    100
            )
    );

    public static List<SideDungeonDefinition> west() {
        return ALL.stream()
                .filter(definition -> definition.side() == SideDungeonDefinition.Side.WEST)
                .toList();
    }

    public static List<SideDungeonDefinition> east() {
        return ALL.stream()
                .filter(definition -> definition.side() == SideDungeonDefinition.Side.EAST)
                .toList();
    }
}