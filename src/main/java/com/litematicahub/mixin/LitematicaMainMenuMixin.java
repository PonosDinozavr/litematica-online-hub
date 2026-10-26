package com.litematicahub.mixin;

import com.litematicahub.LitematicaHubScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the gallery entry point to the actual MaLiLib-based Litematica menu. */
import com.litematicahub.ModInit;
import fi.dy.masa.litematica.gui.GuiMainMenu;
@Mixin(GuiMainMenu.class)
public abstract class LitematicaMainMenuMixin extends GuiBase {
    @Inject(method = "initGui", at = @At("TAIL"))
    private void litematicaOnlineHub$addButton(CallbackInfo callbackInfo) {
        ModInit.LOGGER.info("LitematicaMainMenuMixin initialized, adding Online Gallery button in top-right");
        int buttonWidth = 140;
        int x = getScreenWidth() - buttonWidth - 12;
        int y = 12;
        ButtonGeneric button = new ButtonGeneric(x, y, buttonWidth, 20, "Online Gallery");
        addButton(button, new HubButtonListener((net.minecraft.client.gui.screens.Screen) (Object) this));
    }

    @Unique
    private static final class HubButtonListener implements IButtonActionListener {
        private final net.minecraft.client.gui.screens.Screen hubParent;

        private HubButtonListener(net.minecraft.client.gui.screens.Screen hubParent) {
            this.hubParent = hubParent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            Minecraft.getInstance().setScreenAndShow(new LitematicaHubScreen(hubParent));
        }
    }
}
