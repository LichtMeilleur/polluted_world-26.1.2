package com.licht_meilleur.polluted_world.pollution;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.world.ModBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class PollutionLogic {

    private static final int SAFE_DEPTH = 20;
    private static final int WEAK_DEPTH = 10;
    private static final int DAMAGE_INTERVAL = 40;
    private static final float DAMAGE_AMOUNT = 1.0F;

    private PollutionLogic() {
    }

    public static void tickPlayer(ServerPlayer player) {
        ServerLevel level = player.level();

        if (!level.dimension().equals(Level.OVERWORLD)) return;

        if (player.tickCount % 100 == 0) {

            protectSurfaceMonstersFromSun(level, player.blockPosition());
        }

        if (player.isCreative() || player.isSpectator()) return;

        int pollutionLevel = getPollutionLevel(level, player.blockPosition());
        if (pollutionLevel <= 0) return;

        if (hasGasMask(player)) {
            ItemStack filter = findFilter(player);

            if (!filter.isEmpty()) {
                damageFilter(player, filter);
                return;
            }

            if (player.tickCount % 80 == 0) {
                player.sendOverlayMessage(
                        Component.translatable("message.polluted_world.no_filter")
                );
            }
        }

        if (hasGasMask(player) && !hasFilter(player) && player.tickCount % 80 == 0) {
            player.sendOverlayMessage(
                    Component.translatable("message.polluted_world.no_filter")
            );
        }

        if (player.tickCount % DAMAGE_INTERVAL == 0) {
            var motion = player.getDeltaMovement();

            player.hurtServer(level, player.damageSources().magic(), DAMAGE_AMOUNT);

            player.setDeltaMovement(motion);
            player.hurtMarked = true;
        }
    }

    public static int getPollutionLevel(Level level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return 0;
        }

        if (!level.getBiome(pos).is(ModBiomeTags.POLLUTED_BIOMES)) {
            return 0;
        }

        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );

        int depth = surfaceY - pos.getY();

        if (level.canSeeSky(pos)) {
            return 3;
        }

        if (depth < WEAK_DEPTH) {
            return 2;
        }

        if (depth < SAFE_DEPTH) {
            return 1;
        }

        return 0;
    }

    public static boolean isPollutedPosition(Level level, BlockPos pos) {
        return getPollutionLevel(level, pos) > 0;
    }

    public static boolean hasGasMask(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return isItem(helmet, "gas_mask");
    }

    public static boolean hasWorkingGasMask(ServerPlayer player) {
        return hasGasMask(player) && !findFilter(player).isEmpty();
    }
    public static boolean hasFilter(ServerPlayer player) {

        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {

            ItemStack stack = inv.getItem(i);

            if (isItem(stack, "filter")) {
                return true;
            }
        }

        return false;
    }
    private static boolean isFilter(ItemStack stack) {
        return isItem(stack, "poor_filter")
                || isItem(stack, "filter")
                || isItem(stack, "high_filter");
    }

    public static ItemStack findFilter(ServerPlayer player) {

        Inventory inv = player.getInventory();

        // 高性能
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);

            if (isItem(stack, "high_filter")) {
                return stack;
            }
        }

        // 標準
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);

            if (isItem(stack, "filter")) {
                return stack;
            }
        }

        // 粗悪
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);

            if (isItem(stack, "poor_filter")) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static void damageFilter(ServerPlayer player, ItemStack filter) {
        if (player.tickCount % DAMAGE_INTERVAL != 0) {
            return;
        }

        int nextDamage = filter.getDamageValue() + 1;

        if (nextDamage >= filter.getMaxDamage()) {
            filter.shrink(1);
            player.sendOverlayMessage(
                    Component.translatable("message.polluted_world.filter_broken")
            );
            return;
        }

        filter.setDamageValue(nextDamage);
    }

    private static boolean isItem(ItemStack stack, String name) {
        if (stack.isEmpty()) return false;

        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(PollutedWorldMod.id(name));
    }



    private static void protectSurfaceMonstersFromSun(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(64.0D, 48.0D, 64.0D);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof Monster)) continue;
            if (!isPollutedPosition(level, mob.blockPosition())) continue;

            if (mob.isOnFire()) {
                mob.setRemainingFireTicks(0);
            }
        }
    }
}