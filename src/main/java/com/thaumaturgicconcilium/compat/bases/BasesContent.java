package com.thaumaturgicconcilium.compat.bases;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.leclowndu93150.thaumaturge.content.wands.WandParts;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumaturgicconcilium.TCRegistry;
import com.thaumaturgicconcilium.ThaumaturgicConcilium;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Casting Bracelets (TC4 Concilium {@code Bracelets}, registered only with Thaumic Bases, whose bracelet model they
 * used). Names no Thaumic Bases type: the bracelets need Bases only to exist.
 * <ul>
 * <li>Caps: TC4 divided the cap's cost modifier by 1.5, so each bracelet has its own cap entry at that value (thaumium
 * 0.6; alchemical 0.6, Aqua 0.533; vinteum 0.6; shadowmetal 0.467).</li>
 * <li>Cores: staff-class, capacities 35 (infernal, tainted), 50 (blood, witchwood), 90 (warpwood); each core's own
 * per-tick power is borrowed from the Forbidden Magic / Tainted Magic staff or wand core of the same wood.</li>
 * <li>Blood (Neo Vitae) and witchwood (Ars Magica Legacy) bracelets show in the creative tab and have recipes only with
 * those mods; warpwood exists only with Tainted Magic. Livingwood, dreamwood and terrasteel (Botania) are cut.</li>
 * </ul>
 */
public final class BasesContent {
    private static final String MODID = ThaumaturgicConcilium.MODID;
    public static final DeferredRegister<WandCap> CAPS = DeferredRegister.create(WandCap.REGISTRY_KEY, MODID);
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    private static final List<Bracelet> BRACELETS = new ArrayList<>();
    /** Casters that cast this tick with a bracelet: their cooldown is halved once the cast has set it. */
    private static final Set<UUID> JUST_CAST = ConcurrentHashMap.newKeySet();

    record Bracelet(DeferredItem<BraceletItem> item, DeferredHolder<WandCap, WandCap> cap, DeferredHolder<WandRod, WandRod> rod) {
    }

    private BasesContent() {
    }

    private static ResourceKey<IAspect> aspect(String ns, String path) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath(ns, path));
    }

    private static DeferredHolder<WandCap, WandCap> cap(String name, float base, List<ResourceKey<IAspect>> special, float specialMod, Identifier texture) {
        return CAPS.register("bracelet_" + name, () -> new WandCap(base, special, specialMod, 0, texture));
    }

    /** A bracelet core: staff-class, borrowing the per-tick power of a partner core looked up by id when it runs. */
    private static DeferredHolder<WandRod, WandRod> rod(String name, int capacity, Identifier borrowFrom, Identifier texture, boolean glow) {
        IWandRodOnUpdate borrowed = (stack, player) -> player.level().registryAccess().lookup(WandRod.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(borrowFrom)).map(WandRod::onUpdate).ifPresent(u -> u.onUpdate(stack, player));
        return RODS.register("bracelet_" + name, () -> new WandRod(capacity, 0, texture, borrowed, glow, true, false));
    }

    private static void bracelet(String name, DeferredHolder<WandCap, WandCap> cap, DeferredHolder<WandRod, WandRod> rod, boolean inTab) {
        DeferredItem<BraceletItem> item = ITEMS.registerItem("casting_bracelet_" + name, p -> new BraceletItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));
        BRACELETS.add(new Bracelet(item, cap, rod));
        if (inTab) {
            TCRegistry.addToTab(item);
        }
    }

    private static Identifier id(String ns, String path) {
        return Identifier.fromNamespaceAndPath(ns, path);
    }

    public static void register(IEventBus modBus, boolean taintedMagic) {
        ModList mods = ModList.get();
        var thaumium = cap("thaumium", 0.6F, List.of(), 0.6F, id("thaumaturge", "textures/models/wand_cap_thaumium.png"));
        var alchemical = cap("alchemical", 0.6F, List.of(aspect("thaumaturge", "aqua")), 0.8F / 1.5F, id("forsakenmagic", "textures/models/wand_cap_alchemical.png"));
        var vinteum = cap("vinteum", 0.6F, List.of(), 0.6F, id("forsakenmagic", "textures/models/wand_cap_vinteum.png"));
        bracelet("infernal", thaumium, rod("infernal", 35, id("forsakenmagic", "infernal"), id("forsakenmagic", "textures/models/wand_rod_infernal.png"), true), true);
        bracelet("tainted", thaumium, rod("tainted", 35, id("forsakenmagic", "tainted"), id("forsakenmagic", "textures/models/wand_rod_tainted.png"), false), true);
        bracelet("blood", alchemical, rod("blood", 50, id("forsakenmagic", "blood_staff"), id("forsakenmagic", "textures/models/wand_rod_blood.png"), false),
                mods.isLoaded("neovitae"));
        bracelet("witchwood", vinteum, rod("witchwood", 50, id("forsakenmagic", "witchwood_staff"), id("forsakenmagic", "textures/models/wand_rod_witchwood.png"), false),
                mods.isLoaded("arsmagicalegacy"));
        if (taintedMagic) {
            var shadowmetal = cap("shadowmetal", 0.7F / 1.5F, List.of(), 0.7F / 1.5F, id("taintedmagic", "textures/models/wand_cap_shadowmetal.png"));
            bracelet("warpwood", shadowmetal, rod("warpwood", 90, id("taintedmagic", "warpwood_staff"), id("taintedmagic", "textures/models/wand_rod_warpwood.png"), true), true);
        }
        CAPS.register(modBus);
        RODS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(BasesContent::defaults);
        NeoForge.EVENT_BUS.addListener(BasesContent::onCast);
        NeoForge.EVENT_BUS.addListener(BasesContent::onPlayerTick);
    }

    private static void defaults(ModifyDefaultComponentsEvent event) {
        for (Bracelet b : BRACELETS) {
            event.modify(b.item(), (builder, registries, item) -> builder.set(TTDataComponents.WAND_PARTS.get(), new WandParts(b.cap().get(), b.rod().get(), false)));
        }
    }

    private static boolean holdsBracelet(LivingEntity caster) {
        return caster.getMainHandItem().getItem() instanceof BraceletItem || caster.getOffhandItem().getItem() instanceof BraceletItem;
    }

    private static void onCast(SpellCastEvent.Post event) {
        LivingEntity caster = event.caster();
        if (!caster.level().isClientSide() && holdsBracelet(caster)) {
            JUST_CAST.add(caster.getUUID());
        }
    }

    /** TC4 set the focus cooldown to half its activation cooldown; the cast sets the full one, so it is halved after. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !JUST_CAST.remove(player.getUUID())) {
            return;
        }
        int ticks = Math.round(CasterManager.cooldownSeconds(player) * 20.0F);
        if (ticks > 1) {
            CasterManager.startCooldown(player, ticks / 2);
        }
    }
}
