package com.licht_meilleur.polluted_world.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class JustEnoughGunsLootCompat {

    public enum Category {
        START_SUPPLY,
        MILITARY,
        RARE,
        LEGENDARY,
        CORPSE_MILITARY
    }

    private static final String MOD_ID = "jeg";

    // 開始支給：最低限の護身用
    private static final List<String> START_SUPPLY = List.of(
            "jeg:revolver",
            "jeg:pistol_ammo"
    );

    // スクラップワークベンチ級
    private static final List<String> MILITARY = List.of(
            "jeg:revolver",
            "jeg:waterpipe_shotgun",
            "jeg:double_barrel_shotgun",
            "jeg:semi_auto_pistol",
            "jeg:pistol_ammo",
            "jeg:handmade_shell",
            "jeg:pistol_magazine",
            "jeg:repair_kit",
            "jeg:scrap"
    );

    // ガンメタルワークベンチ級
    private static final List<String> RARE = List.of(
            "jeg:combat_pistol",
            "jeg:custom_smg",
            "jeg:semi_auto_rifle",
            "jeg:combat_rifle",
            "jeg:assault_rifle",
            "jeg:pump_shotgun",
            "jeg:pistol_ammo",
            "jeg:rifle_ammo",
            "jeg:shotgun_shell",
            "jeg:pistol_magazine",
            "jeg:smg_magazine",
            "jeg:rifle_magazine",
            "jeg:shotgun_magazine",
            "jeg:smoke_grenade"
    );

    // ガンナイトワークベンチ級
    private static final List<String> LEGENDARY = List.of(
            "jeg:burst_rifle",
            "jeg:bolt_action_rifle",
            "jeg:service_rifle",
            "jeg:light_machine_gun",
            "jeg:minigun",
            "jeg:grenade_launcher",
            "jeg:rocket_launcher",
            "jeg:machine_gun_magazine",
            "jeg:rifle_drum_magazine",
            "jeg:shotgun_drum_magazine",
            "jeg:grenade",
            "jeg:stun_grenade",
            "jeg:ammo_pouch"
    );

    // 死体：弾多め、銃はスクラップ級、ごく稀にレア級
    private static final List<String> CORPSE_MILITARY = List.of(
            "jeg:pistol_ammo",
            "jeg:rifle_ammo",
            "jeg:handmade_shell",
            "jeg:shotgun_shell",
            "jeg:pistol_magazine",
            "jeg:revolver",
            "jeg:semi_auto_pistol",
            "jeg:waterpipe_shotgun",
            "jeg:combat_pistol",
            "jeg:custom_smg"
    );

    private JustEnoughGunsLootCompat() {
    }

    public static boolean isLoaded() {
        return BuiltInRegistries.ITEM.keySet()
                .stream()
                .anyMatch(id -> MOD_ID.equals(id.getNamespace()));
    }

    public static void addBonusLoot(Container container, RandomSource random, Category category) {
        if (!isLoaded()) return;

        int rolls = switch (category) {
            case START_SUPPLY -> 2;
            case MILITARY -> 1 + random.nextInt(2);
            case RARE -> random.nextInt(3) == 0 ? 1 : 0;
            case LEGENDARY -> random.nextInt(8) == 0 ? 1 : 0;
            case CORPSE_MILITARY -> 1 + random.nextInt(2);
        };

        for (int i = 0; i < rolls; i++) {
            ItemStack stack = randomStack(random, category);
            if (!stack.isEmpty()) {
                insertRandomEmptySlot(container, stack, random);
            }
        }
    }

    private static ItemStack randomStack(RandomSource random, Category category) {
        List<String> ids = switch (category) {
            case START_SUPPLY -> START_SUPPLY;
            case MILITARY -> MILITARY;
            case RARE -> RARE;
            case LEGENDARY -> LEGENDARY;
            case CORPSE_MILITARY -> CORPSE_MILITARY;
        };

        for (int attempt = 0; attempt < 16; attempt++) {
            String id = ids.get(random.nextInt(ids.size()));
            Item item = findItem(id);

            if (item == Items.AIR) continue;

            return new ItemStack(item, countFor(id, random, category));
        }

        return ItemStack.EMPTY;
    }

    private static int countFor(String id, RandomSource random, Category category) {
        if (category == Category.START_SUPPLY && id.equals("jeg:pistol_ammo")) {
            return 32 + random.nextInt(33); // 32〜64発
        }

        if (id.endsWith("_ammo") || id.endsWith("_shell")) {
            return switch (category) {
                case CORPSE_MILITARY -> 4 + random.nextInt(13);
                case MILITARY -> 8 + random.nextInt(17);
                case RARE -> 12 + random.nextInt(25);
                case LEGENDARY -> 16 + random.nextInt(33);
                default -> 16;
            };
        }

        if (id.endsWith("_magazine")) return 1;
        if (id.contains("grenade")) return 1;

        return 1;
    }

    private static Item findItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return Items.AIR;

        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
    }

    private static void insertRandomEmptySlot(Container container, ItemStack stack, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            int slot = random.nextInt(container.getContainerSize());

            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack);
                return;
            }
        }
    }
}