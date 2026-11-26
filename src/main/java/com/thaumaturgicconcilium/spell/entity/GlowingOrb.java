package com.thaumaturgicconcilium.spell.entity;

/** An effect entity drawn as a glowing billboard (TC4's particle-sheet orbs) by the spell client's orb renderer. */
public interface GlowingOrb {
    /** Packed {@code 0xRRGGBB} tint. */
    int glowColor();

    /** Billboard size in blocks. */
    float glowScale();
}
