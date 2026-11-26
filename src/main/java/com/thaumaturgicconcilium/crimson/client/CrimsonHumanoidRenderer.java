package com.thaumaturgicconcilium.crimson.client;

import com.leclowndu93150.thaumaturge.client.entity.TTModelLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * A TC4 biped (64x32 sheet, on Thaumaturge's own cultist model layer) with worn armour and held items drawn by the
 * usual layers: the crimson cultists' robes and plate, the Paranoid Warrior's armour and blade.
 */
public class CrimsonHumanoidRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private final Identifier texture;

    public CrimsonHumanoidRenderer(EntityRendererProvider.Context context, Identifier texture, float shadow) {
        super(context, new HumanoidModel<>(context.bakeLayer(TTModelLayers.CULTIST)), shadow);
        this.texture = texture;
        addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
