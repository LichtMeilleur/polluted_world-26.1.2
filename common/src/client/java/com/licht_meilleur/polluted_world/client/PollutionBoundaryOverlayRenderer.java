package com.licht_meilleur.polluted_world.client;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PollutionBoundaryOverlayRenderer {

    private static final Identifier TEXTURE =
            PollutedWorldMod.id("textures/gui/pollution_boundary.png");

    // 画像サイズに合わせてください
    private static final int TEX_W = 1536;
    private static final int TEX_H = 1024;

    // 仮想の板を置きたい最大距離
    private static final int SCREEN_DISTANCE = 96;

    // レイ判定間隔
    private static final int STEP = 2;

    // これ以下だとかなり近い扱い
    private static final int NEAR_DISTANCE = 8;

    private PollutionBoundaryOverlayRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui) return;
        if (mc.player == null) return;
        if (mc.level == null) return;

        Player player = mc.player;
        Level level = mc.level;

        int currentPollution = PollutionLogic.getPollutionLevel(
                level,
                player.blockPosition()
        );

        int boundaryDistance;

        if (currentPollution > 0) {
            // 汚染区画内では、板が常にかなり近い扱い
            boundaryDistance = NEAR_DISTANCE;
        } else {
            boundaryDistance = findPollutionDistanceInView(player, level);

            if (boundaryDistance < 0) {
                return;
            }
        }

        drawPushedScreen(graphics, boundaryDistance);
    }

    private static int findPollutionDistanceInView(Player player, Level level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        for (int d = STEP; d <= SCREEN_DISTANCE; d += STEP) {
            Vec3 sample = eye.add(look.scale(d));

            int pollution = PollutionLogic.getPollutionLevel(
                    level,
                    net.minecraft.core.BlockPos.containing(sample)
            );

            if (pollution > 0) {
                return d;
            }
        }

        return -1;
    }

    private static void drawPushedScreen(GuiGraphicsExtractor graphics, int distance) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float t = 1.0F - Mth.clamp(
                (float) distance / (float) SCREEN_DISTANCE,
                0.0F,
                1.0F
        );

        // 遠い時は薄く小さく、近い時は濃く大きく
        float alpha = Mth.lerp(t, 0.12F, 0.68F);
        float scale = Mth.lerp(t, 0.55F, 2.15F);

        int drawW = Math.round(width * scale);
        int drawH = Math.round(height * scale);

        int x = (width - drawW) / 2;
        int y = (height - drawH) / 2;

        int color = ARGB.white(alpha);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                0.0F,
                0.0F,
                drawW,
                drawH,
                TEX_W,
                TEX_H,
                TEX_W,
                TEX_H,
                color
        );
    }
}