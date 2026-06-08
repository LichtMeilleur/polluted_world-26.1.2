package com.licht_meilleur.polluted_world.registry.neoforge;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeItemGroups {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PollutedWorldMod.MOD_ID);

    private NeoForgeItemGroups() {
    }

    public static void register(IEventBus bus) {
        TABS.register("polluted_world", () ->
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.polluted_world"))
                        .icon(() -> new ItemStack(NeoForgeItems.GAS_MASK.get()))
                        .displayItems((parameters, output) -> {
                            output.accept(NeoForgeItems.GAS_MASK.get());
                            output.accept(NeoForgeItems.FILTER.get());
                        })
                        .build()
        );

        TABS.register(bus);
    }
}