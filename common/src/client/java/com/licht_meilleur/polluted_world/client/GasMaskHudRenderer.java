package com.licht_meilleur.polluted_world.client;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.item.GasMaskItem;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GasMaskHudRenderer {

    private static final Identifier MASK_76_100 =
            PollutedWorldMod.id("textures/gui/gasmask/gas_mask_screen76_100.png");
    private static final Identifier MASK_51_75 =
            PollutedWorldMod.id("textures/gui/gasmask/gas_mask_screen51_75.png");
    private static final Identifier MASK_26_50 =
            PollutedWorldMod.id("textures/gui/gasmask/gas_mask_screen26_50.png");
    private static final Identifier MASK_1_25 =
            PollutedWorldMod.id("textures/gui/gasmask/gas_mask_screen1_25.png");

    private static final Identifier ICON =
            PollutedWorldMod.id("textures/gui/gasmask/gas_mask_icon.png");

    private GasMaskHudRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui) return;
        if (minecraft.player == null) return;

        Player player = minecraft.player;
        ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);

        if (!PollutionLogic.isGasMaskStack(mask)) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        Identifier overlay = overlayFor(mask);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                overlay,
                0,
                0,
                0,
                0,
                width,
                height,
                width,
                height
        );

        renderFilterHud(graphics, player);
    }

    private static Identifier overlayFor(ItemStack mask) {
        int max = mask.getMaxDamage();

        if (max <= 0) {
            return MASK_76_100;
        }

        float remaining = 1.0F - ((float) mask.getDamageValue() / (float) max);

        if (remaining > 0.75F) return MASK_76_100;
        if (remaining > 0.50F) return MASK_51_75;
        if (remaining > 0.25F) return MASK_26_50;

        return MASK_1_25;
    }

    private static void renderFilterHud(GuiGraphicsExtractor graphics, Player player) {
        ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int iconSize = 14;
        int barWidth = 54;
        int barHeight = 5;

        int barX = width / 2 + 24;
        int barY = height - 49;

        int iconGap = 3;
        int iconX = barX - iconSize - iconGap;
        int iconY = barY - 5;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON,
                iconX,
                iconY,
                0.0F,
                0.0F,
                iconSize,
                iconSize,
                64,
                64,
                64,
                64
        );

        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xAA000000);
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA222222);

        if (!GasMaskItem.hasInstalledFilter(mask)) {
            return;
        }

        float remainingFilter =
                com.licht_meilleur.polluted_world.item.GasMaskItem.getInstalledFilterRemaining(mask);

        remainingFilter = Math.max(0.0F, Math.min(1.0F, remainingFilter));

        int filledWidth = Math.round(barWidth * remainingFilter);

        graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFFFFFFFF);
    }

}