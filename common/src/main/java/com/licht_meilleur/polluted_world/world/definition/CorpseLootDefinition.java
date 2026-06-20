package com.licht_meilleur.polluted_world.world.definition;

public record CorpseLootDefinition(
        String blockName,
        String lootTable,
        int weight
) {
}