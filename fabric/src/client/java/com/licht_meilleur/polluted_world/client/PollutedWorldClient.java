package com.licht_meilleur.polluted_world.client;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.network.ChangeFilterPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class PollutedWorldClient implements ClientModInitializer {
    private static KeyMapping CHANGE_FILTER;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                PollutedWorldMod.id("gas_mask_hud"),
                (graphics, tickCounter) -> GasMaskHudRenderer.render(graphics)
        );

        CHANGE_FILTER = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.polluted_world.change_filter",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CHANGE_FILTER.consumeClick()) {
                ClientPlayNetworking.send(new ChangeFilterPayload());
            }
        });

        /*
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                PollutedWorldMod.id("pollution_boundary_overlay"),
                (graphics, tickCounter) -> PollutionBoundaryOverlayRenderer.render(graphics)
        );

         */
        /*
        LevelRenderEvents.END_MAIN.register(context -> {
            PollutionBoundaryWorldRenderer.render(context);
        });

         */

    }
}