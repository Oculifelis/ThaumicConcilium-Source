package com.thaumaturgicconcilium.rift.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.AstralMonitorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** TC4 {@code AstralMonitorGUI}: the original's sheet with the one slot for a carved wand or a watched-place token. */
public class AstralMonitorScreen extends AbstractContainerScreen<AstralMonitorMenu> {
    private static final ResourceLocation SHEET = ThaumaturgicConcilium.id("textures/gui/astral_monitor.png");

    public AstralMonitorScreen(AstralMonitorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(SHEET, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int xm, int ym) {
    }
}
