package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.RailDefinition;

import java.util.List;

public class RailRegistry {

    public static final List<RailDefinition> RAILS = List.of(

            /*
            new RailDefinition(
                    "normal_rail",
                    false,
                    30
            ),

             */

            new RailDefinition(
                    "rail_with_side_street",
                    true,
                    30
            ),

            new RailDefinition(
                    "collapse_rail",
                    false,
                    20
            ),

            new RailDefinition(
                    "trains_graveyard",
                    false,
                    20
            ),

        new RailDefinition(
                    "flooded_collapse_rail",
                    false,
                    10
            )
    );
}
