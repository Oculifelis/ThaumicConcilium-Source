package com.thaumaturgicconcilium.compat.gadomancy;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The sin-aspect aura effects TC4 Concilium gave Gadomancy's Aura Pylons for Forbidden Magic's aspects. Datapack
 * registry {@code thaumaturgicconcilium:sin_aura} (files in {@code data/<ns>/thaumaturgicconcilium/sin_aura/}): an aspect, what it
 * does, every how many ticks, how far from the pylon, and how much. Gadomancy's own aura effects are a closed set of
 * actions, so these run beside them from {@code compat.gadomancy.link.GadomancyLink}, on the same pylons. This class
 * names no Gadomancy type; the registry exists only when Gadomancy is loaded.
 */
public record SinAuras(ResourceKey<IAspect> aspect, Action action, int interval, int range, int amount) {
    public static final ResourceKey<Registry<SinAuras>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicConcilium.id("sin_aura"));
    public static final Codec<SinAuras> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(SinAuras::aspect),
            Action.CODEC.fieldOf("action").forGetter(SinAuras::action),
            Codec.intRange(1, 72000).optionalFieldOf("interval", 8).forGetter(SinAuras::interval),
            Codec.intRange(1, 64).optionalFieldOf("range", 8).forGetter(SinAuras::range),
            Codec.intRange(0, 72000).optionalFieldOf("amount", 1).forGetter(SinAuras::amount)
    ).apply(i, SinAuras::new));

    public enum Action implements StringRepresentable {
        /** Infernus: dropped items in range turn into what a furnace would make of them. */
        SMELT("smelt"),
        /** Luxuria: animals waiting to breed again are ready at once. */
        RESET_BREEDING("reset_breeding"),
        /** Gula: players are fed {@code amount} hunger and saturation. */
        FEED("feed"),
        /** Ira: monsters turn on a random creature near them (8 blocks). */
        PROVOKE("provoke"),
        /** Invidia: creatures other than players drop what they hold. */
        DISARM("disarm"),
        /** Desidia: every effect on a creature lasts {@code amount} ticks longer. */
        EXTEND_EFFECTS("extend_effects"),
        /** Superbia: monsters are thrown away from the pylon. */
        REPEL("repel");

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);
        private final String name;

        Action(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> event.dataPackRegistry(REGISTRY_KEY, CODEC));
    }

    /** Runs this aura once around {@code centre} (the pylon's lowest block). */
    public void apply(ServerLevel level, BlockPos centre) {
        Vec3 c = Vec3.atCenterOf(centre);
        AABB box = new AABB(centre).inflate(range);
        switch (action) {
            case SMELT -> {
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                    ItemStack stack = item.getItem();
                    SingleRecipeInput input = new SingleRecipeInput(stack);
                    level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).ifPresent(r -> {
                        ItemStack out = r.value().assemble(input);
                        if (!out.isEmpty()) {
                            item.setItem(out.copyWithCount(Math.min(out.getMaxStackSize(), out.getCount() * stack.getCount())));
                        }
                    });
                }
            }
            case RESET_BREEDING -> {
                for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, box)) {
                    if (mob.getAge() > 0) {
                        mob.setAge(0);
                    }
                }
            }
            case FEED -> {
                for (Player player : level.getEntitiesOfClass(Player.class, box)) {
                    player.getFoodData().eat(amount, 1.0F);
                }
            }
            case PROVOKE -> {
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy)) {
                    List<Mob> near = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(8.0), m -> m != mob && m.isAlive());
                    if (!near.isEmpty()) {
                        mob.setLastHurtByMob(near.get(level.getRandom().nextInt(near.size())));
                    }
                }
            }
            case DISARM -> {
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> !(e instanceof Player))) {
                    ItemStack held = living.getMainHandItem();
                    if (!held.isEmpty()) {
                        living.spawnAtLocation(level, held.copy(), 0.5F);
                        living.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    }
                }
            }
            case EXTEND_EFFECTS -> {
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
                    for (MobEffectInstance e : new ArrayList<>(living.getActiveEffects())) {
                        if (!e.isInfiniteDuration()) {
                            living.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration() + amount, e.getAmplifier(), e.isAmbient(),
                                    e.isVisible(), e.showIcon()));
                        }
                    }
                }
            }
            case REPEL -> {
                // TC4 aimed at (dx, entity y, dz) * (2, 1, 2), so a mob was flung up by its own height above 0; it is now
                // pushed straight away from the pylon, level, at TC4's speed of 1.
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy)) {
                    Vec3 away = new Vec3(mob.getX() - c.x, 0.0, mob.getZ() - c.z);
                    if (away.lengthSqr() < 1.0E-4) {
                        continue;
                    }
                    mob.setDeltaMovement(away.normalize());
                    mob.hurtMarked = true;
                }
            }
        }
    }
}
