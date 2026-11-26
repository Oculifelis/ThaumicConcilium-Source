package com.thaumaturgicconcilium.spell.entity;

/**
 * Hook for entities a Charged Wisp does something special to when it comes within zapping range (TC4: the Brightest
 * One short-circuits and changes aspect; a chained rift opens toward the Brightest One). Implement it on the entity;
 * the wisp calls it instead of zapping that entity.
 */
public interface ChargedWispTarget {
    /**
     * The wisp reached this entity.
     *
     * @param wisp the wisp
     * @return true when the wisp is spent (it is removed), false to let it fly on
     */
    boolean onChargedWisp(ChargedWispEntity wisp);
}
