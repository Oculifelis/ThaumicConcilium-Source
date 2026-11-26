package com.thaumaturgicconcilium.boss;

import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import com.thaumaturgicconcilium.crimson.TCCrimson;
import com.thaumaturgicconcilium.mob.MadThaumaturgeEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Crimson Initiation (TC4 Concilium {@code MadThaumaturge.interact}): in the full Pontifex robe, use a crimson dagger on
 * a Mad Thaumaturge and he is reborn as a crimson cultist (knight, cleric or paladin, one in three each). TC4 needed
 * Tainted Magic's Crystal Dagger; the daggers are now the item tag {@code thaumaturgicconcilium:initiation_daggers}
 * (Thaumaturge's Crimson Blade, plus Tainted Magic's dagger when it is installed).
 */
public final class CrimsonInitiation {
    public static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, ThaumaturgicConcilium.id("initiation_daggers"));
    private static final DustParticleOptions BLOOD = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0x8A0000).toVector3f(), 1.2F);

    private CrimsonInitiation() {
    }

    static void register(IEventBus bus) {
        bus.addListener(PlayerInteractEvent.EntityInteract.class, CrimsonInitiation::interact);
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!(event.getTarget() instanceof MadThaumaturgeEntity mad) || !held.is(DAGGERS) || !Ethereal.isFullSet(player)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player.level() instanceof ServerLevel level) || !mad.isAlive()) {
            return;
        }
        EntityType<? extends Mob> type = switch (level.getRandom().nextInt(3)) {
            case 0 -> TTEntities.CULTIST_KNIGHT.get();
            case 1 -> TTEntities.CULTIST_CLERIC.get();
            default -> TCCrimson.CRIMSON_PALADIN.get();
        };
        Mob cultist = type.create(level);
        if (cultist == null) {
            return;
        }
        cultist.moveTo(mad.getX(), mad.getY(), mad.getZ(), mad.getYRot(), mad.getXRot());
        cultist.finalizeSpawn(level, level.getCurrentDifficultyAt(mad.blockPosition()), MobSpawnType.CONVERSION, null);
        mad.discard();
        level.addFreshEntity(cultist);
        level.playSound(null, cultist.getX(), cultist.getY(), cultist.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(BLOOD, mad.getX(), mad.getY() + 1.0, mad.getZ(), 24, 0.3, 0.6, 0.3, 0.0);
    }
}
