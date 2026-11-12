# ReTerraForged 0.0.32 (draft)

TerraForged-style terrain for Minecraft 1.21.1, NeoForge and Fabric. Java 21. License LGPL-3.0-only. Full list of changes: README in the repository.

## What's new

- **Generation version 1, new worlds only**: worlds made before keep every block. It comes with Generation Fixes; a preset from the player's library gets it when a new world is created.
  - **Strongholds, ancient cities and trial chambers are built under rivers and lakes.** The structure rule refused them at any depth there, so there were fewer strongholds, and the Eye of Ender skipped their point.
  - **Rock strata follow the world's seed.** Before, every world had the same strata.
  - **A 64-bit seed gives its own terrain.** Before, only the lower 32 bits counted. Seeds that fit 32 bits, including all text seeds, give the same terrain as before.
- **Snow Line Follows World Height** applies only in the dimension the preset builds. Before, the setting changed snow and ice in other mods' dimensions too.
- **The tile size is stored in the world** (`reterraforged/generation.json` in the world folder). A change of `tileSize` in `performance.conf` now applies to new worlds only, so it no longer makes erosion seams in an existing world.
- **Preset screen:** exporting a datapack no longer freezes the game. Opening Create World, Done and Export As Datapack keep the game responsive; the vanilla loading screen shows during the export.
- **Preset screen hints:** five hints describe their settings again (Plain Stone Erosion, Min and Max Start Distance of lakes, Mountain Biome Usage, Volcano Biome Usage). Their texts were lost in all 11 languages after 0.0.21.

## Fixes

- **Swamp Basins in another region's climate** (possible cause of [#42](https://github.com/Vladeezyy/reterraforged/issues/42)): a piece of the world could get the swamp basin and climate of another region, as whole chunks of swamp in the middle of a desert, for example. It depended on the order of the generation threads and was gone when the world was made again. Not reproduced yet.
- **Generation speed without changing the world:** caches per thread, fewer locks, tiles are made outside the shared lock, and the feature step no longer waits for a tile. The terrain stays block for block the same as in 0.0.31: the chunk fingerprints of the Default preset and of the preset with Swamp Basins and Lowland Swamps match it.
- **Leaving a world cancels its unfinished tiles**, so they do not delay the first tile of the next world.
- **Trees from saplings keep the blocks under their roots** (glass, water, flowers, saplings, rails). This touched tree templates from datapacks with blocks beside the trunk on the bottom layer. The mod's built-in trees are not affected.
- **Noise in datapacks:** the number of octaves and terrace steps is limited, since a huge or negative value hung or crashed generation. A strata selector below 0 no longer crashes the game. The `simplex_ridge` noise type can be written.
- **Other mods:** injections that would conflict with another mod at the same place are replaced with compatible ones. TerraBlender is an optional dependency. The spawn point search is replaced only in worlds with a preset.
- **TerraBlender diagnostics** with Darker Depths or Deeper Darker installed wrote lines to the log on every biome pick. They run only with `-Drtf.tbdiag=true` now.
- **Build:** the Loom and Architectury plugin versions are pinned (no snapshots).

## Known limitations

- Mineshafts under river beds at sea level are still not built: their start point is above the depth threshold.
- Far from the center (past 16.7 million blocks) the ground comes in steps of 2 blocks, and the noise distortion gets coarse already around 8 million blocks. Keep the world border within 8 million blocks (`/worldborder`).
- Seeds: in older worlds the terrain uses only the lower 32 bits, so seeds `x` and `x + 2^32` have nearly the same terrain there.
- `tileSize` in `performance.conf` also changes a few surface blocks, as erosion runs per tile. An existing world keeps the tile size it was made with.
- The surface on beaches and slopes sometimes takes a biome without the erosion of its tile, as in the original.

## Tested

- 1.21.1, NeoForge and Fabric dedicated servers: self test 41 of 41 on both loaders; the worlds of both loaders match; the chunk fingerprint matches 0.0.31.
- C2ME: the self test passes.
- Biomes O' Plenty with Regions Unexplored: 89 of 121 biome kinds, as in 0.0.31.
- A tile that fails to generate stops the server by itself.
- A world made by the previous release gives the same terrain with this build (CI).
- 98 unit tests (was 45).

## Built-in presets

The 8 new built-in presets from another branch are added here before the release. This section is not complete yet.

## Versions

- **1.21.1:** NeoForge 21.1.201; Fabric Loader 0.16.3, Fabric API 0.102; Java 21

Each version has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.

## How to update

- Replace the old jar in `mods/` with the new one. New worlds are made with the preset as before.
- Generation version 1 applies to new worlds only: a world made before keeps every block.
