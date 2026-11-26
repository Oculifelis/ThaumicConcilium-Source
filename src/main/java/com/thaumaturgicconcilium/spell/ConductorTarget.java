package com.thaumaturgicconcilium.spell;

import net.minecraft.world.entity.player.Player;

/**
 * Hook for entities the Vis Conductor's Dematerialization does something special to when the caster lets go while
 * aiming at them with a crystal locked (TC4: a rift collapses by {@code amount * (1 + potency)} when the crystal was
 * Auram; the Peeler Of Matter is destroyed and taints the ground). Implement it on the entity.
 */
public interface ConductorTarget {
    /**
     * The caster released a Dematerialization charge at this entity.
     *
     * @param caster  the player
     * @param lock    the crystal lock, with the charge drained this hold
     * @param potency TC4 potency of the cast
     * @return true when handled, so the plain magic damage is skipped
     */
    boolean onDematerialize(Player caster, ConductorLock.State lock, float potency);
}
