package com.thaumaturgicconcilium.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.mob.AliveLimbEntity;
import com.thaumaturgicconcilium.mob.QuicksilverElementalEntity;
import com.thaumaturgicconcilium.mob.VengefulGolemEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Renderer for the Concilium biped mobs: the original's CC0 entity sheet on {@link TCBipedModel}, with worn armour and
 * held items. The Dissolved variant draws the whole body with the end-portal starfield (the original's ender shader);
 * the Quicksilver Elemental is scaled by its health and wobbles.
 */
public class TCBipedRenderer<T extends Mob> extends HumanoidMobRenderer<T, TCBipedModel<T>> {
    private final ResourceLocation texture;
    private final boolean ender;
    private final float baseShadow;

    public TCBipedRenderer(EntityRendererProvider.Context context, String texture, float shadow, boolean ender) {
        super(context, new TCBipedModel<>(context.bakeLayer(TCMobsClient.BIPED)), shadow);
        this.texture = ThaumaturgicConcilium.id("textures/entity/" + texture + ".png");
        this.ender = ender;
        this.baseShadow = shadow;
        if (!ender) {
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        }
    }

    public static <T extends Mob> EntityRendererProvider<T> of(String texture) {
        return ctx -> new TCBipedRenderer<>(ctx, texture, 0.5F, false);
    }

    public static <T extends Mob> EntityRendererProvider<T> ender() {
        return ctx -> new TCBipedRenderer<>(ctx, "thaumaturge", 0.5F, true);
    }

    private static float size(Mob entity) {
        return entity instanceof QuicksilverElementalEntity qs ? qs.sizeScale() : 1.0F;
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        model.wobble = entity instanceof QuicksilverElementalEntity ? 1.0F : 0.0F;
        model.limb = entity instanceof AliveLimbEntity limb ? limb.limbType() : -1;
        model.golemHead = entity instanceof VengefulGolemEntity;
        shadowRadius = baseShadow * size(entity);
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTicks) {
        float size = size(entity);
        if (size != 1.0F) {
            poseStack.scale(size, size, size);
        }
    }

    @Override
    protected @Nullable RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (ender && bodyVisible) {
            return RenderType.endPortal();
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
