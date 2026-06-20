package com.licht_meilleur.polluted_world.pollution;

import com.licht_meilleur.polluted_world.item.GasMaskItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class GasMaskFilterHandler {
    private GasMaskFilterHandler() {
    }

    public static void changeFilter(ServerPlayer player) {
        ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);

        if (!PollutionLogic.isGasMaskStack(mask)) {
            player.sendOverlayMessage(Component.translatable("message.polluted_world.no_gas_mask"));
            return;
        }

        ItemStack newFilter = PollutionLogic.findFilter(player);

        if (newFilter.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.polluted_world.no_replacement_filter"));
            return;
        }

        returnOldFilterIfUseful(player, mask);

        GasMaskItem.installFilter(mask, newFilter);

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARMOR_EQUIP_CHAIN,
                SoundSource.PLAYERS,
                0.8F,
                1.2F
        );

        player.sendOverlayMessage(Component.translatable("message.polluted_world.filter_changed"));
    }

    private static void returnOldFilterIfUseful(ServerPlayer player, ItemStack mask) {
        if (!GasMaskItem.hasInstalledFilter(mask)) {
            return;
        }

        float remaining = GasMaskItem.getInstalledFilterRemaining(mask);

        if (remaining < 0.18F) {
            GasMaskItem.clearInstalledFilter(mask);
            return;
        }

        ItemStack oldFilter = GasMaskItem.removeInstalledFilter(mask);

        if (oldFilter.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(oldFilter)) {
            player.drop(oldFilter, false);
        }
    }
}