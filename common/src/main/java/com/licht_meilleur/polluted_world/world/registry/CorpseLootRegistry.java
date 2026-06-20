package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.CorpseLootDefinition;

import java.util.List;

public class CorpseLootRegistry {

    public static final List<CorpseLootDefinition> ALL = List.of(
            new CorpseLootDefinition(
                    "corpse_chest_01",
                    "corpse/military",
                    30
            )
    );

    public static String lootTableFor(String blockName) {
        for (CorpseLootDefinition def : ALL) {
            if (def.blockName().equals(blockName)) {
                return def.lootTable();
            }
        }

        return "corpse/civilian";
    }
}