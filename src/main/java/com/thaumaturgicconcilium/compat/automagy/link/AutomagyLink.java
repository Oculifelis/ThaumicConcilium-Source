package com.thaumaturgicconcilium.compat.automagy.link;

import com.thaumaturgicconcilium.rift.AstralHooks;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import com.automagy.AMRegistry;

/**
 * Automagy link (TC4 Concilium {@code Integration.crystalEye}): an attuned Crystalline Eye fits the Astral Monitor like a
 * Lithographer's own lens, showing the place the eye is bound to. The Red-Powered Mind itself is in
 * {@code compat.horizons} (it needs Horizons to be made and Automagy's redstone parts in its recipe).
 */
public final class AutomagyLink {
    private AutomagyLink() {
    }

    public static void register() {
        AstralHooks.addTarget(stack -> stack.is(AMRegistry.CRYSTALLINE_EYE.get())
                ? Optional.ofNullable(stack.get(AMRegistry.LINK.get())) : Optional.<GlobalPos>empty());
    }
}
