package com.licht_meilleur.polluted_world.world.registry;

import com.licht_meilleur.polluted_world.world.definition.StationDefinition;

import java.util.List;

public class StationRegistry {

    public static final StationDefinition START_STATION = new StationDefinition(
            "station_entrance_01",
            "station_village_01",
            0
    );

    public static final List<StationDefinition> ADDITIONAL_STATIONS = List.of(
            new StationDefinition(
                    "station_entrance_02",
                    "station_village_02",
                    100
            ),
            new StationDefinition(
                    "station_entrance_03",
                    "station_village_03",
                    100
            ),
            new StationDefinition(
                    "station_entrance_04",
                    "station_village_04",
                    100
            )
    );
}