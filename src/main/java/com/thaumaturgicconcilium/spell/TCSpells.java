package com.thaumaturgicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.spell.effect.AlchemistSpreeEffect;
import com.thaumaturgicconcilium.spell.effect.CoolingEffect;
import com.thaumaturgicconcilium.spell.effect.EtherealManipulatorEffect;
import com.thaumaturgicconcilium.spell.effect.ImpulseEffect;
import com.thaumaturgicconcilium.spell.effect.PositiveBurstEffect;
import com.thaumaturgicconcilium.spell.effect.ReflectionEffect;
import com.thaumaturgicconcilium.spell.effect.ScribingEffect;
import com.thaumaturgicconcilium.spell.effect.SignalEffect;
import com.thaumaturgicconcilium.spell.effect.SpotlightEffect;
import com.thaumaturgicconcilium.spell.effect.TaintAnimationEffect;
import com.thaumaturgicconcilium.spell.effect.VisConductorEffect;
import com.thaumaturgicconcilium.spell.effect.WarpExtractionEffect;
import com.thaumaturgicconcilium.spell.effect.WrathAttractionEffect;
import com.thaumaturgicconcilium.spell.entity.ChargedWispEntity;
import com.thaumaturgicconcilium.spell.entity.CompressedBlastEntity;
import com.thaumaturgicconcilium.spell.entity.ConcentratedWarpChargeEntity;
import com.thaumaturgicconcilium.spell.entity.DistortionEffectEntity;
import com.thaumaturgicconcilium.spell.entity.PositiveBurstOrbEntity;
import com.thaumaturgicconcilium.spell.entity.WrathEffectEntity;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Thaumic Concilium's foci as Thaumaturge spell effects. Every TC4 focus is one behaviour type; its rank-5 special
 * upgrades are further parts of the same type with other JSON settings (so Refreeze is its own part, made of the
 * {@code cooling} behaviour with frost shards turned on). Every number is a field of the part JSON in
 * {@code data/thaumaturgicconcilium/thaumaturge/spell_part/}; the standard TC4 upgrades are Thaumaturge's own modifiers,
 * read back from the cast's power, radius and duration (see {@link SpellUtil}).
 *
 * <p>Placed straight under the origin, an effect gets the caster as its target and does what the TC4 focus did with
 * the caster's aim. Behind a delivery it acts from or on the point the delivery reached. The hold-to-use foci are
 * channelled spells; what TC4 did on release is run when the caster lets go (see {@link Channel}).
 */
public final class TCSpells {
    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, ThaumaturgicConcilium.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ThaumaturgicConcilium.MODID);

    // ---- behaviours ----
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<SignalEffect>> SIGNAL = effect("signal", SignalEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ImpulseEffect>> IMPULSE = effect("impulse", ImpulseEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<PositiveBurstEffect>> POSITIVE_BURST = effect("positive_burst",
            PositiveBurstEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<VisConductorEffect>> VIS_CONDUCTOR = effect("vis_conductor",
            VisConductorEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<CoolingEffect>> COOLING = effect("cooling", CoolingEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<WrathAttractionEffect>> WRATH_ATTRACTION = effect("wrath_attraction",
            WrathAttractionEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<TaintAnimationEffect>> TAINT_ANIMATION = effect("taint_animation",
            TaintAnimationEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ReflectionEffect>> REFLECTION = effect("reflection", ReflectionEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ScribingEffect>> SCRIBING = effect("scribing", ScribingEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<EtherealManipulatorEffect>> ETHEREAL_MANIPULATOR = effect(
            "ethereal_manipulator", EtherealManipulatorEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<SpotlightEffect>> SPOTLIGHT = effect("spotlight", SpotlightEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<WarpExtractionEffect>> WARP_EXTRACTION = effect("warp_extraction",
            WarpExtractionEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<AlchemistSpreeEffect>> ALCHEMIST_SPREE = effect("alchemist_spree",
            AlchemistSpreeEffect.CODEC);

    // ---- entities ----
    public static final DeferredHolder<EntityType<?>, EntityType<CompressedBlastEntity>> COMPRESSED_BLAST = ENTITIES.register("compressed_blast",
            () -> EntityType.Builder.<CompressedBlastEntity>of(CompressedBlastEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(8).updateInterval(2).build(key("compressed_blast")));
    public static final DeferredHolder<EntityType<?>, EntityType<PositiveBurstOrbEntity>> POSITIVE_BURST_ORB = ENTITIES.register("positive_burst_orb",
            () -> EntityType.Builder.<PositiveBurstOrbEntity>of(PositiveBurstOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(6).updateInterval(2).build(key("positive_burst_orb")));
    public static final DeferredHolder<EntityType<?>, EntityType<ConcentratedWarpChargeEntity>> WARP_CHARGE = ENTITIES.register(
            "concentrated_warp_charge", () -> EntityType.Builder.<ConcentratedWarpChargeEntity>of(ConcentratedWarpChargeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(1).build(key("concentrated_warp_charge")));
    public static final DeferredHolder<EntityType<?>, EntityType<ChargedWispEntity>> CHARGED_WISP = ENTITIES.register("charged_wisp",
            () -> EntityType.Builder.<ChargedWispEntity>of(ChargedWispEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(8).updateInterval(2).build(key("charged_wisp")));
    public static final DeferredHolder<EntityType<?>, EntityType<WrathEffectEntity>> WRATH_EFFECT = ENTITIES.register("wrath_effect",
            () -> EntityType.Builder.<WrathEffectEntity>of(WrathEffectEntity::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(20).build(key("wrath_effect")));
    public static final DeferredHolder<EntityType<?>, EntityType<DistortionEffectEntity>> DISTORTION_EFFECT = ENTITIES.register("distortion_effect",
            () -> EntityType.Builder.<DistortionEffectEntity>of(DistortionEffectEntity::new, MobCategory.MISC).sized(1.0F, 1.0F)
                    .clientTrackingRange(6).updateInterval(2).build(key("distortion_effect")));

    // ---- attachments ----
    /** The crystal a player's Vis Conductor is draining; read it through {@link ConductorLock}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<ConductorLock.State>>> CONDUCTOR_LOCK = ATTACHMENTS.register(
            "conductor_lock", () -> AttachmentType.<Optional<ConductorLock.State>>builder(Optional::empty)
                    .serialize(ConductorLock.State.CODEC.optionalFieldOf("lock")).build());

    private TCSpells() {
    }

    private static <B extends SpellBehavior> DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<B>> effect(String name, MapCodec<B> codec) {
        return BEHAVIORS.register(name, () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, codec));
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicConcilium.id(name));
    }

    public static void register(IEventBus modBus) {
        BEHAVIORS.register(modBus);
        ENTITIES.register(modBus);
        ATTACHMENTS.register(modBus);
        TCSpellEvents.register(NeoForge.EVENT_BUS);
    }
}
