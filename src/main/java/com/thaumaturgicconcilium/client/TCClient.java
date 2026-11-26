package com.thaumaturgicconcilium.client;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client setup: entity renderers and the Destabilized Crystal's aspect tint. */
public final class TCClient {
    private TCClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TCClient::renderers);
        modBus.addListener(TCClient::blockTints);
        modBus.addListener(TCClient::itemTints);
        com.thaumaturgicconcilium.spell.client.TCSpellClient.init(modBus);
        com.thaumaturgicconcilium.mob.client.TCMobsClient.init(modBus);
        com.thaumaturgicconcilium.crimson.client.TCCrimsonClient.init(modBus);
        com.thaumaturgicconcilium.rift.client.TCRiftsClient.init(modBus);
        com.thaumaturgicconcilium.boss.client.TCBossesClient.init(modBus);
        com.thaumaturgicconcilium.compat.CompatA.initClient(modBus);
        com.thaumaturgicconcilium.compat.CompatB.initClient(modBus);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCRegistry.THROWN_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(TCRegistry.SHARD_POWDER.get(), NoopRenderer::new);
    }

    private static void blockTints(RegisterColorHandlersEvent.Block event) {
        event.register(TCClient::crystalTint, TCRegistry.DESTABILIZED_CRYSTAL.get());
    }

    /** The robe's cloth layer takes its dye (crimson undyed); the crystal item shows the empty crystal's violet. */
    private static void itemTints(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack, ROBE) : -1,
                com.thaumaturgicconcilium.boss.TCBosses.PONTIFEX_ROBE_HEAD.get(), com.thaumaturgicconcilium.boss.TCBosses.PONTIFEX_ROBE_CHEST.get(),
                com.thaumaturgicconcilium.boss.TCBosses.PONTIFEX_ROBE_LEGS.get(), com.thaumaturgicconcilium.boss.TCBosses.PONTIFEX_ROBE_FEET.get());
        event.register((stack, tint) -> EMPTY, TCRegistry.DESTABILIZED_CRYSTAL.get());
    }

    private static final int ROBE = -6285288;
    private static final int EMPTY = 0xFFB9A3D6;

    /** The crystal takes its aspect's colour; empty, it is pale violet. */
    private static int crystalTint(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        var mc = Minecraft.getInstance();
        if (level == null || pos == null || mc.level == null || !(level.getBlockEntity(pos) instanceof DestabilizedCrystalBlockEntity crystal)) {
            return EMPTY;
        }
        return crystal.aspect().flatMap(k -> mc.level.registryAccess().registryOrThrow(IAspect.REGISTRY_KEY).getHolder(k))
                .map(h -> 0xFF000000 | h.value().color()).orElse(EMPTY);
    }
}
