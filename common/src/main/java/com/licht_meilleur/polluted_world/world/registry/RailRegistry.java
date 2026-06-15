package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.RailDefinition;

import java.util.List;

public class RailRegistry {

    public static final List<RailDefinition> RAILS = List.of(

            new RailDefinition(
                    "normal_rail",
                    false,
                    50
            ),

            new RailDefinition(
                    "rail_with_side_street",
                    true,
                    30
            ),

            new RailDefinition(
                    "collapsed_rail",
                    false,
                    20
            )
    );
}
