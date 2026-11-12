# ReTerraForged 0.0.32 (draft)

TerraForged-style terrain for Minecraft 1.21.1 – 26.3, NeoForge and Fabric. Java 21 for 1.21.x, Java 25 for 26.x. License LGPL-3.0-only. Full list of changes: README in the repository.

## What's new

- **Generation version 1, new worlds only**: worlds made before keep every block. It comes with Generation Fixes; a preset from the player's library gets it when a new world is created.
  - **Strongholds, ancient cities and trial chambers are built under rivers and lakes.** The structure rule refused them at any depth there, so there were fewer strongholds, and the Eye of Ender skipped their point.
  - **Rock strata follow the world's seed.** Before, every world had the same strata.
  - **A 64-bit seed gives its own terrain.** Before, only the lower 32 bits counted. Seeds that fit 32 bits, including all text seeds, give the same terrain as before.
  - **Erosion reads the biomes of each chunk**, as the surface rules do: sand no longer stays on steep slopes by islands ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)).
  - **Columns past 16.7 million blocks** no longer come in pairs of equal columns.
- **Snow Line Follows World Height** applies only in the dimension the preset builds. Before, the setting changed snow and ice in other mods' dimensions too.
- **The tile size is stored in the world** (`reterraforged/generation.json` in the world folder). A change of `tileSize` in `performance.conf` now applies to new worlds only, so it no longer makes erosion seams in an existing world.
- **Preset screen:** exporting a datapack no longer freezes the game. Opening Create World, Done and Export As Datapack keep the game responsive; the vanilla loading screen shows during the export.
- **Preset screen hints:** five hints describe their settings again (Plain Stone Erosion, Min and Max Start Distance of lakes, Mountain Biome Usage, Volcano Biome Usage). Their texts were lost in all 11 languages after 0.0.21.
- **Eight new built-in presets:** Archipelago, Wetlands, Towering Peaks, Endless Continent, River Valleys, Lush Wilds, Rolling Plains and Rugged Highlands (see Built-in presets).
- **Preset datapacks work on both loaders:** exports write the biome modifiers for NeoForge and for Fabric. Fabric also reads the NeoForge folder of older datapacks.
- **Preset screen:** the preset list has three columns: the list, a relief map and description of the selected preset, and the buttons. A setting that differs from Default is marked, and a right click resets it. The search box finds settings on all pages. Done without changes does not export the datapack again.
- **Bug reports:** the log and crash reports carry the versions, the generation version, the tile size, the preset's differences from Default and the mods that touch generation. `/reterraforged info` (permission level 2) shows the same. The seed of a dedicated server is written only with `logSeed=true` in `config/reterraforged/diagnostics.conf`.

## Fixes

- **Swamp Basins in another region's climate** (possible cause of [#42](https://github.com/Vladeezyy/reterraforged/issues/42)): a piece of the world could get the swamp basin and climate of another region, as whole chunks of swamp in the middle of a desert, for example. It depended on the order of the generation threads and was gone when the world was made again. Not reproduced, so the cause is not proven.
- **Generation is a few percent faster, the same world:** on 1.21.1 NeoForge the Default preset went from 15.2 to 14.8 ms per chunk and the preset with Swamp Basins and Lowland Swamps from 15.3 to 14.6 ms (the Default difference is within the spread of the runs); on a 4-core server there is no change. Nothing a player would notice. The world stays block for block the same as in 0.0.31.
- **Leaving a world cancels its unfinished tiles**, so they do not delay the first tile of the next world.
- **Trees from saplings keep the blocks under their roots** (glass, water, flowers, saplings, rails). It touched tree templates from datapacks with blocks beside the trunk on the bottom layer; the mod's built-in trees are not affected.
- **Noise in datapacks:** the number of octaves and terrace steps is limited (a huge or negative value hung or crashed generation); a strata selector below 0 no longer crashes the game; the `simplex_ridge` type can be written.
- **Other mods:** injections that would conflict with another mod at the same place are replaced with compatible ones, and the spawn point search is replaced only in worlds with a preset. TerraBlender is an optional dependency. Its diagnostic counters, which ran on every biome pick with Darker Depths or Deeper Darker installed, now run only with `-Drtf.tbdiag=true`.
- **Preset export** no longer runs on the render thread (see above).
- **Structure settings** no longer write whole structure sets into the world's datapack, so a world loads after a structure mod is removed. The sets of datapacks from 0.0.12–0.0.31 are skipped.
- **Behaviour change, TerraBlender with Darker Depths or Deeper Darker:** in worlds without a preset their underground regions stay TerraBlender's own. Before, the mod replaced them there too. Worlds with a preset are unchanged.
- **Build:** the Loom and Architectury plugin versions are pinned (not snapshots).

## Known limitations

- Mineshafts under river beds at sea level are still not built: their start point is above the depth threshold.
- Far from the center (past 16.7 million blocks), in worlds made without generation version 1 the ground comes in pairs of equal columns (steps of 2 blocks). New worlds have no pairs, but float precision still leaves fine roughness. The noise distortion gets coarse already around 8 million blocks. Keep the world border within 8 million blocks (`/worldborder`).
- A preset datapack exported on Fabric by an older version gives NeoForge worlds none of the mod's trees, erosion, swamp surface or snow. NeoForge logs a warning, and an operator who joins is told in chat. Export the preset again with this version.
- A Fabric world made with a preset datapack exported on NeoForge (the preset zip of the releases is one) had none of the mod's trees, erosion, swamp surface or snow. With this version its new chunks get them, so such a world can show a seam between the chunks made before and after the update.
- Seeds: in worlds made without generation version 1 the terrain uses only the lower 32 bits, so seeds `x` and `x + 2^32` have nearly the same terrain there.
- `tileSize` in `performance.conf` also changes a few surface blocks, as erosion runs per tile. An existing world keeps the tile size it was made with.
- The surface on beaches and slopes sometimes takes a biome without the erosion of its tile, as in the original. Not fixed.
- Not tested yet: Biomes O' Plenty with Regions Unexplored on Fabric 1.21.1.

## Tested

- 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3, NeoForge and Fabric, real dedicated servers: self test 42 checks on each version and loader (on 1.21.1 with biome mods: 41); the worlds of the two loaders match in heights and biomes; the 0.0.31 preset on this build gives the same surface and heights (all 4501 fingerprint lines match on 1.21.1, 1.21.11 and 26.2; on 26.1.2 and 26.3, 1 and 4 whole chunks out of 96 differ, where features cross the chunk border).
- Client GUI test in Russian on both loaders: 31 screenshots, including the five restored hints.
- Biomes O' Plenty with TerraBlender on NeoForge: the self test and the biome kinds of the mods pass.
- A tile that fails to generate stops the server by itself, in 47–48 s.
- Unit tests: 98 on every version except 26.3 (92).
- 1.21.1 only: C2ME on NeoForge and on Fabric (with ModernFix on Fabric), self test passes; Biomes O' Plenty with Regions Unexplored on NeoForge: 89 of 121 biome kinds, as in 0.0.31; stress test with 467 preset variants, no failures; GUI test in English.

## Built-in presets

Eight presets are added to the preset list. Each is Default with a few settings changed:

- **Archipelago:** island groups off the coasts, some volcanic.
- **Wetlands:** wide swamps just above the sea, more lakes and wetlands.
- **Towering Peaks:** a higher world with more and taller mountains.
- **Endless Continent:** one land without oceans, with more rivers and lakes. Slower to generate (about 30 %).
- **River Valleys:** many rivers in wide, winding valleys, with gorges and lakes.
- **Lush Wilds:** denser forests, more grass and flowers.
- **Rolling Plains:** wide, gentle plains and steppes with few mountains.
- **Rugged Highlands:** plateaus, badlands, terraced hills and volcanoes instead of plains.

## Versions

- **1.21.1:** NeoForge 21.1.201; Fabric Loader 0.16.3, Fabric API 0.102; Java 21
- **1.21.11:** NeoForge 21.11.45; Fabric Loader 0.17.3, Fabric API 0.141; Java 21
- **26.1.2:** NeoForge 26.1.2.116; Fabric Loader 0.19.5, Fabric API 0.155; Java 25
- **26.2:** NeoForge 26.2.0.90; Fabric Loader 0.19.5, Fabric API 0.161; Java 25
- **26.3:** NeoForge 26.3.0.76; Fabric Loader 0.19.5, Fabric API 0.162; Java 25

Each version has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.

## How to update

- Replace the old jar in `mods/` with the new one. New worlds are made with the preset as before.
- Generation version 1 applies to new worlds only: a world made before keeps every block.
- A preset datapack exported on Fabric by an older version gives NeoForge worlds none of the mod's biome modifiers: export the preset again.
