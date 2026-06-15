package com.licht_meilleur.polluted_world.world.definition;

public record SideDungeonDefinition(
        String structureName,
        String markerName,
        Side side,
        int weight
) {
    public enum Side {
        WEST,
        EAST
    }
}