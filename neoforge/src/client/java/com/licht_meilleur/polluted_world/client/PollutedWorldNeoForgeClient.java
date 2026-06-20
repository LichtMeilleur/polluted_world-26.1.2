package com.licht_meilleur.polluted_world.client;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.licht_meilleur.polluted_world.network.ChangeFilterPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = PollutedWorldMod.MOD_ID, value = Dist.CLIENT)
public final class PollutedWorldNeoForgeClient {

    private static final KeyMapping CHANGE_FILTER = new KeyMapping(
            "key.polluted_world.change_filter",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KeyMapping.Category.MISC
    );

    private PollutedWorldNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHANGE_FILTER);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                PollutedWorldMod.id("gas_mask_hud"),
                (graphics, tickCounter) -> GasMaskHudRenderer.render(graphics)
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (CHANGE_FILTER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ChangeFilterPayload());
        }
    }
}