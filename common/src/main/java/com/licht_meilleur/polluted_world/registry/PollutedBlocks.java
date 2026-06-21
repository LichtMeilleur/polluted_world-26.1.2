package com.licht_meilleur.polluted_world.registry;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class PollutedBlocks {
    private static Supplier<Block> corpseChest01;
    private static Supplier<Block> corpseChest02;
    private static Supplier<Block> corpseChest03;

    private PollutedBlocks() {
    }

    public static void setCorpseChest01(Supplier<Block> supplier) {
        corpseChest01 = supplier;
    }
    public static void setCorpseChest02(Supplier<Block> supplier) {
        corpseChest02 = supplier;
    }
    public static void setCorpseChest03(Supplier<Block> supplier) {
        corpseChest03 = supplier;
    }

    public static Block corpseChest01() {
        if (corpseChest01 == null) {
            throw new IllegalStateException("Corpse chest block is not registered yet.");
        }
        return corpseChest01.get();
    }
    public static Block corpseChest02() {
        if (corpseChest02 == null) {
            throw new IllegalStateException("Corpse chest block is not registered yet.");
        }
        return corpseChest02.get();
    }
    public static Block corpseChest03() {
        if (corpseChest03 == null) {
            throw new IllegalStateException("Corpse chest block is not registered yet.");
        }
        return corpseChest03.get();
    }
}