package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The "ethereal" state (TC4 TCPlayerCapabilities + TCEntityEventHandler): a player who has passed through a Peeler of
 * Matter, or toggled a full Pontifex robe, is intangible.
 * <ul>
 * <li>No collision (the {@code EtherealPlayerMixin} sets noclip each tick) and no gravity unless sneaking.</li>
 * <li>No damage and no healing. Magic damage and falling out of the world still dispel one point of flesh.</li>
 * <li>Touching the world with anything but a wand (right or left click, using an item, attacking) dispels one point of
 * flesh, until all six Protolimbs are absorbed.</li>
 * <li>Each point of flesh lost lowers the player's health cap by one; at the cap of zero the player dies.</li>
 * <li>With six Protolimbs, a wand used below the bottom of the world makes Solid Void.</li>
 * </ul>
 * Dying while ethereal ends it; the flesh debt is kept, but never more than the full health minus two.
 */
public final class Ethereal {
    private static final ResourceLocation RIFT_GEM = ThaumaturgicConcilium.id("rift_gem");
    private static final int COMMAND_RANGE = 32;
    private static final int COMMAND_TICKS = 100;
    private static final int FULL_LIMBS = 6;

    private Ethereal() {
    }

    // ---------------------------------------------------------------- state
    public static boolean isEthereal(Player player) {
        return player.getData(TCBosses.ETHEREAL);
    }

    public static void setEthereal(Player player, boolean value) {
        player.setData(TCBosses.ETHEREAL, value);
        if (!value) {
            player.noPhysics = player.isSpectator();
        }
    }

    public static boolean robeToggled(Player player) {
        return player.getData(TCBosses.ROBE_TOGGLE);
    }

    public static void setRobeToggled(Player player, boolean value) {
        player.setData(TCBosses.ROBE_TOGGLE, value);
    }

    public static int fleshLost(Player player) {
        return player.getData(TCBosses.FLESH_LOST);
    }

    public static void setFleshLost(Player player, int amount) {
        player.setData(TCBosses.FLESH_LOST, Math.max(0, amount));
    }

    public static int protolimbs(Player player) {
        return player.getData(TCBosses.PROTOLIMBS);
    }

    public static void setProtolimbs(Player player, int amount) {
        player.setData(TCBosses.PROTOLIMBS, amount);
    }

    /** Wearing all four Pontifex robe pieces. */
    public static boolean isFullSet(Player player) {
        return TCBosses.isRobe(player.getItemBySlot(EquipmentSlot.HEAD)) && TCBosses.isRobe(player.getItemBySlot(EquipmentSlot.CHEST))
                && TCBosses.isRobe(player.getItemBySlot(EquipmentSlot.LEGS)) && TCBosses.isRobe(player.getItemBySlot(EquipmentSlot.FEET));
    }

    /** The wand (and the Rift Gem) stay safe to use while ethereal. */
    public static boolean isWand(ItemStack stack) {
        return stack.getItem() instanceof ItemWand || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(RIFT_GEM);
    }

    /** One point of flesh dispelled by touching the world (not in creative, not once whole again). */
    private static void dispel(Player player) {
        if (!player.level().isClientSide() && !player.isCreative() && isEthereal(player) && protolimbs(player) < FULL_LIMBS) {
            setFleshLost(player, fleshLost(player) + 1);
        }
    }

    // ---------------------------------------------------------------- events
    static void register(IEventBus bus) {
        bus.addListener(PlayerTickEvent.Pre.class, Ethereal::tick);
        bus.addListener(LivingIncomingDamageEvent.class, Ethereal::damage);
        bus.addListener(LivingHealEvent.class, e -> {
            if (e.getEntity() instanceof Player p && isEthereal(p)) {
                e.setCanceled(true);
            }
        });
        bus.addListener(PlayerInteractEvent.RightClickBlock.class, e -> touch(e.getEntity(), e.getItemStack()));
        bus.addListener(PlayerInteractEvent.LeftClickBlock.class, e -> touch(e.getEntity(), e.getItemStack()));
        bus.addListener(PlayerInteractEvent.RightClickItem.class, Ethereal::rightClickItem);
        bus.addListener(LivingEntityUseItemEvent.Start.class, e -> {
            if (e.getEntity() instanceof Player p) {
                touch(p, e.getItem());
            }
        });
        bus.addListener(AttackEntityEvent.class, Ethereal::attack);
        bus.addListener(LivingChangeTargetEvent.class, Ethereal::spareRobeWearer);
        bus.addListener(PlayerEvent.Clone.class, Ethereal::respawn);
    }

    private static void touch(Player player, ItemStack held) {
        if (!isWand(held)) {
            dispel(player);
        }
    }

    private static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!isWand(held)) {
            dispel(player);
            return;
        }
        // TC4: wand right-clicked in the air below y = -1 (under the bedrock) with all six protolimbs -> Solid Void.
        if (!player.level().isClientSide() && held.getItem() instanceof ItemWand && protolimbs(player) >= FULL_LIMBS
                && player.getY() < player.level().getMinBuildHeight() - 1) {
            if (!player.getInventory().add(new ItemStack(TCBosses.SOLID_VOID_ITEM.get()))) {
                player.drop(new ItemStack(TCBosses.SOLID_VOID_ITEM.get()), false);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    private static void tick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (isEthereal(player)) {
            Vec3 v = player.getDeltaMovement();
            if (!player.isShiftKeyDown()) {
                player.setDeltaMovement(v.x, 0.0, v.z);
            }
            player.resetFallDistance();
        }
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        if (robeToggled(server) && !isFullSet(server)) {
            setEthereal(server, false);
            setRobeToggled(server, false);
        }
        int flesh = fleshLost(server);
        if (flesh > 0) {
            float cap = server.getMaxHealth() - flesh;
            if (cap <= 0.0F) {
                if (!server.isCreative() && !server.isSpectator()) {
                    server.hurt(server.damageSources().genericKill(), Float.MAX_VALUE);
                }
            } else if (server.getHealth() > cap) {
                server.setHealth(cap);
            }
        }
    }

    private static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isEthereal(player) || player.isCreative()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            dispel(player);
        }
        event.setCanceled(true);
    }

    /**
     * Touching the world by attacking costs flesh, and a full robe commands every crimson cultist within 32 blocks to
     * attack the same target, with Strength from the player's Desiderium research points (TC4 Fames) and Regeneration
     * from Victus (TC4 Sano), both /100 and clamped to 1-200, for five seconds.
     */
    private static void attack(AttackEntityEvent event) {
        Player player = event.getEntity();
        touch(player, player.getMainHandItem());
        if (!(player instanceof ServerPlayer server) || !(event.getTarget() instanceof LivingEntity target) || target instanceof EntityCultist
                || !isFullSet(server)) {
            return;
        }
        var cultists = server.level().getEntitiesOfClass(EntityCultist.class, server.getBoundingBox().inflate(COMMAND_RANGE));
        if (cultists.isEmpty()) {
            return;
        }
        int strength = Mth.clamp(pool(server, TTAspects.DESIDERIUM) / 100, 1, 200);
        int regen = Mth.clamp(pool(server, TTAspects.VICTUS) / 100, 1, 200);
        for (EntityCultist cultist : cultists) {
            cultist.setTarget(target);
            cultist.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, COMMAND_TICKS, strength));
            cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COMMAND_TICKS, regen));
        }
    }

    private static int pool(ServerPlayer player, ResourceKey<IAspect> aspect) {
        Holder<IAspect> holder = player.level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect).orElse(null);
        return holder == null ? 0 : AspectPoolAccess.amount(player, holder);
    }

    /** Crimson cultists will not take a player in the full Pontifex robe as their target (TC4 Tainted Magic hook). */
    private static void spareRobeWearer(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof EntityCultist && event.getNewAboutToBeSetTarget() instanceof Player player && isFullSet(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * After a death: ethereal, the robe toggle and the protolimbs reset (not copied); the flesh debt stays, capped at
     * full health minus two. TC4 only capped it when the player died ethereal, so a debt above the full health taken
     * out of the ethereal state killed the player again on every respawn; the port always caps it.
     */
    private static void respawn(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player player = event.getEntity();
        int flesh = fleshLost(player);
        float health = player.getMaxHealth();
        if (flesh >= health) {
            setFleshLost(player, Mth.floor(health - 2.0F));
        }
    }
}
