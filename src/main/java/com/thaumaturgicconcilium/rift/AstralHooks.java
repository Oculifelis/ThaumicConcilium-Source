package com.thaumaturgicconcilium.rift;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Optional phase 4 hooks into the Astral Monitor and the Lithographer, so compat code can feed them without the rift set
 * naming any partner mod.
 * <ul>
 * <li>{@link #addTarget}: extra items that point an Astral Monitor at a place (TC4: Automagy's Crystal Eye, read through its
 * {@code lookX/Y/Z/Dim} tag). A monitor holding such an item watches that place.</li>
 * <li>{@link BrainSource}: a block that gives the Lithographer its 4-bit "brain" value (TC4: the Red-Powered Mind's redstone
 * power). A watched block that is not a brain source keeps the old reading, the strongest redstone signal it gives out.</li>
 * </ul>
 */
public final class AstralHooks {
    private static final List<Function<ItemStack, Optional<GlobalPos>>> TARGETS = new CopyOnWriteArrayList<>();

    private AstralHooks() {
    }

    /** A block whose brain value the Lithographer shows for a watched place (0..15). */
    public interface BrainSource {
        int brainPower(ServerLevel level, BlockPos pos);
    }

    /** Registers a reader that turns an item into a watched place (empty when the item is not one, or not attuned). */
    public static void addTarget(Function<ItemStack, Optional<GlobalPos>> reader) {
        TARGETS.add(reader);
    }

    /** The place an item points at through a registered reader, if any. */
    public static Optional<GlobalPos> target(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (Function<ItemStack, Optional<GlobalPos>> reader : TARGETS) {
            Optional<GlobalPos> pos = reader.apply(stack);
            if (pos.isPresent()) {
                return pos;
            }
        }
        return Optional.empty();
    }
}
