package com.licht_meilleur.polluted_world.world.definition;

public record RailDefinition(
        String structureName,
        boolean allowSideDungeon,
        int weight
) {
}