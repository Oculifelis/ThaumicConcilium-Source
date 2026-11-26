package com.thaumaturgicconcilium.rift.client;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.rift.AstralMonitorItem;
import com.thaumaturgicconcilium.rift.PartyStatusPayload;
import com.thaumaturgicconcilium.rift.RiftData;
import com.thaumaturgicconcilium.rift.TCRifts;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * TC4 {@code ClientEvents.renderAstralHole}: a lithographed player who has not used Coagulating Eye Drops sees the
 * astral hole in the top-left corner, a slowly blinking star over it, and around it the constellation glyphs of the four
 * imprinted targets (health, blood, mana and runic shielding, and the 4-bit value of a watched place). TC4's blink
 * shader faded star and glyphs in turn; that is the alpha here. Also draws the readings of a held Astral Monitor, which
 * TC4 showed on the monitor's 3D screen (simplified: a row of the same glyphs beside the hotbar).
 */
public final class AstralHoleLayer implements GuiLayer {
    public static final Identifier ID = ThaumaturgicConcilium.id("astral_hole");
    private static final Identifier HOLE = ThaumaturgicConcilium.id("textures/gui/astral_hole.png");
    private static final Identifier STAR = ThaumaturgicConcilium.id("textures/gui/star.png");
    private static final Identifier GLYPHS = ThaumaturgicConcilium.id("textures/gui/constellations.png");
    private static final int COL_RUNES = 0;
    private static final int COL_MANA = 1;
    private static final int COL_BLOOD = 2;
    private static final int COL_HEALTH = 3;
    private static final int COL_BRAIN = 4;
    // glyph positions per target, from TC4
    private static final int[][] HEALTH_AT = {{0, 20}, {20, 0}, {100, 10}, {15, 100}};
    private static final int[][] BLOOD_AT = {{14, 40}, {37, 17}, {90, 37}, {37, 98}};
    private static final int[][] MANA_AT = {{2, 55}, {53, 12}, {85, 56}, {53, 90}};
    private static final int[][] RUNES_AT = {{5, 70}, {73, 2}, {95, 72}, {73, 108}};

    private static int[] health = {-1, -1, -1, -1};
    private static int[] blood = {-1, -1, -1, -1};
    private static int[] mana = {-1, -1, -1, -1};
    private static int[] runes = {-1, -1, -1, -1};
    private static int[] brains = {-1, -1, -1, -1};

    static void receive(PartyStatusPayload p) {
        health = four(p.health());
        blood = four(p.blood());
        mana = four(p.mana());
        runes = four(p.runes());
        brains = four(p.brains());
    }

    private static int[] four(List<Integer> list) {
        int[] out = {-1, -1, -1, -1};
        for (int i = 0; i < Math.min(4, list.size()); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        heldMonitor(graphics, player.getMainHandItem());
        if (!player.getData(TCRifts.LITHOGRAPH).showing()) {
            return;
        }
        // TC4: the timer runs 0..60..0, one step every 3 ticks
        int phase = (player.tickCount / 3) % 120;
        int timer = phase <= 60 ? phase : 120 - phase;
        graphics.blit(RenderPipelines.GUI_TEXTURED, HOLE, 0, 0, 0.0F, 0.0F, 320, 243, 512, 384, 512, 384);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(80.0F, 80.0F);
        pose.rotate(timer / 30.0F * Mth.DEG_TO_RAD);
        pose.translate(-80.0F, -80.0F);
        graphics.blit(RenderPipelines.GUI_TEXTURED, STAR, 0, 0, 0.0F, 0.0F, 112, 112, 190, 190, 190, 190, alpha((60 - timer) / 60.0F));
        pose.popMatrix();

        int glyphTint = alpha(timer / 60.0F);
        for (int i = 0; i < 4; i++) {
            glyph(graphics, HEALTH_AT[i], COL_HEALTH, health[i], glyphTint);
            glyph(graphics, BLOOD_AT[i], COL_BLOOD, blood[i], glyphTint);
            glyph(graphics, MANA_AT[i], COL_MANA, mana[i], glyphTint);
            glyph(graphics, RUNES_AT[i], COL_RUNES, runes[i], glyphTint);
        }
        pose.pushMatrix();
        pose.rotate(timer / 30.0F * Mth.DEG_TO_RAD);
        for (int i = 0; i < 4; i++) {
            if (brains[i] == -1) {
                continue;
            }
            for (int bit = 3; bit >= 0; bit--) {
                int on = (brains[i] >> bit) & 1;
                int step = 36 - bit * 12;
                int[] at = switch (i) {
                    case 0 -> new int[]{5, 25 + step};
                    case 1 -> new int[]{30 + step, 3};
                    case 2 -> new int[]{102 - bit * 3, 20 + step};
                    default -> new int[]{30 + step, 100 - bit * 3};
                };
                glyph(graphics, at, COL_BRAIN, on, glyphTint);
            }
        }
        pose.popMatrix();
    }

    private static void glyph(GuiGraphicsExtractor graphics, int[] at, int column, int value, int tint) {
        if (value < 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, GLYPHS, at[0], at[1], 16.0F * column, 16.0F * Math.min(value, 15), 16, 16, 256, 256, tint);
    }

    private static int alpha(float a) {
        return ARGB.colorFromFloat(Mth.clamp(0.25F + 0.75F * a, 0.0F, 1.0F), 1.0F, 1.0F, 1.0F);
    }

    /** A held, loaded Astral Monitor: its target's glyphs in a row right of the hotbar. */
    private static void heldMonitor(GuiGraphicsExtractor graphics, ItemStack stack) {
        if (!(stack.getItem() instanceof AstralMonitorItem)) {
            return;
        }
        RiftData.Readings r = stack.get(TCRifts.ASTRAL_READINGS.get());
        if (r == null) {
            return;
        }
        int x = graphics.guiWidth() / 2 + 98;
        int y = graphics.guiHeight() - 19;
        int tint = 0xFFFFFFFF;
        glyph(graphics, new int[]{x, y}, COL_HEALTH, r.health(), tint);
        glyph(graphics, new int[]{x + 17, y}, COL_BLOOD, r.blood(), tint);
        glyph(graphics, new int[]{x + 34, y}, COL_MANA, r.mana(), tint);
        glyph(graphics, new int[]{x + 51, y}, COL_RUNES, r.runes(), tint);
    }
}
