# ReTerraForged 0.0.32

TerraForged-style terrain for Minecraft 1.21.1 – 26.3, NeoForge and Fabric. Java 21 for 1.21.x, Java 25 for 26.x. License LGPL-3.0-only. Full list of changes: README in the repository.

## What's new

- **Generation version 1, new worlds only**: worlds made before keep every block. It comes with Generation Fixes; a preset from the player's library gets it when a new world is created.
  - **Strongholds, ancient cities and trial chambers are built under rivers and lakes.** The structure rule refused them at any depth there, so there were fewer strongholds, and the Eye of Ender skipped their point.
  - **Rock strata follow the world's seed.** Before, every world had the same strata.
  - **A 64-bit seed gives its own terrain.** Before, only the lower 32 bits counted. Seeds that fit 32 bits, including all text seeds, give the same terrain as before.
  - **Erosion reads the biomes of each chunk**, as the surface rules do: sand no longer stays on steep slopes by islands ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)).
  - **Columns past 16.7 million blocks** no longer come in pairs of equal columns.
- **Snow Line Follows World Height** applies only in the dimension the preset builds. Before, the setting changed snow and ice in other mods' dimensions too.
- **Snow Line Height goes up to 1024** (it was 512), the top of the tallest world.
- **Fake rivers on four more terrains** (the Fake Water Biomes page, off by default): land below sea level gets a river biome, as steppe and badlands already did, now also on plateaus, hills, mountains and plains. Before, a plateau canyon below sea level kept its land biome. Presets saved before have these switches off, so existing worlds keep their biomes.
- **The tile size is stored in the world** (`reterraforged/generation.json` in the world folder). A change of `tileSize` in `performance.conf` now applies to new worlds only, so it no longer makes erosion seams in an existing world.
- **Preset screen:** exporting a datapack no longer freezes the game. Opening Create World, Done and Export As Datapack keep the game responsive; the vanilla loading screen shows during the export.
- **Preset screen hints:** five hints describe their settings again (Plain Stone Erosion, Min and Max Start Distance of lakes, Mountain Biome Usage, Volcano Biome Usage). Their texts were lost in all 11 languages after 0.0.21.
- **Eight new built-in presets:** Archipelago, Wetlands, Towering Peaks, Endless Continent, River Valleys, Lush Wilds, Rolling Plains and Rugged Highlands (see Built-in presets).
- **Preset datapacks work on both loaders:** exports write the biome modifiers for NeoForge and for Fabric. Fabric also reads the NeoForge folder of older datapacks.
- **Preset screen:** the preset list has three columns: the list, the selected preset's map and description, and the buttons. The map is drawn in the colours of the settings preview's Biome Types mode, at twice its detail. A setting that differs from Default is marked, and a right click resets it. The search box finds settings on all pages. Done without changes does not export the datapack again.
- **Bug reports:** the log and crash reports carry the versions, the generation version, the tile size, the preset's differences from Default and the mods that touch generation. `/reterraforged info` (permission level 2) shows the same. The seed of a dedicated server is written only with `logSeed=true` in `config/reterraforged/diagnostics.conf`.

## Fixes

- **Swamp Basins in another region's climate** (possible cause of [#42](https://github.com/Vladeezyy/reterraforged/issues/42)): a piece of the world could get the swamp basin and climate of another region, as whole chunks of swamp in the middle of a desert, for example. It depended on the order of the generation threads and was gone when the world was made again. Not reproduced, so the cause is not proven.
- **Generation is a few percent faster, the same world:** on 1.21.1 NeoForge the Default preset went from 15.2 to 14.8 ms per chunk and the preset with Swamp Basins and Lowland Swamps from 15.3 to 14.6 ms (the Default difference is within the spread of the runs); on a 4-core server there is no change. Nothing a player would notice. The world stays block for block the same as in 0.0.31.
- **Leaving a world cancels its unfinished tiles**, so they do not delay the first tile of the next world.
- **Trees from saplings keep the blocks under their roots** (glass, water, flowers, saplings, rails). It touched tree templates from datapacks with blocks beside the trunk on the bottom layer; the mod's built-in trees are not affected.
- **Noise in datapacks:** the number of octaves and terrace steps is limited (a huge or negative value hung or crashed generation); a strata selector below 0 no longer crashes the game; the `simplex_ridge` type can be written.
- **Other mods:** injections that would conflict with another mod at the same place are replaced with compatible ones, and the spawn point search is replaced only in worlds with a preset. TerraBlender is an optional dependency. Its diagnostic counters, which ran on every biome pick with Darker Depths or Deeper Darker installed, now run only with `-Drtf.tbdiag=true`.
- **Preset export** no longer runs on the render thread (see above).
- **Structure settings** no longer write whole structure sets into the world's datapack, so a world loads after a structure mod is removed. The sets of datapacks from 0.0.12–0.0.31 are skipped. On both loaders: before, Fabric looked for the preset file on the NeoForge path, so a Fabric world with the set of a removed mod still did not load.
- **26.3 only: the Nether and the End no longer use the overworld's ReTerraForged context** (also in 0.0.30 and 0.0.31, in worlds with Generation Fixes). A check read a tag of the preset datapack, which every dimension sees, so the Nether's structure rule dropped starts where the overworld has a river, a lake or mountains (on a 160 × 160 chunk test: 493 nether fossils instead of 526, and one fortress became a bastion), and each chunk of the Nether and the End built overworld tiles (structure starts on 128 × 128 chunks: Nether 5.5 s instead of 1.4 s, End 13.9 s instead of 1.3 s). Now only the dimension's own noise router decides, as on 26.2. The terrain of the Nether and the End did not change, and their saplings are vanilla again.
- **Behaviour change, TerraBlender with Darker Depths or Deeper Darker:** in worlds without a preset their underground regions stay TerraBlender's own. Before, the mod replaced them there too. Worlds with a preset are unchanged.
- **Build:** the Loom and Architectury plugin versions are pinned (not snapshots).

## Known limitations

- **26.3 worlds made with 0.0.30 or 0.0.31 (Generation Fixes on):** the new Nether chunks get vanilla's structure choice. A fortress whose start falls on a new chunk at the edge of the explored area may be cut off (rare).
- Mineshafts under river beds at sea level are still not built: their start point is above the depth threshold.
- Far from the center (past 16.7 million blocks), in worlds made without generation version 1 the ground comes in pairs of equal columns (steps of 2 blocks). New worlds have no pairs, but float precision still leaves fine roughness. The noise distortion gets coarse already around 8 million blocks. Keep the world border within 8 million blocks (`/worldborder`).
- A preset datapack exported on Fabric by an older version gives NeoForge worlds none of the mod's trees, erosion, swamp surface or snow. NeoForge logs a warning, and an operator who joins is told in chat. Export the preset again with this version.
- A Fabric world made with a preset datapack exported on NeoForge (the preset zip of the releases is one) had none of the mod's trees, erosion, swamp surface or snow. With this version its new chunks get them, so such a world can show a seam between the chunks made before and after the update.
- Seeds: in worlds made without generation version 1 the terrain uses only the lower 32 bits, so seeds `x` and `x + 2^32` have nearly the same terrain there.
- `tileSize` in `performance.conf` also changes a few surface blocks, as erosion runs per tile. An existing world keeps the tile size it was made with.
- The surface on beaches and slopes sometimes takes a biome without the erosion of its tile, as in the original. Not fixed.
- Not tested yet: Biomes O' Plenty with Regions Unexplored on Fabric 1.21.1.

## Tested

- 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3, NeoForge and Fabric, real dedicated servers: self test 50 checks on each version and loader; structures checked; the worlds of the two loaders match in heights and biomes.
- Fingerprint of the new world against the NeoForge reference: on 1.21.1, 26.1.2, 26.2 and 26.3 all 4501 of 4501 lines match. On 1.21.11 the surface and the base match, and one whole chunk out of 96 differs (vegetation across the chunk border). Before and after the new switches of this build, the Fabric fingerprint matches on 1.21.11, 26.1.2, 26.2 and 26.3; the Fabric references were taken again.
- The 0.0.31 preset on this build gives the same surface and heights: on 1.21.1 all 4501 lines match on both loaders, and on 1.21.11 and 26.2 all 4501 match. On 26.1.2 and 26.3, 1 and 4 whole chunks out of 96 differ, where features cross the chunk border.
- Client GUI test in Russian on NeoForge: 35 screenshots on 1.21.1, 34 on 1.21.11, 26.2 and 26.3.
- Unit tests: 167 on 1.21.1, 1.21.11, 26.1.2 and 26.2; 159 on 26.3.
- Biomes O' Plenty with TerraBlender on NeoForge (26.2, 26.3): the self test (49 of 49) and the biome kinds of the mods pass. With Terralith added (every version, both loaders) the biome kinds pass; two checks of the self test fail because of Terralith, as with 0.0.31: its forest grows its own trees, and five of its biomes have no vanilla dungeons (on 26.3 also: cherry saplings have room for 5 of 12 trees there).
- A tile that fails to generate stops the server by itself (Fabric, every version).
- A world made by 0.0.31 opened by this build gives the same ground as a new one (NeoForge, every version).
- C2ME on Fabric, every version: the self test passes.
- 1.21.1 only: widget test of the preset screen, 697 actions, all checks passed; GUI test in Russian on Fabric and in English on NeoForge; stress test with 483 preset variants, no failures; C2ME on NeoForge and with ModernFix on Fabric, self test passes; Biomes O' Plenty with Regions Unexplored on NeoForge: 89 of 121 biome kinds, as in 0.0.31.

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
