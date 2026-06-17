package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.SurfaceStructureDefinition;

import java.util.List;

public class SurfaceStructureRegistry {

    public static final List<SurfaceStructureDefinition> ALL = List.of(
            new SurfaceStructureDefinition(
                    "church",
                    "polluted_world:surface_anchor",
                    20
            ),
            new SurfaceStructureDefinition(
                    "broken_church",
                    "polluted_world:surface_anchor",
                    100
            )
    );
}