package com.thaumaturgicconcilium.mob.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Render state of the Concilium biped mobs: the few extras {@link TCBipedModel} needs on top of a humanoid. */
public class TCBipedState extends HumanoidRenderState {
    /** Quicksilver wobble strength (0 = none). */
    public float wobble;
    /** Overall size (Quicksilver Elemental: health / 30). */
    public float size = 1.0F;
    /** Alive Limb part 0-5, or -1 for a whole body. */
    public int limb = -1;
    /** Vengeful Golem's squat head (TC4 scaled it 0.65 x 0.75). */
    public boolean golemHead;
}
