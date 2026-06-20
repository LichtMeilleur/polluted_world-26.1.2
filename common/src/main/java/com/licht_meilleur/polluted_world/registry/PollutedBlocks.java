package com.licht_meilleur.polluted_world.registry;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class PollutedBlocks {
    private static Supplier<Block> corpseChest01;

    private PollutedBlocks() {
    }

    public static void setCorpseChest01(Supplier<Block> supplier) {
        corpseChest01 = supplier;
    }

    public static Block corpseChest01() {
        if (corpseChest01 == null) {
            throw new IllegalStateException("Corpse chest block is not registered yet.");
        }
        return corpseChest01.get();
    }
}