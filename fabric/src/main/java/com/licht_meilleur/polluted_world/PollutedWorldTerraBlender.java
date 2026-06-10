package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.worldgen.PollutedRegion;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

public class PollutedWorldTerraBlender implements TerraBlenderApi {

    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new PollutedRegion());
    }
}