package com.thaumaturgicconcilium.rift.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.LithographerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC4 {@code LithographerGUI}: the original's sheet, four monitor slots around the lens; no labels, as in TC4. */
public class LithographerScreen extends AbstractContainerScreen<LithographerMenu> {
    private static final Identifier SHEET = ThaumaturgicConcilium.id("textures/gui/lithographer.png");

    public LithographerScreen(LithographerMenu menu, Inventory inventory, Component title) {
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
