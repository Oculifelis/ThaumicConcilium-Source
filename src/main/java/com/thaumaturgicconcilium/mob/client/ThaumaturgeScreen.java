package com.thaumaturgicconcilium.mob.client;

import com.thaumaturgicconcilium.mob.ThaumaturgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** The Thaumaturge's trade table, drawn on Thaumaturge's Pech trade sheet (same slots, same trade button). */
public class ThaumaturgeScreen extends AbstractContainerScreen<ThaumaturgeMenu> {
    private static final ResourceLocation SHEET = ResourceLocation.parse("thaumaturge:textures/gui/gui_pech.png");
    private static final int BUTTON_X = 67;
    private static final int BUTTON_Y = 24;
    private static final int BUTTON_SIZE = 25;

    public ThaumaturgeScreen(ThaumaturgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 175;
        imageHeight = 232;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(SHEET, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.canTrade()) {
            graphics.blit(SHEET, leftPos + BUTTON_X, topPos + BUTTON_Y, 176.0F, 0.0F, BUTTON_SIZE, BUTTON_SIZE, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int xm, int ym) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double rx = mouseX - (leftPos + BUTTON_X);
        double ry = mouseY - (topPos + BUTTON_Y);
        if (button == 0 && rx >= 0 && ry >= 0 && rx < BUTTON_SIZE && ry < BUTTON_SIZE && menu.canTrade() && minecraft != null
                && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ThaumaturgeMenu.TRADE_BUTTON);
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.0F);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
