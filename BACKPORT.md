# NeoForge 1.21.1 backport

This folder builds Thaumaturgic Concilium 0.6.1 for Minecraft 1.21.1 (NeoForge 21.1, Thaumaturge 1.1.0 for 1.21.1).
It starts from the 26.1.2 code at commit fe90d7d and keeps its content, data and art. The later Witchery imp deal and
the Botania bracelet art are not in this build.

## Cuts and changes

- Ars Magica Legacy, Thaumic Horizons, Thaumaturgic Bases and Gadomancy compat is left out, since those mods have no
  1.21.1 build. Witchery, Neo Vitae and Automagia compat stays. The Drainage Syringe and the seals stay as base content.
- `LegacyIds` is removed. It remapped ids from older 26.1.2 worlds, and no 1.21.1 world has them.
- Armour pieces are `ArmorItem`s on registered armour materials (`TCArmor`) with layer textures in
  `textures/models/armor`, instead of equippable items with equipment assets. The Pontifex robe uses Thaumaturge's robe
  layers.
- Robes are dyed with the vanilla armour dye recipe through the `minecraft:dyeable` tag. The four `dye_pontifex_robe_*`
  recipes are removed. Undyed robes keep the original red through a default dye colour and an item colour handler.
- Item models live in `models/item` with overrides, since 1.21.1 has no `items/` item definitions. Block items point at
  their block model. The Drainage Syringe switches to its filled model through an item property
  (`thaumaturgicconcilium:syringe_filled`). Robe and Destabilized Crystal tints are item and block colour handlers.
- Recipes use 1.21.1 ingredient objects (`{"item": ...}`, `{"tag": ...}`).
- The Causal Bouillon dimension type uses the 1.21.1 fields with End sky effects and a fixed time of 6000. Beds and
  respawn anchors do not work there, as before.
- The Witchery compat pack uses pack format 48.
- Mobs that only spawned outside Peaceful lose that spawn flag, since 1.21.1 has no such entity type setting.
- Lightning bolt particles are sent to each nearby player.
- The Nether Explorer swims in lava through `canDrownInFluidType`.
- The Lithographer drops its contents in `onRemove`.
- Rift render types come from Thaumaturge's flux rift renderer, with the end portal type as a fallback.
- The lesser portal works out Thaumaturge's portal size and frame and queues it through Thaumaturge's occluding effect
  renderer.
- The Solid Void shapes are drawn as end portal cubes.
- The astral hole overlay is a GUI layer.
- The test-only window focus mixin is removed. NeoForge 1.21.1 opens the game window in its early loading screen.
