package com.licht_meilleur.polluted_world.item;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

public class GasMaskItem extends Item {
    public static final String FILTER_ID = "FilterId";
    public static final String FILTER_DAMAGE = "FilterDamage";
    public static final String FILTER_MAX_DAMAGE = "FilterMaxDamage";

    public GasMaskItem(Properties properties) {
        super(properties.component(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.HEAD).build()
        ));
    }

    public static boolean hasInstalledFilter(ItemStack gasMask) {
        CompoundTag tag = customTag(gasMask);
        return tag.contains(FILTER_ID) && tag.getInt(FILTER_MAX_DAMAGE).orElse(0) > 0;
    }

    public static float getInstalledFilterRemaining(ItemStack gasMask) {
        CompoundTag tag = customTag(gasMask);

        int max = tag.getInt(FILTER_MAX_DAMAGE).orElse(0);
        int damage = tag.getInt(FILTER_DAMAGE).orElse(0);

        if (max <= 0) return 0.0F;

        return Math.max(0.0F, Math.min(1.0F, 1.0F - ((float) damage / (float) max)));
    }

    public static boolean installFilter(ItemStack gasMask, ItemStack filter) {
        if (gasMask.isEmpty() || filter.isEmpty()) return false;

        Identifier filterId = BuiltInRegistries.ITEM.getKey(filter.getItem());

        CustomData data = gasMask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        gasMask.set(DataComponents.CUSTOM_DATA, data.update(tag -> {
            tag.putString(FILTER_ID, filterId.toString());
            tag.putInt(FILTER_DAMAGE, 0);
            tag.putInt(FILTER_MAX_DAMAGE, filter.getMaxDamage());
        }));

        filter.shrink(1);
        return true;
    }

    public static void damageInstalledFilter(ItemStack gasMask, int amount) {
        if (!hasInstalledFilter(gasMask)) return;

        CustomData data = gasMask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        gasMask.set(DataComponents.CUSTOM_DATA, data.update(tag -> {
            int damage = tag.getInt(FILTER_DAMAGE).orElse(0);
            int max = tag.getInt(FILTER_MAX_DAMAGE).orElse(0);

            int next = damage + amount;

            if (next >= max) {
                tag.remove(FILTER_ID);
                tag.remove(FILTER_DAMAGE);
                tag.remove(FILTER_MAX_DAMAGE);
            } else {
                tag.putInt(FILTER_DAMAGE, next);
            }
        }));
    }

    private static CompoundTag customTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static ItemStack removeInstalledFilter(ItemStack gasMask) {
        CompoundTag tag = customTag(gasMask);

        if (!hasInstalledFilter(gasMask)) {
            return ItemStack.EMPTY;
        }

        String id = tag.getString(FILTER_ID).orElse("");
        int damage = tag.getInt(FILTER_DAMAGE).orElse(0);
        int max = tag.getInt(FILTER_MAX_DAMAGE).orElse(0);

        Item item = BuiltInRegistries.ITEM
                .getOptional(Identifier.tryParse(id))
                .orElse(null);


        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack filter = new ItemStack(item);

        if (max > 0) {
            int safeDamage = Math.max(0, Math.min(damage, filter.getMaxDamage() - 1));
            filter.setDamageValue(safeDamage);
        }

        clearInstalledFilter(gasMask);

        return filter;
    }

    public static void clearInstalledFilter(ItemStack gasMask) {
        CustomData data = gasMask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        gasMask.set(DataComponents.CUSTOM_DATA, data.update(tag -> {
            tag.remove(FILTER_ID);
            tag.remove(FILTER_DAMAGE);
            tag.remove(FILTER_MAX_DAMAGE);
        }));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack gasMask = player.getItemInHand(hand);

        // オフハンド時はフィルター操作
        if (hand == InteractionHand.OFF_HAND) {

            if (!player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.CONSUME;
            }

            ItemStack removed = removeInstalledFilter(gasMask);

            if (removed.isEmpty()) {
                serverPlayer.sendOverlayMessage(
                        Component.literal("フィルターは装着されていません")
                );
                return InteractionResult.CONSUME;
            }

            if (!serverPlayer.getInventory().add(removed)) {
                serverPlayer.drop(removed, false);
            }

            serverPlayer.sendOverlayMessage(
                    Component.literal("フィルターを取り外した")
            );

            return InteractionResult.CONSUME;
        }

        // メインハンド時は装備
        return super.use(level, player, hand);
    }
}