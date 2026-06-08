package com.licht_meilleur.polluted_world;

import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItemGroups;
import com.licht_meilleur.polluted_world.registry.neoforge.NeoForgeItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(PollutedWorldMod.MOD_ID)
public class PollutedWorldNeoForge {

    public PollutedWorldNeoForge(IEventBus modBus) {
        NeoForgeItems.register(modBus);
        NeoForgeItemGroups.register(modBus);

        //PollutedWorldMod.init();
    }
}