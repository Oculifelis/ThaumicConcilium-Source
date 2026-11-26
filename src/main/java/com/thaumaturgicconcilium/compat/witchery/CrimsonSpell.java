package com.thaumaturgicconcilium.compat.witchery;

import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import dev.sterner.witchery.core.api.SymbologySpell;
import dev.sterner.witchery.feature.symbology.GridDirection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * One of the Crimson Cult's symbology spells (TC4 {@code WitcheryIntegration}'s {@code SymbolEffect}s), drawn with a
 * Witchery wand. The stroke path is the original's: TC4 Witchery's mystic branch recorded 0 for a stroke up, 1 down,
 * 2 right and 3 left (from {@code ItemMysticBranch.onUsingTick}).
 */
final class CrimsonSpell implements SymbologySpell {
    @FunctionalInterface
    interface Effect {
        void cast(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel);
    }

    private final ResourceLocation id;
    private final List<GridDirection> path;
    private final int cost;
    private final int maxLevel;
    private final Effect effect;

    CrimsonSpell(String name, byte[] strokes, int cost, int maxLevel, Effect effect) {
        this.id = ThaumaturgicConcilium.id(name);
        this.path = strokes(strokes);
        this.cost = cost;
        this.maxLevel = maxLevel;
        this.effect = effect;
    }

    /** TC4 stroke codes to Witchery 0.5 grid directions. */
    static List<GridDirection> strokes(byte[] strokes) {
        GridDirection[] out = new GridDirection[strokes.length];
        for (int i = 0; i < strokes.length; i++) {
            out[i] = switch (strokes[i]) {
                case 0 -> GridDirection.UP;
                case 1 -> GridDirection.DOWN;
                case 2 -> GridDirection.RIGHT;
                default -> GridDirection.LEFT;
            };
        }
        return List.of(out);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<GridDirection> getGridPath() {
        return path;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public void onUse(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
        effect.cast(level, player, look, spellLevel);
    }
}
