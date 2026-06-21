package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.CorpseLootDefinition;

import java.util.List;

public class CorpseLootRegistry {

    private static final String CORPSE_PREFIX = "corpse_";

    public static final List<CorpseLootDefinition> ALL = List.of(
            new CorpseLootDefinition(
                    "corpse_chest_01",
                    "corpse/military",
                    30
            ),
            new CorpseLootDefinition(
                    "corpse_chest_02",
                    "corpse/research",
                    30
            ),
            new CorpseLootDefinition(
                    "corpse_chest_03",
                    "corpse/civilian",
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

    public static String lootTableFromMarker(String marker) {
        String normalized = normalizeMarker(marker);

        if (!normalized.startsWith(CORPSE_PREFIX)) {
            return null;
        }

        String category = normalized.substring(CORPSE_PREFIX.length());

        if (category.isBlank()) {
            return "corpse/civilian";
        }

        return "corpse/" + category;
    }

    public static boolean isCorpseMarker(String marker) {
        String normalized = normalizeMarker(marker);
        return normalized.startsWith(CORPSE_PREFIX);
    }

    private static String normalizeMarker(String marker) {
        if (marker == null) {
            return "";
        }

        if (marker.startsWith("pw:")) {
            return marker.substring("pw:".length());
        }

        if (marker.startsWith("polluted_world:")) {
            return marker.substring("polluted_world:".length());
        }

        return marker;
    }
}