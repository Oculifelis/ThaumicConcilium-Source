package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.AliveLimbEntity;
import com.thaumaturgicconcilium.mob.QuicksilverElementalEntity;
import com.thaumaturgicconcilium.mob.VengefulGolemEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the Concilium biped mobs: the original's CC0 entity sheet on {@link TCBipedModel}, with worn armour and
 * held items. The Dissolved variant draws the whole body with the end-portal starfield (the original's ender shader);
 * the Quicksilver Elemental is scaled by its health and wobbles.
 */
public class TCBipedRenderer<T extends Mob> extends HumanoidMobRenderer<T, TCBipedState, TCBipedModel> {
    private final Identifier texture;
    private final boolean ender;

    public TCBipedRenderer(EntityRendererProvider.Context context, String texture, float shadow, boolean ender) {
        super(context, new TCBipedModel(context.bakeLayer(TCMobsClient.BIPED)), shadow);
        this.texture = ThaumaturgicConcilium.id("textures/entity/" + texture + ".png");
        this.ender = ender;
        if (!ender) {
            addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                    context.getEquipmentRenderer()));
        }
    }

    public static <T extends Mob> EntityRendererProvider<T> of(String texture) {
        return ctx -> new TCBipedRenderer<>(ctx, texture, 0.5F, false);
    }

    public static <T extends Mob> EntityRendererProvider<T> ender() {
        return ctx -> new TCBipedRenderer<>(ctx, "thaumaturge", 0.5F, true);
    }

    @Override
    public TCBipedState createRenderState() {
        return new TCBipedState();
    }

    @Override
    public void extractRenderState(T entity, TCBipedState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.size = entity instanceof QuicksilverElementalEntity qs ? qs.sizeScale() : 1.0F;
        state.wobble = entity instanceof QuicksilverElementalEntity ? 1.0F : 0.0F;
        state.limb = entity instanceof AliveLimbEntity limb ? limb.limbType() : -1;
        state.golemHead = entity instanceof VengefulGolemEntity;
    }

    @Override
    protected void scale(TCBipedState state, PoseStack poseStack) {
        if (state.size != 1.0F) {
            poseStack.scale(state.size, state.size, state.size);
        }
    }

    @Override
    protected float getShadowRadius(TCBipedState state) {
        return super.getShadowRadius(state) * state.size;
    }

    @Override
    protected @Nullable RenderType getRenderType(TCBipedState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (ender && bodyVisible) {
            return RenderTypes.endPortal();
        }
        return super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    public Identifier getTextureLocation(TCBipedState state) {
        return texture;
    }
}
