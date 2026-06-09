package com.licht_meilleur.polluted_world.registry.fabric;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.registry.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class FabricItemGroups {

    private FabricItemGroups() {
    }

    public static final CreativeModeTab POLLUTED_WORLD_TAB =
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.polluted_world"))
                    .icon(() -> new ItemStack(ModItems.GAS_MASK))
                    .displayItems((parameters, output) -> {
                                output.accept(ModItems.GAS_MASK);
                                output.accept(ModItems.ACTIVATED_CHARCOAL);
                                output.accept(ModItems.POOR_FILTER);
                                output.accept(ModItems.FILTER);
                                output.accept(ModItems.HIGH_FILTER);
                    })
                    .build();

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                PollutedWorldMod.id("polluted_world"),
                POLLUTED_WORLD_TAB
        );
    }
}