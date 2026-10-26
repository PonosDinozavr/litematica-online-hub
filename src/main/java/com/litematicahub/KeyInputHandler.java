package com.litematicahub;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static KeyMapping openHubKey;

    public static void registerKeyInputs() {
        openHubKey = new KeyMapping(
                "key.litematicahub.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                Category.register(Identifier.fromNamespaceAndPath("litematicahub", "hub"))
        );
        registerKeyMapping(openHubKey);
        ModInit.LOGGER.info("Registered openHubKey, default key K");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHubKey.consumeClick()) {
                ModInit.LOGGER.info("openHubKey clicked, opening LitematicaHubScreen");
                client.setScreenAndShow(new LitematicaHubScreen(null));
            }
        });
    }

    /**
     * Registers the key binding so it appears in Options -> Controls and can be rebound.
     * Fabric moved this helper between packages across versions, so we try the current
     * location and fall back to the older ones, then to vanilla options as a last resort.
     */
    private static void registerKeyMapping(KeyMapping mapping) {
        try {
            Class<?> helper = Class.forName("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper");
            helper.getMethod("registerKeyMapping", KeyMapping.class).invoke(null, mapping);
            ModInit.LOGGER.info("Key binding registered via KeyMappingHelper");
            return;
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        try {
            Class<?> helper = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
            helper.getMethod("registerKeyBinding", KeyMapping.class).invoke(null, mapping);
            ModInit.LOGGER.info("Key binding registered via KeyBindingHelper (legacy)");
            return;
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        // vanilla fallback: still ends up in the controls screen, so it stays rebindable
        try {
            Class<?> optionsClass = Class.forName("net.minecraft.client.Options");
            for (String name : new String[]{"addKeyMapping", "registerKeyMapping", "addKey"}) {
                try {
                    optionsClass.getMethod(name, KeyMapping.class)
                            .invoke(Minecraft.getInstance().options, mapping);
                    ModInit.LOGGER.info("Key binding registered through vanilla Options." + name);
                    return;
                } catch (NoSuchMethodException ignored) {
                }
            }
            ModInit.LOGGER.warn("No known registration method on vanilla Options");
        } catch (Throwable e) {
            ModInit.LOGGER.error("Could not register the hub key binding", e);
        }
    }

    public static void register() {
        registerKeyInputs();
    }
}
