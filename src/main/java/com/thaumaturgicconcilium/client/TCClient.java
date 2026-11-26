package com.thaumaturgicconcilium.client;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.block.DestabilizedCrystalBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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

    private static void blockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new CrystalTint()), TCRegistry.DESTABILIZED_CRYSTAL.get());
    }

    /** The crystal takes its aspect's colour; empty, it is pale violet. */
    private static final class CrystalTint implements BlockTintSource {
        private static final int EMPTY = 0xFFB9A3D6;

        @Override
        public int color(BlockState state) {
            return EMPTY;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            var mc = Minecraft.getInstance();
            if (mc.level == null || !(level.getBlockEntity(pos) instanceof DestabilizedCrystalBlockEntity crystal)) {
                return EMPTY;
            }
            return crystal.aspect().flatMap(k -> mc.level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(k))
                    .map(h -> 0xFF000000 | h.value().color()).orElse(EMPTY);
        }
    }
}
