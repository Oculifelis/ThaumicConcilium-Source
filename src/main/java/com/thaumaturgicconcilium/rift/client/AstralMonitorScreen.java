package com.thaumaturgicconcilium.rift.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.AstralMonitorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC4 {@code AstralMonitorGUI}: the original's sheet with the one slot for a carved wand or a watched-place token. */
public class AstralMonitorScreen extends AbstractContainerScreen<AstralMonitorMenu> {
    private static final Identifier SHEET = ThaumaturgicConcilium.id("textures/gui/astral_monitor.png");

    public AstralMonitorScreen(AstralMonitorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SHEET, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }
}
