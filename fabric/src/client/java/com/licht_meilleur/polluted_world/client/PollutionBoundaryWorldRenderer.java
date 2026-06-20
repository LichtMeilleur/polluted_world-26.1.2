package com.licht_meilleur.polluted_world.client;

import com.licht_meilleur.polluted_world.pollution.PollutionLogic;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class PollutionBoundaryWorldRenderer {
    private static final int MAX_DISTANCE = 96;
    private static final int STEP = 1;

    private PollutionBoundaryWorldRenderer() {
    }

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Level level = mc.level;

        List<BlockPos> hitPositions = findRayTips(player, level);

        if (hitPositions.isEmpty()) {
            return;
        }

        for (BlockPos hitPos : hitPositions) {
            drawCubeArea(context, hitPos, 1);
        }

        context.bufferSource().endBatch();

    }

    private static List<BlockPos> findRayTips(Player player, Level level) {
        List<BlockPos> hits = new ArrayList<>();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1, 0, 0);
        }

        Vec3 up = right.cross(look).normalize();

        boolean currentPolluted =
                PollutionLogic.isPollutedPosition(level, player.blockPosition());

        int columns = 13;
        int rows = 7;

        double spreadX = 1.25D;
        double spreadY = 0.75D;

        for (int y = 0; y < rows; y++) {
            double oy = ((double) y / (rows - 1) - 0.5D) * spreadY;

            for (int x = 0; x < columns; x++) {
                double ox = ((double) x / (columns - 1) - 0.5D) * spreadX;

                Vec3 ray = look
                        .add(right.scale(ox))
                        .add(up.scale(oy))
                        .normalize();

                BlockPos hit = findRayTip(player, level, eye, ray, currentPolluted);

                if (hit != null && !hits.contains(hit)) {
                    hits.add(hit);
                }
            }
        }

        return hits;
    }

    private static BlockPos findRayTip(
            Player player,
            Level level,
            Vec3 eye,
            Vec3 ray,
            boolean currentPolluted
    ) {
        for (int d = STEP; d <= MAX_DISTANCE; d += STEP) {
            Vec3 sample = eye.add(ray.scale(d));
            BlockPos pos = BlockPos.containing(sample);

            boolean polluted = PollutionLogic.isPollutedPosition(level, pos);

            if (!currentPolluted && polluted) {
                return pos;
            }

            if (currentPolluted && !polluted) {
                return pos;
            }
        }

        return null;
    }

    private static void drawCubeArea(LevelRenderContext context, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                drawCube(context, center.offset(dx, dy, 0));
            }
        }
    }

    private static void drawCube(LevelRenderContext context, BlockPos pos) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.position();

        float x0 = (float) (pos.getX() - cam.x);
        float y0 = (float) (pos.getY() - cam.y);
        float z0 = (float) (pos.getZ() - cam.z);

        float x1 = x0 + 1.0F;
        float y1 = y0 + 1.0F;
        float z1 = z0 + 1.0F;

        Matrix4f matrix = context.poseStack().last().pose();
        VertexConsumer vc = context.bufferSource().getBuffer(RenderTypes.debugQuads());

        int r = 255;
        int g = 255;
        int b = 0;
        int a = 90;

        // DOWN
        quad(vc, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        // UP
        quad(vc, matrix, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a);
        // NORTH
        quad(vc, matrix, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a);
        // SOUTH
        quad(vc, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        // WEST
        quad(vc, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // EAST
        quad(vc, matrix, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a);
    }

    private static void quad(
            VertexConsumer vc,
            Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int r, int g, int b, int a
    ) {
        vc.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        vc.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }
}