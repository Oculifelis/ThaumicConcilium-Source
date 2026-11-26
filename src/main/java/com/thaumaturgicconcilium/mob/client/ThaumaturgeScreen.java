package com.thaumaturgicconcilium.mob.client;

import com.thaumaturgicconcilium.mob.ThaumaturgeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** The Thaumaturge's trade table, drawn on Thaumaturge's Pech trade sheet (same slots, same trade button). */
public class ThaumaturgeScreen extends AbstractContainerScreen<ThaumaturgeMenu> {
    private static final Identifier SHEET = Identifier.parse("thaumaturge:textures/gui/gui_pech.png");
    private static final int BUTTON_X = 67;
    private static final int BUTTON_Y = 24;
    private static final int BUTTON_SIZE = 25;

    public ThaumaturgeScreen(ThaumaturgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 175, 232);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SHEET, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.canTrade()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SHEET, leftPos + BUTTON_X, topPos + BUTTON_Y, 176.0F, 0.0F, BUTTON_SIZE, BUTTON_SIZE, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double rx = event.x() - (leftPos + BUTTON_X);
        double ry = event.y() - (topPos + BUTTON_Y);
        if (event.button() == 0 && rx >= 0 && ry >= 0 && rx < BUTTON_SIZE && ry < BUTTON_SIZE && menu.canTrade() && minecraft != null
                && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ThaumaturgeMenu.TRADE_BUTTON);
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.0F);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
