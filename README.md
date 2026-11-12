# ReTerraForged

TerraForged-style world generation for Minecraft **1.21.1 – 26.3** · **NeoForge** & **Fabric** · the generation of
ReTerraForged 0.0.6, fixed and ported.

![A river in the desert between red badlands](docs/showcase-01.jpg)

| ![A sandy coast and a river mouth](docs/showcase-02.jpg) | ![A wide river between wooded shores](docs/showcase-03.jpg) |
|---|---|
| ![A lake in the plains](docs/showcase-04.jpg) | ![A lake by a village](docs/showcase-05.jpg) |
| ![A sea shore and a sandbank](docs/showcase-06.jpg) | ![Hills to the horizon](docs/showcase-07.jpg) |
| ![A desert river valley](docs/showcase-08.jpg) | ![Sunset over the mountains](docs/showcase-09.jpg) |
| ![A cherry grove on the slopes](docs/showcase-10.jpg) | ![A badlands canyon in a spruce forest](docs/showcase-11.jpg) |
| ![Terracotta hills in the meadows](docs/showcase-12.jpg) | ![A snowy peak above the forest](docs/showcase-13.jpg) |
| ![A river in the jungle](docs/showcase-14.jpg) | ![Under the trees of a forest](docs/showcase-15.jpg) |
| ![A beach at the edge of the jungle](docs/showcase-16.jpg) | ![A shipwreck in a lake on the plains](docs/showcase-17.jpg) |
| ![A flower forest](docs/showcase-18.jpg) | ![A lake under the mountains](docs/showcase-19.jpg) |
| ![A pillager outpost on the plains](docs/showcase-20.jpg) | ![Desert villages at night](docs/showcase-21.jpg) |
| ![Preset screen with the preview map](docs/preset-screen.jpg) |  |

<details>
<summary><b>🇬🇧 English — click to open</b></summary>

<sub>Shaders and long view distance are just for the looks: the mod does not depend on them.</sub>

TerraForged-style world generation: continents and oceans, mountain ranges, river networks with tributaries, lakes,
wetlands, slope erosion, rock strata, and a preset editor with a live map of the world to be. This is a working fork of
[ReTerraForged](https://github.com/racoonman2/ReTerraForged) by racoonman2, built on TerraForged by dags. The
generation is that of the original **ReTerraForged 0.0.6** for 1.20.1, without its bugs and on new game versions.

## Download

| Minecraft | Release | NeoForge | Fabric Loader / API | Java |
|---|---|---|---|---|
| 26.3 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

Every release has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.
New versions are released for 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3; 1.21.3 to 1.21.10 stay at 0.0.25.
Earlier versions stay on the [releases page](https://github.com/Vladeezyy/reterraforged/releases), and their source
on protected branches `release/<version>-<minecraft>` (for example `release/0.0.14-1.21.1`).

## Usage

**Singleplayer.** When creating a world: World tab → Customize → pick a preset →
Done. Without this step the world will be vanilla. The experimental settings warning is expected, press Proceed.

**Server.** For a new world, put the datapack `reterraforged-preset-<version>.zip` into
`world/datapacks/` before the first start. Your own preset can be exported with the "Export As Datapack" button on the
preset screen. A datapack exported by this version works on NeoForge and on Fabric. One exported on NeoForge by an older
version works on Fabric too; one exported on Fabric by an older version gets no biome modifiers on NeoForge (see Known
limitations).

The mod only affects new worlds created with its preset. It does not change an existing world.

**Bug reports.** When the overworld loads, the log gets the world's details, and every crash report has them too: the
versions, the generation version, the tile size, the preset's differences from Default and the mods that touch
generation. The seed of a dedicated server is written only with `logSeed=true` in `config/reterraforged/diagnostics.conf`
(the default is `false`; single player always writes it). An operator can run `/reterraforged info` (permission level 2)
for the same details, with the seed. The preset list has a Copy World Info button, which copies the mod version, the seed
and the selected preset's settings.

## Features

- **TerraForged terrain:** continents with coastlines, shallows and bays, mountain chains, plateaus, hills, dales,
  steppes, badlands, volcanoes.
- **Water:** river networks with main rivers and tributaries, lakes on rivers, wetlands; riverbed depth and width are
  configurable.
- **Surface:** water droplet erosion simulation, rock and scree on steep slopes, rock strata, snow only where it would
  settle naturally.
- **Snow Line** (0.0.25, Miscellaneous page, off by default): snow on the ground and on the trees above a height,
  whatever the biome, so mountains in warm climates get snowy peaks; still water above it freezes. Snow Line Height
  and Snow Line Variation set where it lies and how far its edge goes up and down. Since 0.0.26 the snow can be deeper:
  Snow Depth at the Line and Snow Depth on the Peaks (1 to 8 layers, 8 is a full block) and Snow Depth Growth (over how
  many blocks above the line the depth goes from one to the other).
- **River and lake valleys** (0.0.27, Rivers page, [#29](https://github.com/Vladeezyy/reterraforged/issues/29)): Valley
  Width, Steepest Valley Side (ground steeper than this beside a river or lake is cut back to that slope), Valley Follows
  Meanders (no dry valley floor beside a river where it bends away) and Gorge Chance. All keep the old valleys by
  default; with tall or stretched terrain they replace the sheer walls a river cut through a mountain range.
- **Deepslate Level** (0.0.27, World page, [#32](https://github.com/Vladeezyy/reterraforged/issues/32)): the height
  where stone gives way to deepslate; 0 is vanilla's.
- **Warm Oceans by Rainforests** (0.0.28, Climate page, [#35](https://github.com/Vladeezyy/reterraforged/issues/35)): the sea
  of tropical rainforest regions is warm ocean, with coral reefs; off by default (lukewarm, as in 0.0.6).
- **Generation Fixes** (0.0.30, World page, [#28](https://github.com/Vladeezyy/reterraforged/issues/28), [#38](https://github.com/Vladeezyy/reterraforged/issues/38)): fixes that change the
  terrain, on in new presets and off in presets saved before, so existing worlds keep their terrain. Islands differ from seed to seed, their beaches
  are above the water, and their horizontal scale changes only their terrain, not their outline. Swamp floors no longer repeat from chunk to chunk.
  Lowland Swamps also picks the swamps that biome mods such as Terralith keep on other climates, and reaches the mixed terrains it used to leave
  out, most of the terrain regions. It now also reaches dry and cold lowland, where Terralith keeps Orchid Swamp and Ice Marsh. The Infinite continent type has land. Another mod's
  dimension on the overworld's noise settings gets terrain instead of a flat sea. In new worlds it also turns on generation version 1: deep structures under rivers and lakes, rock strata that follow the world's seed, the full 64-bit seed, erosion that reads the biomes of each chunk (sand no longer stays on steep slopes by islands, [#38](https://github.com/Vladeezyy/reterraforged/issues/38)), and columns past 16.7 million blocks that no longer come in pairs. Worlds made before do not change.
- **Lowland Swamps and Highest Swamps** (0.0.29, Climate page, [#28](https://github.com/Vladeezyy/reterraforged/issues/28)): wet,
  flat lowlands can get wide swamps (off by default), and a swamp biome too high above the sea is picked again (12 blocks in
  new presets), which keeps Terralith's swamps off the hills.
- **Swamp Basins** (0.0.30, Climate page, [#28](https://github.com/Vladeezyy/reterraforged/issues/28)): this share of the terrain
  regions with a wet, not cold climate become flat land just above the sea, one big swamp each. With Highest Swamps at 12 and Biomes O'
  Plenty and Terralith, 25 % gives swamps on 5 to 7 % of the land, 36 to 46 % of them wide, in patches of 1 to 1.5 km². It changes the terrain, so it is
  off by default and presets saved before have none. Generation is 4 to 5 % slower at 25 %. Rivers still cross the basins.
- **Island settings** (0.0.30, Islands page, [#38](https://github.com/Vladeezyy/reterraforged/issues/38)): Coast Noise Octaves (1 to 8, default 5), Coast Noise Gain (0 to 1, default 0.5) and Coast Noise Lacunarity (1 to 4, default 2) make island coasts rough instead of always smooth. Island Seed Offset (with a button for a random one) moves the islands to another layout. Distance From Continents (0 to 1, default 0) keeps islands away from continents: at 1 the nearest island is about 190 blocks from a continent and the median island 860, against 30 and 610 at 0. Continent Mountains (off by default) gives islands the continents' mountain noise; with the island mountain sliders at maximum, the highest point is about 300 instead of 235.
- **Vegetation density** (0.0.30, Miscellaneous page, [#37](https://github.com/Vladeezyy/reterraforged/issues/37)): Grass Density (0 to 3), Tree Density (0 to 2) and Plant Density (0 to 3, flowers, bushes, mushrooms and other plants): 1 is the usual amount, 0 none. They need Custom Biome Features (on in Default). They also scale the vegetation of other mods' biomes, recognised by the names of their features (one with an unknown name stays as it is). At 1 they cost nothing. In new worlds they apply
  only where the preset builds the terrain, not in other dimensions that use the same biomes.
- **Saplings grow the mod's trees** (0.0.31, [#41](https://github.com/Vladeezyy/reterraforged/issues/41), with Custom Biome Features on, on in Default): an oak, birch, spruce, jungle, acacia or dark oak sapling, bone meal too, grows one of the mod's template trees instead of the vanilla one. Spruce, jungle and dark oak grow 2 × 2 from four saplings. Next to flowers a bee nest appears with a 5 % chance, as in vanilla. Vanilla still picks the tree and puts the saplings back when the tree does not fit: the mod's trees are taller (jungle 16–19 blocks, redwood about 20), so a sapling needs more room than in vanilla. Saplings of other mods, cherry, mangrove and azalea are unchanged. With Custom Biome Features off, saplings grow vanilla trees.
- **Rock strata** ([#1](https://github.com/Vladeezyy/reterraforged/issues/1)): bands of granite, diorite, andesite and
  stone under the ground. The number of layers and their relative thickness are sliders on the Miscellaneous page.
  Stones of other mods go in through the block tag `reterraforged:rock`; Quark's jasper, limestone and shale are in it
  already. "Ore Compatible Stone Only" (on by default, as in TerraForged) keeps the strata to the stones ores generate in
  (tag `reterraforged:ore_compatible`): on 1.18+ ores skip Quark's stones, so switch it off to get them, with fewer
  ores in their layers.
- **The mod's own trees** from templates (oak, birch, spruce, redwood, willow, acacia, jungle), with bee nests like the
  vanilla ones.
- **Preset editor:** more than 160 settings on eleven pages (world, surface, caves, climate, terrain, islands, rivers,
  filters, structures and more) with a preview map that redraws on every change. Presets can be saved, copied and
  exported as a datapack. The preset list has three columns: the list, the selected preset's relief map and description, and
  the buttons. A setting that differs from Default is marked (and its page in the menu), and a right click resets it. The
  search box finds settings on all pages. Done without changes does not export the datapack again.
- **Structures:** spacing, separation and salt for every structure set, or disable it. Villages and other surface
  structures are not placed on mountains, as in 0.0.6.
- **Presets:** the "Default" of ReTerraForged 0.0.6, eight presets added in 0.0.32, and the classic TerraForged presets
  (Beautiful, Huge Biomes, Lite, Vanilla-ish). Each new preset is Default with a few settings changed:
  - **Archipelago:** island groups off the coasts, some volcanic.
  - **Wetlands:** wide swamps just above the sea, more lakes and wetlands.
  - **Towering Peaks:** a higher world with more and taller mountains.
  - **Endless Continent:** one land without oceans, with more rivers and lakes. Slower to generate: 16.6–16.9 ms per chunk against 12.5–13.2 for Default (1.21.1 NeoForge, 16 cores); the other seven are within 12.4–14.6.
  - **River Valleys:** many rivers in wide, winding valleys, with gorges and lakes.
  - **Lush Wilds:** denser forests, more grass and flowers.
  - **Rolling Plains:** wide, gentle plains and steppes with few mountains.
  - **Rugged Highlands:** plateaus, badlands, terraced hills and volcanoes instead of plains.
- **11 languages:** English, Russian, Ukrainian, Chinese, German, French, Spanish, Portuguese (Brazil), Japanese, Korean,
  Polish.

## What this fork fixes

**Generation as in 0.0.6.** The 1.21.1 port this fork started from changed the generation beyond what porting needed:
"Default", mountain climate (snowy peaks next to deserts), rivers, lakes, wetlands, structures on mountains, desert
slopes, bushes, cave biome depth. Every class of the mod was compared with the 0.0.6 source, and all of it is back. The
port's variant is gone (0.0.17): the "Original Generation (0.0.6)" switch was removed, every world generates as 0.0.6.
New chunks of worlds made with fork versions before 0.0.11 follow 0.0.6 too, so they may show a seam at the border.

**Bugs that broke the world:**
- a new world could not be created: generation crashed on a mixin error;
- on NeoForge the preset's trees and other features did not appear: the biome modifiers were in a registry that does
  not exist;
- on 26.x the mod's trees did not grow: since 26.1 grass is no longer in the `#dirt` tag, and since 26.3 the tree
  templates were read as air;
- a Fabric server hung while generating the Nether and the End, Fabric singleplayer hung on new chunks;
- `/locate`, explorer maps and dolphins froze the server for minutes;
- a memory leak: the generator's caches were never freed until the game was closed;
- extreme settings produced NaN terrain.

**Biome mods (Biomes O' Plenty and others through TerraBlender), 0.0.15:**
- a third of Biomes O' Plenty's biome kinds never generated (woodland, orchard, grassland, pasture, tundra and more);
  now 52 of 61 do, against 55 in a vanilla world;
- on Fabric 1.21.10 and 1.21.11 the regions of biome mods ignored the ReTerraForged map;
- on 26.3 chunks got other biomes than `/locate`, structures and the spawn expected, and the surfaces of biome mods
  (sand, mud, volcanic rock) seemed missing.

**Seams on cliffs, 0.0.17** ([#2](https://github.com/Vladeezyy/reterraforged/issues/2)): with mods that repaint
mountain surfaces, such as William Wythers' Overhauled Overworld, the rock of steep slopes came out in chunk-sized
squares and stripes.

**Other fixes, 0.0.32**:
- **Swamp Basins in another region's climate** ([#42](https://github.com/Vladeezyy/reterraforged/issues/42), a possible cause): a piece of the world could get the swamp basin and the climate of another region, as whole chunks of swamp in the middle of a desert, for example. Region ids repeat, and the cache of each generation thread looked a region up by its id alone. It depended on the order of the threads and was gone when the world was made again. Not reproduced, so the cause is not proven.
- **Generation speed, the same world:** a cache of 16 swamp columns and 64 terrain cells per thread instead of one entry, a column's rounded height computed once per chunk column, tiles made outside the shared lock, the feature step no longer waiting for a tile, tree templates read without a shared lock per tree, and a tree checking the structures of its own chunk only. The world stays block for block the same as in 0.0.31: the chunk fingerprints of the Default preset and of the preset with Swamp Basins and Lowland Swamps are the same 4501 lines as in 0.0.31. Measured in Testing: a few percent faster, nothing a player would notice.
- **Leaving a world cancels its unfinished tiles**, so they do not delay the first tile of the next world.
- **Trees from saplings keep the blocks under their roots** (glass, water, flowers, saplings, rails). It touched tree templates from datapacks with blocks beside the trunk on the bottom layer; the mod's built-in trees are not affected.
- **Noise in datapacks:** the number of octaves and terrace steps is limited (a huge or negative value hung or crashed generation); a strata selector below 0 no longer crashes the game; the `simplex_ridge` type can be written.
- **Other mods:** injections that would conflict with another mod at the same place are replaced with compatible ones (wrap instead of redirect, no capture of local variables), mixin fields are unique, and the spawn point search is replaced only in worlds with a preset. TerraBlender is an optional dependency. TerraBlender's diagnostic counters, which ran on every biome pick and wrote lines to the log with Darker Depths or Deeper Darker installed, now run only with `-Drtf.tbdiag=true`.
- **Snow Line Follows World Height** works only in the dimension the preset builds. Before, one scale applied to the whole game, so snow and ice moved in other mods' dimensions too.
- **Tile size is stored in the world** (`reterraforged/generation.json` in the world folder). Before, changing `tileSize` in `performance.conf` (or another config on a server) gave erosion seams between old and new chunks. Now the config applies to new worlds.
- **Preset screen:** the datapack export no longer runs on the render thread, so opening Create World, Done and Export As Datapack no longer freezes the game (the vanilla loading screen shows during the export). A stale preview calculation is stopped. Five hints describe their settings again (Plain Stone Erosion, the two lake start distances, Mountain Biome Usage, Volcano Biome Usage): their texts from 0.0.21 were lost in all 11 languages.
- **Generation version 1, new worlds only** (worlds made before keep every block; a preset from the player's library gets it when a new world is created; it comes with Generation Fixes): deep structures are built under rivers and lakes (strongholds, ancient cities, trial chambers; the structure rule refused them at any depth, so there were fewer strongholds and the Eye of Ender skipped their point); the rock strata follow the world's seed (before, they were the same in every world); a 64-bit seed gives its own terrain (before, only the lower 32 bits counted; seeds that fit 32 bits, text seeds included, give the terrain they gave before).
- **Structure settings and the datapack:** they no longer write whole structure sets into the world's datapack, so a world
  that had changed structure settings no longer fails to load after a structure mod is removed. The sets of datapacks from
  0.0.12–0.0.31 are skipped, and the settings apply to the sets the game has.
- **Preset datapack on both loaders:** exports write the biome modifiers for NeoForge and for Fabric. Before, a datapack of
  the other loader gave a world without the mod's trees, erosion, swamp surface and snow. Fabric also reads the NeoForge
  folder of an older datapack (see Known limitations for the other direction).
- **Erosion and island shores** ([#38](https://github.com/Vladeezyy/reterraforged/issues/38), generation version 1, new
  worlds): the erosion reads the biomes of the chunk, as the surface rules do, so sand no longer stays on steep slopes by
  islands.
- **Columns past 16.7 million blocks** (generation version 1, new worlds): they no longer come in pairs of equal columns.
- **Darker Depths and Deeper Darker** (behaviour change): in worlds without a preset, TerraBlender's own underground regions
  stay (see Compatibility).
- **Build:** the Loom and Architectury plugin versions are pinned (not snapshots); the CI token only reads.

**Other fixes, 0.0.31**:
- **Erosion leaves other mods' surface blocks alone** ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)): the erodible tag lists vanilla's dirt blocks by name, not the whole dirt tag. Regions Unexplored's Chalk Cliffs kept 29.1 % chalk grass (was 11.2 %, with 18.3 % strata rock). A datapack can add a mod's block to the tag. An existing world keeps the tag of its preset datapack, so the fix is for new worlds.
- **Island Horizontal Scale** ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)) no longer works backwards. **Steep shores** get a stony shore's climate, so beach sand stays off mountain slopes ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)) and Regions Unexplored's Chalk Cliffs generate ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)).
- **TerraBlender regions of other biome mods:** a biome's region is now a hash of its id, by the regions' weights. Regions Unexplored with Biomes O' Plenty: 89 of 121 biome kinds, was 76; on 26.x Biomes O' Plenty alone: 51 of 59, was 50. Worlds without TerraBlender are not affected.
- **Preset export on a started server** with Regions Unexplored or Biomes O' Plenty failed with `reterraforged:strata/depth is not valid in current registry set`. It works now.
- **Flower Forest** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37)) has as many flowers as vanilla: 2.125 patches a chunk instead of 1.5. On 26.2, three seeds of 32 × 32 chunks gave 60.3 flowers per 256 columns, vanilla 60.0. Plant Density scales them.
- **Fabric: a world's biome modifiers no longer carry over to the next world** of the same game session. The preset's modifiers were added to Fabric API's own list, which lives for the whole game: after leaving a ReTerraForged world, a vanilla world opened next got the preset's trees, and another ReTerraForged world got its grass, erosion and vegetation density twice. They are now applied from a copy of the list.
- **No crash in other dimensions with the preset's trees:** a mod's or a datapack's dimension with a birch forest, grove, windswept hills or old growth taiga, but without ReTerraForged terrain, stopped the server with a "Feature placement" crash (UnsupportedOperationException). There the trees now keep their usual chance.
- **A sapling's tree leaves the saplings around it**, as vanilla's does (#41 in 0.0.31): a tree grown from a sapling could replace nearby saplings and flowers with its trunk or leaves, so a tree farm lost saplings. It now replaces only what vanilla's trees replace: on 1.21.1 flowers stay too; from 1.21.11 on vanilla's trees take small flowers, and so do the mod's. World generation is unchanged.
- **Preset export in a folder whose name ends in "!"** (an instance called "Pack!", a Windows user "Name!"): the zip was written to the wrong path, so the export failed, and with such a temporary folder new worlds were made without the preset. A cleanup failure after a working export (Windows, an antivirus holding a file) no longer hides the export either.
- **River maps:** a river map was dropped 5 minutes after it was made even while in use, and made again under a lock that every generation thread waited on (with an infinite continent one map covers the whole world). It now lives while it is used and is made without stopping the threads that need other maps.
- **Another mod's generator with the mod's terrain** (a random state no ChunkMap set up): chunks generated while its context was being built could come out with a height of 0 (a flat sea). Threads now wait for the context.

**Tree and grass density, 0.0.30** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37), with Custom Biome Features on):
forests, taigas and jungles had half of vanilla's trees, because a tree could not grow on a column with grass or under
leaves. They now have about as many as vanilla's, evenly spaced, without large clearings. Groves (spruces), savannas
and bamboo jungles also have about as many trees as vanilla's, and birch forests are no longer mostly tall grass. New
sliders scale the grass, the trees and the other plants of all overworld biomes, those of other mods too (recognised by
the names of their features): 1 is the usual amount, 0 none. Trees per 256 columns of land (16 × 16 blocks), 0.0.29 → 0.0.30,
vanilla in brackets: taiga 2.95 → 5.58 (6.24), forest 2.49 → 5.62 (5.84), jungle 2.33 → 4.83 (5.14), grove 0.11 → 5.46 (5.55),
savanna 0.08 → 1.10 (0.94). The share of empty 16 × 16 squares in forests went from 3–5 % to 0.

**Other fixes, 0.0.30**:
- **1.21.11 with Terralith:** the terrain came out in steps of 8 blocks. The mod made a new density sampler on every pass over the noise settings, so Terralith's cached density function was split into several caches that each held one value per cell. Fixed on every version; only 1.21.11 showed it (newer lithostitched hides it). New chunks of an existing 1.21.11 world with Terralith will not match the old ones at the border.
- **26.2 with TerraBlender:** the preset could not be built, so a world could not be created; TerraBlender 26.2.0.0.2 wraps surface rules in its own rule, the mod now reads the rule inside.
- **26.1.2, 26.2, 26.3:** after a failed terrain tile the server never shut down; it now stops by itself in about a minute, as on 1.21.x.
- **Lowland Swamps** ([#28](https://github.com/Vladeezyy/reterraforged/issues/28)): it now also reaches dry and cold lowland. With Biomes O' Plenty and Terralith, Lowland Swamps 12 and Highest Swamps 12 (blocks), Terralith's Orchid Swamp went from 0.03 to 0.69 % of the land and Ice Marsh from 0.02 to 0.72 %; Biomes O' Plenty's swamps stay as they were; all swamps together 9.9 % of the land instead of 7.1 %.

**Forests, 0.0.29** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37), with Custom Biome Features on): near the
edge of a climate region a wrong formula left bare strips in forests; forests and dark forests had lost their leaf litter
(1.21.11 and newer). Also in 0.0.29, all from 0.0.6: the large spruces of fir and
redwood forests were never placed, carver caves ignored the Lava Level slider, EXPERIMENTAL continents did not start.

**Trees and swamps, 0.0.28** ([#34](https://github.com/Vladeezyy/reterraforged/issues/34)): the trees of
swamps, plains, savannas, meadows, windswept hills and snowy plains all stood on the corner of their chunk, a 16-block
grid; rivers kept vanilla's oaks by the water; swamp huts (and other mods' swamp structures) never generated, because
the wetlands counted as river for structures.

**Preset screen:**
- on 1.21.11+ the sliders changed nothing;
- the selected preset was not highlighted, edits and a re-applied preset were lost;
- "back" skipped a page;
- on 1.21.6+ the screen crashed the game;
- "Export As Datapack" of a preset exported before kept in the archive what the new settings had switched off, such as
  the erosion decorator ([#5](https://github.com/Vladeezyy/reterraforged/issues/5)).

The structure page and the sliders that did nothing before now work (horizontal scale of hills, dales, plateaus,
badlands, volcano scales). Settings the generation does not use (nor did 0.0.6) are shown as inactive. Presets read
from files are held to valid values, and a broken file in the presets folder no longer crashes the game.

## Known limitations

- **Far from the center** (past 16.7 million blocks, 2^24) terrain coordinates lose precision. In worlds made without
  generation version 1 the ground comes in pairs of equal columns (steps of 2 blocks). New worlds have no pairs, but the
  float precision still leaves fine roughness. No crash and no holes. Old worlds keep the steps, since changing them would
  change their terrain. The coordinates are floats, as in the original ReTerraForged, and the noise distortion gets coarse
  already around 8 million blocks: keep the world border within 8 million blocks (`/worldborder`).
- **A Fabric world made with a preset datapack exported on NeoForge** (the preset zip of the releases is one) had none of the mod's trees, erosion, swamp surface or snow. With 0.0.32 its new chunks get them, so such a world can show a seam between the chunks made before and after the update.
- **A preset datapack exported on Fabric by an older version, used on NeoForge:** it has no NeoForge biome modifiers, so the
  world has no trees, erosion, swamp surface or snow of the mod. NeoForge logs a warning, and an operator who joins is told
  in chat. Export the preset again with this version. Not fixed: reading those modifiers would need a mixin into NeoForge's
  application of the modifiers.
- **Seeds:** in worlds made without generation version 1 the terrain uses the lower 32 bits of the seed, so seeds `x` and `x + 2^32` have nearly
  the same terrain there (99 % of the heights). Generation version 1 (new worlds, with Generation Fixes) uses the whole 64-bit seed. Switching old
  worlds to the upper bits would change every world made from a 64-bit seed.
- **`tileSize` in `performance.conf`** also changes a few surface blocks (7 to 11 % of the surface chunks have some difference), as erosion runs per
  tile. A world keeps the tile size it was made with (`reterraforged/generation.json`); the config applies to new worlds. Keep the same value on
  all machines of a modpack. Above 5 it is cut to 5: 8 needed about 9 GB.
- **Lowland Swamps with biome mods:** swamps go where the terrain is low, mostly river valleys, because the setting does not reshape the land. With
  Biomes O' Plenty and Terralith at 12 / 12 they cover about 10 % of the land and only about 3 % of them are wide. Wide swamps come from Swamp Basins (see
  [#28](https://github.com/Vladeezyy/reterraforged/issues/28)).
- **Mineshafts under river beds at sea level** are still not built: their start point is above the depth threshold.
- **The surface on beaches and slopes** sometimes takes a biome without the erosion of its tile, as in the original. Not fixed.

## Problems and fixes in detail

<details>
<summary><b>Swamp Basins: the climate of another region</b> (0.0.32, a possible cause of <a href="https://github.com/Vladeezyy/reterraforged/issues/42">#42</a>)</summary>

**Problem.** A piece of the world could get the swamp basin and the climate of another region: whole chunks of swamp in the middle of a desert, for example. It depended on the order of the generation threads and was gone when the world was made again.

**Cause.** Region ids repeat from region to region, and the cache of each generation thread looked a region up by its id alone.

**Fix.** Fixed in 0.0.32. The cause was not reproduced, so it is a possible cause of #42, not a proven one.

</details>

<details>
<summary><b>Strongholds under rivers and lakes</b> (0.0.32, generation version 1)</summary>

**Problem.** Under rivers and lakes no strongholds, ancient cities or trial chambers were built at any depth. There were fewer strongholds, and the Eye of Ender skipped their point.

**Cause.** The structure rule refused deep structures at any depth.

**Fix.** Generation version 1 builds them under rivers and lakes. It applies to new worlds only: a world made before keeps every block. Its rock strata also follow the seed (see below).

</details>

<details>
<summary><b>Rock strata and 64-bit seeds</b> (0.0.32, generation version 1)</summary>

**Problem.** The rock strata were the same in every world. The terrain of a 64-bit seed used only its lower 32 bits, so seeds `x` and `x + 2^32` had nearly the same terrain.

**Cause.** The strata did not depend on the seed, and the terrain read only the lower 32 bits of it.

**Fix.** In generation version 1 (new worlds) the strata follow the seed, and a 64-bit seed gives its own terrain. Seeds that fit 32 bits, text seeds included, give the terrain they gave before.

</details>

<details>
<summary><b>tileSize: seams after a config change</b> (0.0.32)</summary>

**Problem.** Changing `tileSize` in `performance.conf` (or another config on a server) gave erosion seams between the chunks made before and after the change.

**Cause.** The world did not store the tile size it was made with.

**Fix.** The tile size is stored in `reterraforged/generation.json` in the world folder. The config applies to new worlds.

</details>

<details>
<summary><b>Datapack noise: huge values hung the generation</b> (0.0.32)</summary>

**Problem.** A datapack with a huge or negative number of octaves or terrace steps hung or crashed the generation. A strata selector below 0 crashed the game.

**Cause.** The values were not limited.

**Fix.** The number of octaves and terrace steps is limited. A strata selector below 0 no longer crashes the game, and the `simplex_ridge` type can be written.

</details>

<details>
<summary><b>Sapling trees replaced the blocks under their roots</b> (0.0.32)</summary>

**Problem.** A tree grown from a sapling replaced the blocks under its roots (glass, water, flowers, saplings, rails). It happened with tree templates from datapacks that have blocks beside the trunk on the bottom layer.

**Cause.** Such templates have blocks in the bottom layer beside the trunk, and the tree replaced them.

**Fix.** The tree no longer replaces the blocks under its roots. The mod's built-in trees were not affected.

</details>

<details>
<summary><b>Other mods' mixins</b> (0.0.32)</summary>

**Problem.** Some injections would conflict with another mod's injections at the same place.

**Cause.** Redirects and captured local variables.

**Fix.** Wrap is used instead of redirect, local variables are not captured, and the mixin fields are unique.

</details>

<details>
<summary><b>Snow Line Follows World Height in other dimensions</b> (0.0.32)</summary>

**Problem.** The snow line moved in other mods' dimensions too: snow and ice shifted there.

**Cause.** The setting had one scale for the whole game.

**Fix.** The scale applies only in the dimension the preset builds.

</details>

<details>
<summary><b>Preset screen: the game froze on Create World and export</b> (0.0.32)</summary>

**Problem.** Opening Create World, Done or Export As Datapack froze the game.

**Cause.** The datapack was exported on the render thread.

**Fix.** The export runs off the render thread, and the vanilla loading screen shows while it runs. A stale preview calculation is stopped.

</details>

<details>
<summary><b>Structure settings: a world did not load after a mod was removed</b> (0.0.32)</summary>

**Problem.** A world with changed structure settings did not load after a structure mod was removed.

**Cause.** The settings wrote whole structure sets into the world's datapack. Such a file names every structure of the set and
the set of its exclusion zone, so the missing mod made the file fail.

**Fix.** The datapack has no structure sets. The preset's entries are applied to the sets that are loaded, and a set whose
mod is gone is skipped. The files of datapacks from 0.0.12–0.0.31 are left out.

</details>

<details>
<summary><b>A preset datapack on the other loader had no trees or erosion</b> (0.0.32)</summary>

**Problem.** A preset datapack exported on one loader and used on the other gave a world without the mod's trees, erosion,
swamp surface and snow.

**Cause.** The export wrote the biome modifiers into one loader's folder only (`neoforge/biome_modifier` or
`forge/biome_modifier`), and each loader read only its own. The preset file of the releases is a NeoForge export.

**Fix.** Exports write both folders. Fabric also reads the NeoForge folder of an older datapack. An older Fabric export on
NeoForge is not fixed: NeoForge logs a warning, and an operator who joins is told in chat. Export the preset again.

</details>

<details>
<summary><b>Sand on steep island slopes</b> (0.0.32, generation version 1, <a href="https://github.com/Vladeezyy/reterraforged/issues/38">#38</a>)</summary>

**Problem.** Sand stayed on steep slopes by islands, and near shores and biome borders the surface took another biome than
the chunk's.

**Cause.** The erosion asked the biome source again, with the terrain computed without the tile, so it got another biome than
the surface rules, which read the chunk's own biomes.

**Fix.** In generation version 1 the erosion reads the biomes the chunk has, as the surface rules do. Older worlds keep the
old lookup.

</details>

<details>
<summary><b>Pairs of equal columns past 16.7 million blocks</b> (0.0.32, generation version 1)</summary>

**Problem.** Past 2^24 (16 777 216) blocks from the centre the ground came in pairs of equal columns up to the world border.

**Cause.** The terrain is computed with float coordinates. A float cannot hold an odd coordinate that far out, so it was
rounded to an even one, and the column had the cell of its neighbour.

**Fix.** In new worlds (generation version 1) a column the float cannot hold takes its cell from the floats on both sides of
it. Worlds made before keep the pairs. The float precision still leaves fine roughness beyond about 8 million blocks (see
Known limitations).

</details>

<details>
<summary><b>Differences between versions</b> (0.0.32)</summary>

- **1.21.5 and newer:** a tree grown from a sapling takes the small flower under its roots, as vanilla trees do: the `#replaceable_by_trees` tag includes `#small_flowers` there.
- **26.3:** the column height cache is not needed and is not ported: the engine computes such functions once per column itself.

</details>

<details>
<summary><b>Seams along chunk borders on cliffs</b> (0.0.17, <a href="https://github.com/Vladeezyy/reterraforged/issues/2">#2</a>)</summary>

**Problem.** With William Wythers' Overhauled Overworld the volcanic Stony Peaks had patches of diorite and stone cut
off along chunk borders in the middle of its blackstone, tuff and basalt; other mods that repaint slopes looked the
same. Higher vertical or horizontal scales made it worse.

**Cause.** The erosion decorator (rock, gravel and scree on steep slopes) ran as the first feature of each chunk and
rewrote up to 32 blocks of its columns. The features of neighbour chunks reach a few blocks into a chunk, and those
of neighbours generated earlier were already there: the erosion overwrote them, while those of neighbours generated
later stayed. Whether a patch survived depended on the order the chunks were generated in. 0.0.6 has the same bug.

**Fix.** The erosion runs as the chunk's surface is built, before any feature of it or of a neighbour. The terrain
does not change (the same heights everywhere); in a world without such mods about 0.1 - 0.2 % of the ground blocks
differ, all of them at chunk borders, where the result used to depend on the generation order. Worlds made before
keep their chunks; chunks whose surface an older version already built are eroded the old way, so there is no seam
at the edge of the explored area. On 12 x 12 chunks of Stony Peaks with the mod, the ground block changed across a
chunk border 1.41 times as often as inside a chunk before, 0.82 times after; with the vertical scale at 1.5 and the
mountains' at 2 (as in the report) 1.93 before, 0.82 after.

</details>

<details>
<summary><b>Biome mods through TerraBlender</b> (0.0.15)</summary>

ReTerraForged picks vanilla biomes by its own biome types, and gives the biome tables only some temperature and
humidity pairs: on land none is warm with neutral humidity. That suits vanilla's biomes, but Biomes O' Plenty places
woodland, old growth woodland, grassland, orchard and more exactly there: 39 of its 61 overworld kinds generated (55
in a vanilla world with the same seed). The regions of other mods are now searched with the temperature and humidity
moved by up to about one band by two smooth noises; vanilla's region and worlds without TerraBlender are unchanged.
Now 52 of 61.

On Fabric 1.21.10 and 1.21.11 the hook that gives TerraBlender the ReTerraForged biome regions was never applied
(its target stayed in Mojang names in the jar), and TerraBlender chose them by its own noise. On 26.3 the game fills
a chunk's biomes from climate volumes, past that hook, so chunks and `/locate` disagreed at 130 of 289 places with
Biomes O' Plenty. Both are fixed: 288 of 289 places agree.

Tested with Biomes O' Plenty on 1.21.1, 1.21.11 and 26.3, NeoForge and Fabric: `MODS=biomes-o-plenty
tools/servertest.sh` compares a ReTerraForged world with a vanilla world of the same seed and mods (biome kinds, their
share of the land, how much of them is under water, their own surface blocks).

</details>

<details>
<summary><b>Bee nests on the mod's trees</b> (0.0.12)</summary>

**Problem.** ReTerraForged trees never had bee nests, neither in the port nor in the original 0.0.6. Plains, flower
forests and meadows, where vanilla trees carry nests, had no bees at all.

**Cause.** The mod's trees are templates, not vanilla trees. The vanilla bee nest decorator only places a nest just
below the lowest leaves, and the templates have leaves all around the trunk there, so it never found a spot. On top of
that, per-biome decorators were looked up by the registry name instead of the biome name.

**Fix.** A bee nest decorator of its own for templates: the nest goes on the trunk, facing south, wherever there is
room, with 2–3 bees. The chances are vanilla's: plains 5 %, flower forests 2 %, forests and birch forests 0.2 %,
meadow trees always. Saplings next to flowers (bone-mealed ones too) get nests as usual. The self test checks both.

</details>

<details>
<summary><b>26.x: the mod's trees did not grow</b> (0.0.12)</summary>

**Problem.** On 26.1.2, 26.2 and 26.3 ReTerraForged trees almost never appeared and forests stood empty. On 26.x the
erosion did not touch grassy slopes either.

**Cause.** Two separate reasons. Since 26.1 Minecraft moved grass, podzol and mycelium out of the `#dirt` tag into
`#grass_blocks`: the mod checked the soil with `#dirt`, so it did not place trees on grass, and the erosion did not
consider grass erodible. Since 26.3 Minecraft reads blocks from NBT by the keys `id` and `properties` instead of `Name`
and `Properties`: every block of the tree templates became air, and pasting a tree placed nothing.

**Fix.** On 26.x the soil is checked with `#substrate_overworld`, which is exactly the old `#dirt`. The template
palette is converted to the new format when read. The self test now checks that trunks really stand in the forest, not
only that the biome lists the mod's trees: the old check passed while there were no trees.

</details>

<details>
<summary><b>The generation differed from the original 0.0.6</b> (0.0.11–0.0.12)</summary>

**Problem.** Worlds did not look like ReTerraForged: snowy peaks next to deserts, warm oceans right against deserts,
different rivers and lakes, villages on mountains.

**Cause.** The 1.21.1 port this fork started from changed the generation beyond what porting needed:
- "Default" was replaced by an unfinished second preset;
- a mountain region took a single biome from the climate at the region's center;
- plains and hills had another weirdness (it picks biome variants);
- rivers, lakes and wetlands faded out on high ground and in mountains;
- structures were placed on mountains;
- desert slopes turned into sandstone and terracotta;
- bushes were added to several biomes;
- cave biomes sat higher;
- the surface used another random source.

Comparing every class with the 0.0.6 source found two more: the slope gradient was no longer capped at 1 (rock and
snow lay differently), and skipped continents in the open ocean got another continent's rivermap.

**Fix.** All of it is back as in 0.0.6. Since 0.0.17 the port's variant is removed altogether (the "Original Generation
(0.0.6)" switch and its preset): every world generates as 0.0.6.

</details>

<details>
<summary><b>NeoForge: the preset's trees did not appear</b> (0.0.10)</summary>

**Problem.** On NeoForge a world with the preset grew vanilla trees instead of the mod's.

**Cause.** The biome modifiers (tree replacement, forest grass, removing vanilla springs) were in the
`forge:biome_modifier` registry. Forge had it, NeoForge does not, so the modifiers were silently never applied.

**Fix.** On NeoForge the modifiers are in its own `neoforge:biome_modifier` registry.

</details>

<details>
<summary><b>Preset screen: sliders, highlight, edits</b> (0.0.10)</summary>

**Problem.**
- On 1.21.11 and later no slider changed anything.
- The selected preset was not highlighted.
- A re-applied preset and edits of a built-in preset were lost.
- "<<" on the rivers page skipped a page, and on the world page it went back to the top of the list.

**Cause.**
- In 1.21.11 the vanilla slider's `setValue` became protected, and the mod's method of the same name overrode it, so
  values no longer reached the settings.
- The preset list did not draw its selection and dropped it on a click on any button.
- The create world screen did not apply a datapack with the same name a second time.

**Fix.**
- The sliders work on every version.
- The selected preset is highlighted with a yellow "»".
- Every applied preset gets its own datapack name, and edits of built-in presets are kept.
- "Back" goes exactly one page back and returns to the selected preset.

</details>

<details>
<summary><b>Sliders and pages that did nothing</b> (0.0.11)</summary>

**Problem.** The structure page (spacing, separation, salt, disabled) had no effect, neither in the port nor in 0.0.6.
The horizontal scale of hills, dales, plateaus, badlands and torridonian, and the base and vertical scale of volcanoes
changed nothing.

**Cause.** The structure settings never reached the world's datapack. Those terrains had their noise sizes hard-coded.

**Fix.** Changed structure sets are written into the preset datapack. The terrain and volcano scales work, and at 1 the
terrain is as before. Settings the generation does not use (nor did 0.0.6) are shown as inactive, with a tooltip.

</details>

<details>
<summary><b>/locate, explorer maps and dolphins froze the server</b> (0.0.11)</summary>

**Problem.** Finding a witch hut with `/locate` took more than 10 minutes. Explorer maps and dolphins froze the server.

**Cause.** For every place checked (thousands of them) the port built a whole terrain tile with erosion, when a single
height was needed.

**Fix.** The height of a place is read straight from its cell, as in 0.0.6. Finding every vanilla structure around the
spawn now takes about 40 seconds.

</details>

<details>
<summary><b>A new world could not be created</b></summary>

**Problem.** The game crashed right when creating a world with the preset.

**Cause.** The upstream's last commit added a non-`private` static method to the `MixinNoiseChunk` mixin, which Mixin
does not allow.

**Fix.** The method moved to a separate class, `C2MECompat`.

</details>

<details>
<summary><b>Fabric: hangs in the Nether, the End and on new chunks</b></summary>

**Problem.** A Fabric server hung while generating the Nether and the End. Fabric singleplayer hung on new chunks with
`Index -1 out of bounds`.

**Cause.** The mod's erosion and snow features got into the biomes of other dimensions and threw an exception there,
so the chunk never finished. Separately, Fabric did not rebuild the generator's feature list after the mod changed the
biomes.

**Fix.** Outside the ReTerraForged generator these features do nothing. The feature list is rebuilt after the biomes
change.

</details>

<details>
<summary><b>The preset screen crashed the game on 1.21.6+</b></summary>

**Problem.** On 1.21.6 and later the preset screen crashed with `Can only blur once per frame` and lost all its text. In
a small window the buttons were cut off.

**Cause.** The screen drew its background again on top of the one Minecraft had already drawn, so the blur ran twice
per frame, which 1.21.6 forbids. In a narrow window the right column shrank until the buttons did not fit.

**Fix.** The background is drawn once, and the right column is at least 150 pixels wide.

</details>

<details>
<summary><b>/reload did not reload the tree templates</b></summary>

**Problem.** After `/reload` or a datapack change the tree templates stayed the old ones, everywhere except
NeoForge 1.21.1.

**Cause.** The reload hooked the internal name of a compiled Minecraft lambda, which changes from version to version.

**Fix.** The templates are read again whenever the server's resource manager changes. Mixins tied to full constructor
signatures were replaced by version-independent ones.

</details>

<details>
<summary><b>The mod loaded on any Minecraft version</b></summary>

**Problem.** A jar for one version loaded on another and crashed there with an unclear error. `neoforge.mods.toml` said
the mod was version 0.0.6 instead of the real one.

**Fix.** The metadata has exact Minecraft and loader version ranges. A wrong version is rejected at once with a clear
message.

</details>

<details>
<summary><b>Broken preset files and extreme values</b> (0.0.9–0.0.10)</summary>

**Problem.** A foreign or damaged file in the presets folder crashed the game when the preset screen opened. Values no
slider gives (for example, edited by hand) broke the world.

**Fix.** A broken file is skipped with a message in the log, and the other presets load. Values from files are held to
the slider ranges, while the built-in presets stay as they are.

</details>

<details>
<summary><b>NaN terrain</b> (0.0.9)</summary>

**Problem.** Some settings broke the terrain: holes down to bedrock or a flat world at sea level.

**Cause.** A division by zero somewhere, and the resulting NaN spread through all later calculations. This happened
with a horizontal scale of zero, equal ocean/land transition points, rivers of zero length on tiny continents, and the
smallest global scale near the world border.

**Fix.** All of these are guarded, and a last check turns a height that is not a number into sea level. The stress test
runs every setting at its extremes and hundreds of random combinations.

</details>

<details>
<summary><b>Memory leak</b> (0.0.9)</summary>

**Problem.** The game's memory grew with every preview rebuild and every world joined. With heavy settings the game
ran out of memory.

**Cause.** The generator's caches (terrain tiles, rivermaps) were registered in a global list and in the cleanup
scheduler and were never freed until the game closed.

**Fix.** The scheduler holds the caches by a weak reference. A cache nobody needs any more (an old preview, a world
that was left) is garbage collected, and its cleanup stops.

</details>

<details>
<summary><b>ModernFix: a crash when creating a world</b> (0.0.23)</summary>

**Problem.** With ModernFix (or ModernFix-mVUS) the game crashed right after Create New World: "Exception initializing
level" ([#19](https://github.com/Vladeezyy/reterraforged/issues/19)).

**Cause.** The mod's access widener made `NoiseChunk.wrap` public. ModernFix replaces that method and keeps it
protected; Mixin does not allow lowering the access, so Minecraft could not load `NoiseChunk` at all.

**Fix.** The access widener no longer touches the method: the mod calls it only from its own mixin, which needs no
wider access. Tested on 26.1.2 Fabric with C2ME and ModernFix-mVUS.

</details>

<details>
<summary><b>C2ME's OpenCL module: a crash when creating a world</b> (0.0.24)</summary>

**Problem.** With C2ME's OpenCL module (C2ME OpenCL Acceleration) the game stopped right after Create New World:
"OpenCL codegen failed" ([#19](https://github.com/Vladeezyy/reterraforged/issues/19)).

**Cause.** The module compiles each world's terrain for the GPU and only knows vanilla's density functions. On
ReTerraForged's it gives up, and unless `allowIncompatibilityFallback` is set in `config/c2me.toml` (off by default) it
stops the server.

**Fix.** For a world with ReTerraForged terrain the mod makes the module fall back as if that setting were on: the
overworld generates on the CPU (still multi-threaded by C2ME), the Nether and the End keep the GPU. Nothing to set.
Tested on 1.21.1, 1.21.11, 26.1.2 and 26.2 servers (NeoForge and Fabric where the module exists). There is no 26.3
build of the module yet.

</details>

<details>
<summary><b>Ecliptic Seasons: no rain</b> (0.0.25)</summary>

**Problem.** With Ecliptic Seasons neither rain nor its sound came in ReTerraForged worlds, even with `/weather rain`
([#22](https://github.com/Vladeezyy/reterraforged/issues/22)).

**Cause.** ReTerraForged's presets use the legacy random source (the surface of 0.0.6). With it vanilla looks each
column's biome up at y 0 while building the surface, and Ecliptic Seasons keeps that biome as the column's surface
biome. Under most land in ReTerraForged's terrain y 0 is a cave biome (dripstone caves), so it decided there was no
precipitation.

**Fix.** Other mods now get the biome at the surface there; vanilla's own use of the y 0 biome is unchanged, so the
terrain is the same. Chunks generated before the update keep the biome Ecliptic Seasons stored for them. 26.3 looks
the biome up at the surface already.

</details>

## Compatibility

- Works with **Distant Horizons**, **Iris** and **Sodium**.
- **Ecliptic Seasons**: rain, snow and their sound work since 0.0.25 (tested with 0.15.5 on 1.21.1 NeoForge).
- **TerraBlender** and biome mods using it are supported: biome regions follow the ReTerraForged map. Tested with
  **Biomes O' Plenty** and **Terralith** together on 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 (NeoForge and Fabric). On
  26.3 install TerraBlender yourself: Biomes O' Plenty needs it but its Modrinth page does not list it. With Terralith
  the vanilla forests grow Terralith's trees instead of the ReTerraForged ones: Terralith rewrites their feature lists.
- For cave biome mods there is a cave compatibility mode on the Caves page.
- **William Wythers' Overhauled Overworld** 2.6.7 (1.21.1 NeoForge and Fabric): its biomes and surfaces generate,
  no seams on cliffs since 0.0.17. Its forests grow its own trees instead of the ReTerraForged ones. On Fabric it
  needs Fabric Loader 0.18.5 and Fabric API 0.116.12 or newer.

- **ModernFix** and **C2ME**: work together with the mod since 0.0.23 (tested on 26.1.2 Fabric with C2ME 0.4.0-alpha
  and ModernFix-mVUS 5.27.19). C2ME's **OpenCL module** (C2ME OpenCL Acceleration) only accelerates vanilla terrain:
  since 0.0.24 a ReTerraForged world falls back on its own, the overworld generates on the CPU (still multi-threaded
  by C2ME) and the Nether and the End keep the GPU. With 0.0.23 set `allowIncompatibilityFallback = true` under
  `[openclAccel]` in the existing `config/c2me.toml` (a file written from scratch without `version = 3` is reset by
  C2ME). On 1.21.1 (0.0.32) the self test passes with C2ME.
- **TerraBlender** is an optional dependency since 0.0.32: the mod loads without it, and worlds without it are not affected.
- **Other mods' mixins** (0.0.32): injections that would conflict with another mod at the same place are replaced with compatible ones (wrap instead of redirect, no capture of local variables).
- **Spawn point** (0.0.32): the search is replaced only in worlds with a ReTerraForged preset; other worlds keep vanilla's.
- **Darker Depths and Deeper Darker** (0.0.32): with either installed, TerraBlender's diagnostic counters ran on every biome pick and wrote lines to the log. They run only with `-Drtf.tbdiag=true` now. In worlds without a preset (vanilla generation with TerraBlender) the mod no longer
  replaces their underground regions: TerraBlender's own choice stays, as without the mod. Before, the mod replaced them there
  too. Worlds with a preset are unchanged.

## Testing

Every release is checked on every Minecraft version it is published for (currently five) and both loaders before it is published. The tests are part
of the mod (`rtf` commands, enabled only in test runs) and of the repository (`tools/*.sh`), so anyone can repeat them.

The CI (`.github/workflows/build.yml`) runs on every push that changes more than the docs, and on every pull request. A world
made by the last release for the same Minecraft version opens with this build, and the last release's preset gives the same
world with this build as with the release jar on the same runner. A preset exported on one loader runs on the other. The self
test runs on the Fabric Loader and Fabric API the mod is built against, as well as on the newest ones. NeoForge and Fabric
must make the same world. `tools/extramods.sh` installs the other mods of the test runs (`MODS=`); Biomes O' Plenty comes
with TerraBlender and GlitchCore on every version.

<details>
<summary><b>1. Server self test</b>: <code>tools/servertest.sh</code>, 42 checks</summary>

A real dedicated NeoForge or Fabric server is installed, the preset datapack is exported, a new world is created with
the "Default" preset and `rtf selftest` runs in it. Output of 1.21.11 NeoForge (the list below is not complete: it shows 18 of the 42 checks):

```
[RTF selftest] ok   overworld uses the ReTerraForged generator
[RTF selftest] ok   the forest grows ReTerraForged trees, not vanilla ones [reterraforged:forest_trees]
[RTF selftest] ok   trees stand in the forest at -1920, 384 (150 trunk columns)
[RTF selftest] ok   the structure page sets the villages' spacing, separation and salt
[RTF selftest] 289 height samples in 11988 ms, ground 10 .. 154, water at 194 of 289 points
[RTF selftest] 81 chunks fully generated in 2608 ms
[RTF selftest] ok   terrain has relief (ground 10 .. 154)
[RTF selftest] ok   terrain reaches above sea level
[RTF selftest] ok   there is water (194/289)
[RTF selftest] ok   terrain stays inside the world height
[RTF selftest] ok   overworld bedrock floor is whole (holes: 0)
[RTF selftest] ok   spawn is on dry land (2640, 93, 672: Sand)
[RTF selftest] ok   bee nests grow on the mod's trees
[RTF selftest] ok   saplings grown next to flowers get bee nests (7 of 199)
[RTF selftest] ok   a village is found
[RTF selftest] ok   the village stands on land above the sea
[RTF selftest] ok   nether bedrock floor is whole (holes: 0)
[RTF selftest] ok   nether bedrock roof is whole (holes: 0)
[RTF selftest] ok   nether is made of netherrack as usual (22/90)
[RTF selftest] ok   the end island is there
[RTF selftest] PASS all 18
```

The script also fails on any error of the mod in the server log.

</details>

<details>
<summary><b>2. Structure test</b>: <code>rtf structures</code></summary>

Every vanilla structure is searched for near the spawn, generated and measured: how many pieces float in the air or
are buried, and for villages, how many path blocks the streets have. A structure "not near" is fine (biomes decide
that); a floating or buried village is a failure.

```
[RTF structures] village_plains at 3323 52 -819: 110 pieces, 0 floating, 0 buried, 1420 path blocks, terrain badlands
[RTF structures] village_desert at 2177 53 549: 79 pieces, 0 floating, 0 buried, 1040 path blocks, terrain coast
[RTF structures] village_taiga at 4199 67 -1020: 92 pieces, 0 floating, 0 buried, 558 path blocks, terrain hills
[RTF structures] monument at 1632 39 816: 1 pieces, terrain deep_ocean
[RTF structures] stronghold at 1758 2 -1201: 168 pieces, terrain deep_ocean
[RTF structures] trial_chambers at 2222 -56 574: 254 pieces, terrain coast
[RTF structures] PASS: 15 of 26 structures found in 49 s
```

The client test then flies to the village and takes a picture (1.21.11 NeoForge):

![Desert village found by the structure test](docs/test-village.jpg)

</details>

<details>
<summary><b>3. Preset screen click test</b>: <code>tools/clienttest.sh &lt;loader&gt; gui</code></summary>

A real game client is started and driven by mouse events, as a player would: World tab → Customize → click
"Default" → a click on the continent scale slider → `>>` through every page (each scrolled down first) → `<<` back,
checking that every page is still scrolled where it was left → Done → the world is created, and the generator must
use the value set by the click. Then a second round with another value. 12 screenshots per run.

```
[RTF clienttest] click Button 'Customize' at 480 80 on CreateWorldScreen
[RTF clienttest] selected after the click: Default
[RTF clienttest] continent scale 3000 -> 2530 (slider click)
[RTF clienttest] pages with >>: [WorldSettingsPage, SurfaceSettingsPage, CaveSettingsPage, ClimateSettingsPage, ...]
[RTF clienttest] round 0: the world will use the preset with continent scale 2530
```

| After the slider click | The last page |
|---|---|
| ![](docs/test-gui-slider.jpg) | ![](docs/test-gui-last-page.jpg) |

</details>

<details>
<summary><b>4. Stress test</b>: <code>tools/stresstest.sh</code></summary>

`rtf stress` runs inside a real server. Every setting of the preset screen is tried at both ends of its slider, along with every built-in preset, everything at minimum, everything at maximum and random combinations. Each variant goes through the preset codec, the datapack registries of a new world, the preview and the world tiles with every filter, under a time limit. Results of 0.0.13 (1.21.1 NeoForge):

| Mode | Variants | OK | Failed | Hung | Time |
|---|---:|---:|---:|---:|---:|
| default: every slider end, the presets, 40 random combinations | 404 | 404 | 0 | 0 | 65 s |
| `abuse`: NaN, ±infinity, negative and huge numbers, as a broken preset file can hold them | 935 | 935 | 0 | 0 | 126 s |

```
stress: 404 variants in 65 s: 404 ok, 0 failed, 0 hung; slowest terrain().general.terrainSeedOffset = 0 (1338 ms)
stress: 935 variants in 126 s: 935 ok, 0 failed, 0 hung; slowest world().continent.continentSkipping = 0.0 (720 ms)
```

`effects` checks that every setting reaches the world: each value is compared with Default in the terrain cells and in the datapack. 153 of 167 settings change the world. The other 14 do nothing in the original 0.0.6 either, and the preset screen shows them as inactive: ore compatible stone only, plain stone erosion, mountain and volcano biome usage, five branch river sizes, the lake start distances, the island control points and the lava level.

</details>

<details>
<summary><b>5. Performance</b>: <code>tools/benchmark.sh</code>, spark and Java Flight Recorder</summary>

`tools/benchmark.sh` starts a real NeoForge server twice on the same seed: with the "Default" preset (ReTerraForged) and without it (vanilla generation, the baseline). In each world `rtf bench` first warms up, then generates a square of chunks far from the spawn to the full status, with one thread per core asking for chunks while the server keeps ticking. The server runs with [spark](https://spark.lucko.me) (`spark tps` during the generation, printed to the console, nothing uploaded) and a Java Flight Recorder recording.

![World generation speed](docs/benchmark.png)

On 1.21.1 ReTerraForged is about 9 % slower than vanilla. The mod computes a whole continent map, erosion and filters on top of vanilla's work. 0.0.13 closed a part of the gap: the erosion asked every cell's terrain for its erosion modifier through a chain of delegates, which took 4.5 % of all generation time in 0.0.12. On 26.3, with Mojang's new compiled density functions, it is as fast as vanilla.

| One run | Chunks | Time | Chunks/s | Per chunk | Heap in use |
|---|---:|---:|---:|---:|---:|
| 1.21.1 vanilla | 2304 | 27.1 s | 84.9 | 11.8 ms | 958 MB |
| 1.21.1 ReTerraForged 0.0.13 | 2304 | 30.0 s | 76.7 | 13.0 ms | 1193 MB |
| 26.3 vanilla | 4096 | 15.5 s | 264.0 | 3.8 ms | 1101 MB |
| 26.3 ReTerraForged 0.0.13 | 4096 | 15.2 s | 270.1 | 3.7 ms | 1165 MB |

**Generation speed, 0.0.32** (1.21.1 NeoForge, 1024 chunks on 16 cores, the builds alternated; ms per chunk): the Default preset 15.2 in 0.0.31 against 14.8 in 0.0.32 (−2.6 %, within the spread of the runs); the preset with Swamp Basins and Lowland Swamps 15.3 against 14.6 (−4.6 %); vanilla on the same machine 13.4. A 4-core server: no change (25.1 against 25.4 on average over 5 runs, spread 2 ms). The generation is a few percent faster, nothing a player would notice, and the world stays block for block the same.

**Tile cache, 0.0.30.** A tile used to be thrown out after its 64th chunk, and the neighbours made it again: 47 % of the tiles in `rtf bench 96` were
made twice. Now none are: at 2 GB 53.1 chunks/s against 44.6 (heap peak 1615 MB against 1664), at 3 GB 60.2 against 58.8 (1786 MB against 2575). `rtf bench 32` at
3 GB, four runs each in turns: 54.2 chunks/s against 50.6, within the noise of a run that short. Vanilla in the same conditions: 61.0 chunks/s, ReTerraForged 55.7
(TPS over the minute 17.76 and 17.74, tick median 5.3 and 5.7 ms, 95 % 49.4 and 58.9 ms).

**spark during the generation** (`spark tps`, the same runs). 16 threads ask for chunks at once, far more than any player flying with elytra does, so TPS drops for the time of the test in vanilla as well and is back to 20 right after it:

| | TPS 5 s / 10 s / 1 min | Tick min / median / 95 % / max, ms | CPU (process) |
|---|---|---|---|
| 1.21.1 vanilla | 7.4 / 10.8 / 17.5 | 0.3 / 9.2 / 81.0 / 110.3 | 28 % |
| 1.21.1 ReTerraForged | 8.2 / 11.6 / 17.8 | 0.3 / 7.6 / 102.2 / 121.2 | 31 % |
| 26.3 vanilla | 10.2 / 13.5 / 18.5 | 0.1 / 3.5 / 20.3 / 45.7 | 49 % |
| 26.3 ReTerraForged | 9.7 / 13.0 / 18.4 | 0.1 / 3.9 / 21.5 / 42.8 | 50 % |

Raw output of spark (26.3, ReTerraForged):

```
> TPS from last 5s, 10s, 1m, 5m, 15m:
 9.65, 13.02, 18.36, 19.65, 19.88

> Tick durations (min/med/95%ile/max ms) from last 10s, 1m:
 0.1/3.9/21.5/42.8;  0.1/3.9/21.5/42.8

> CPU usage from last 10s, 1m, 15m:
 57%, 53%, 53%  (system)
 50%, 53%, 53%  (process)
```

**Where the time goes** (Java Flight Recorder, `jfr view hot-methods`, share of all samples). Vanilla's aquifers and noise still take most of the time. The hottest method of the mod is the erosion filter:

| Method | 1.21.1 | 26.3 |
|---|---:|---:|
| vanilla `Aquifer.computeSubstance` | 15.0 % | 4.4 % |
| vanilla `Beardifier.compute` (structures) | 6.1 % | 1.6 % |
| ReTerraForged `Erosion.applyDrop` | 5.0 % | 6.0 % |
| ReTerraForged `Perlin.compute` | 1.7 % | 2.9 % |
| ReTerraForged `ITerrain$Delegate.erosionModifier` | 4.5 % in 0.0.12, gone in 0.0.13 | – |

</details>

### 0.0.32, five versions

On 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3, NeoForge and Fabric, on real dedicated servers:

- **Self test:** 42 checks pass on each version and loader (on 1.21.1 with the biome mods: 41).
- **Structures:** checked on each version and loader.
- **Two loaders:** the worlds of NeoForge and Fabric match in heights and biomes.
- **Fingerprint against 0.0.31:** a preset from 0.0.31 on this build gives the same surface and heights. All 4501 lines match on 1.21.1, 1.21.11 and 26.2. On 26.1.2 and 26.3, 1 and 4 whole chunks out of 96 differ: there the features cross the chunk border and depend on the order in which the neighbours are generated.
- **New reference:** the new fingerprint reference differs from the old one only in the rock strata; all 4225 height and biome lines match on each version.
- **Biomes O' Plenty with TerraBlender** (NeoForge): the self test and the biome kinds of the mods pass.
- **A tile that fails to generate:** the server stops by itself, in 47–48 s.
- **Client GUI test** in Russian on both loaders: 31 screenshots, including the five restored hints.
- **Unit tests:** 98 on every version except 26.3 (92).

Only on 1.21.1:

- **C2ME** on NeoForge and on Fabric (with ModernFix on Fabric): the self test passes.
- **Biomes O' Plenty with Regions Unexplored** on NeoForge: 89 of 121 biome kinds, as in 0.0.31.
- **Stress test:** 467 preset variants, no failures.
- **GUI test** in English.
- **A world made by 0.0.31** opened by this build gives the same terrain (CI).

Not checked: Biomes O' Plenty with Regions Unexplored on Fabric 1.21.1. Lithostitched 1.8.1 does not load with Fabric Loader 0.18.3, and Regions Unexplored 0.6.3 needs Fabric Loader 0.17 or newer.


### Results of 0.0.13

| Minecraft | Self test, NeoForge | Self test, Fabric | Structure test | Click test, NeoForge | Click test, Fabric |
|---|:-:|:-:|:-:|:-:|:-:|
| 1.21.1 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.3 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.4 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.5 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.8 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.10 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 1.21.11 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 26.1.2 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 26.2 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |
| 26.3 | ✅ 18/18 | ✅ 18/18 | ✅ 15 of 26 found | ✅ 12 screenshots | ✅ 12 screenshots |

Zero errors of the mod in every server and client log. The tests found two things on the way to this table: on 26.3 the left mouse button is number 1 instead of 0, so the click test clicked nothing until it was fixed, and on 1.21.4 Fabric the click test failed once and passed on every rerun.

## Source

The `main` branch is Minecraft 1.21.1, the other versions are `mc/<version>` branches, one port commit each on top of
the previous one.

## Credits and license

TerraForged by dags; ReTerraForged by racoonman2; this fork by Vladeezyy.

License: **LGPL-3.0-only** ([LICENSE](LICENSE), with the GPL-3.0 text in [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. Forks and modified versions must stay open under the same license and credit the authors. The code taken from the upstream projects keeps its MIT license and copyright notices ([LICENSE-MIT](licenses/LICENSE-MIT)). Please download the mod only from the official release pages.

</details>

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

<sub>Шейдеры и большая дальность прорисовки — только для красоты, мод от них не зависит.</sub>

Генерация мира в духе TerraForged: материки и океаны, горные хребты, речные сети с притоками, озёра, болота, эрозия
склонов, пласты горных пород — и редактор пресетов с живой картой будущего мира. Это рабочий форк
[ReTerraForged](https://github.com/racoonman2/ReTerraForged) от racoonman2, поверх TerraForged от dags. Генерация —
как в оригинальном **ReTerraForged 0.0.6** для 1.20.1, только без его ошибок и на новых версиях игры.

## Скачать

| Minecraft | Релиз | NeoForge | Fabric Loader / API | Java |
|---|---|---|---|---|
| 26.3 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.25](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.25-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.31](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.31-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

В каждом релизе — jar для NeoForge, jar для Fabric (нужен Fabric API) и датапак пресета для серверов.
Новые версии выходят для 1.21.1, 1.21.11, 26.1.2, 26.2 и 26.3; 1.21.3–1.21.10 остаются на 0.0.25.
Прошлые версии остаются на [странице релизов](https://github.com/Vladeezyy/reterraforged/releases), а их исходники —
в защищённых ветках `release/<версия>-<minecraft>` (например, `release/0.0.14-1.21.1`).

## Как пользоваться

**Одиночная игра.** При создании мира: вкладка «Мир» → «Настроить» → выберите пресет →
«Готово». Без этого шага мир будет ванильным. Предупреждение об экспериментальных настройках — нормально, нажмите
«Продолжить».

**Сервер.** Для нового мира положите датапак `reterraforged-preset-<версия>.zip` в
`world/datapacks/` до первого запуска. Свой пресет можно выгрузить кнопкой «Экспорт в датапак» на экране пресетов.

Мод влияет только на новые миры, созданные с его пресетом. Обычный мир он не меняет.

## Возможности

- **Рельеф TerraForged:** материки с береговыми линиями, отмелями и заливами, горные цепи, плато, холмы, долины,
  степи, бесплодные земли, вулканы.
- **Вода:** речные сети с главными реками и притоками, озёра на реках, болота, глубина и ширина русла настраиваются.
- **Поверхность:** симуляция эрозии каплями воды, камень и осыпи на крутых склонах, пласты горных пород, снег только
  там, где он лёг бы естественно.
- **Снеговая линия** (0.0.25, страница «Прочее», по умолчанию выключена): снег на земле и деревьях выше заданной
  высоты в любом биоме, так что у гор в тёплом климате появляются снежные вершины; стоячая вода выше неё замерзает.
  «Высота снеговой линии» и «Разброс снеговой линии» задают, где она проходит и насколько неровен её край. С 0.0.26
  снег может быть глубже: «Глубина снега у линии» и «Глубина снега на вершинах» (от 1 до 8 слоёв, 8 — полный блок) и
  «Нарастание снега» (за сколько блоков над линией глубина меняется от одного значения к другому).
- **Долины рек и озёр** (0.0.27, страница «Реки», [#29](https://github.com/Vladeezyy/reterraforged/issues/29)):
  «Ширина долин», «Крутизна склонов долин» (земля круче заданного угла у реки или озера срезается до него), «Долина по
  излучинам» (без сухого дна долины там, где река уходит в сторону) и «Доля ущелий». По умолчанию долины прежние; при
  высоком или растянутом рельефе настройки убирают отвесные стены, которые река прорезала в горах.
- **Уровень глубинного сланца** (0.0.27, страница «Мир», [#32](https://github.com/Vladeezyy/reterraforged/issues/32)):
  высота, на которой камень сменяется глубинным сланцем; 0 — как в ванилле.
- **Тёплые океаны у тропических лесов** (0.0.28, страница «Климат», [#35](https://github.com/Vladeezyy/reterraforged/issues/35)): море
  в регионах тропических лесов — тёплый океан с коралловыми рифами; по умолчанию выключено (умеренно тёплый, как
  в 0.0.6).
- **Исправления генерации** (0.0.30, страница «Мир», [#28](https://github.com/Vladeezyy/reterraforged/issues/28), [#38](https://github.com/Vladeezyy/reterraforged/issues/38)): исправления, которые меняют
  рельеф. В новых пресетах включены, в сохранённых раньше выключены, поэтому существующие миры не меняются. Острова разные у разных сидов, их пляжи
  над водой, горизонтальный масштаб меняет только их рельеф, а не очертания. Дно болот не повторяется от чанка к чанку. «Болота в низинах»
  подбирают и те болота, которые моды биомов вроде Terralith держат в другом климате, и доходят до смешанных областей рельефа, которые раньше пропускали, а это большинство областей. Теперь «Болота в низинах» доходят и до сухих и холодных низин, где Terralith держит Orchid Swamp и Ice Marsh. У типа материков «Бесконечный» появляется суша. Измерение
  другого мода на шуме верхнего мира получает рельеф вместо ровного моря. В новых мирах оно также включает версию генерации 1: глубокие структуры под реками и озёрами, пласты камня по сиду мира и полный 64-битный сид. Миры, созданные раньше, не меняются.
- **Болота в низинах и Самые высокие болота** (0.0.29, страница «Климат», [#28](https://github.com/Vladeezyy/reterraforged/issues/28)):
  на влажных ровных низинах могут появляться широкие болота (по умолчанию выключено), а болотный биом слишком высоко над морем
  выбирается заново (12 блоков в новых пресетах), поэтому болота Terralith не забираются на холмы.
- **Болотные низины** (0.0.30, страница «Климат», [#28](https://github.com/Vladeezyy/reterraforged/issues/28)): такая доля областей рельефа
  с влажным, не холодным климатом становится ровной сушей чуть выше моря, каждая одним большим болотом. С «Самыми высокими болотами» на 12 и с Biomes O'
  Plenty и Terralith 25 % дают болота на 5–7 % суши, из них широких 36–46 %, участками по 1–1,5 км². Настройка меняет рельеф, поэтому по умолчанию
  выключена, а в сохранённых раньше пресетах её нет. Генерация при 25 % медленнее на 4–5 %. Реки по-прежнему идут и через низины.
- **Настройки островов** (0.0.30, страница «Острова», [#38](https://github.com/Vladeezyy/reterraforged/issues/38)): «Октавы шума берегов» (от 1 до 8, по умолчанию 5), «Усиление шума берегов» (от 0 до 1, по умолчанию 0,5) и «Лакунарность шума берегов» (от 1 до 4, по умолчанию 2) делают берега островов неровными, а не всегда гладкими. «Смещение сида островов» (с кнопкой для случайного значения) переносит острова на другое расположение. «Расстояние от континентов» (от 0 до 1, по умолчанию 0) держит острова дальше от континентов: при 1 ближайший остров примерно в 190 блоках от континента, а медианный остров — в 860, против 30 и 610 при 0. «Горы континентов» (по умолчанию выключено) дают островам шум гор континентов; при максимальных ползунках гор островов самая высокая точка около 300 вместо 235.
- **Плотность растительности** (0.0.30, страница «Прочее», [#37](https://github.com/Vladeezyy/reterraforged/issues/37)): «Плотность травы» (от 0 до 3), «Плотность деревьев» (от 0 до 2) и «Плотность растений» (от 0 до 3; цветы, кусты, грибы и другие растения): 1 — обычное количество, 0 — ни одного. Нужны «Свои элементы биомов» (включены в «По умолчанию»). Они меняют и растительность биомов других модов, которых узнают по именам элементов (элемент с неизвестным именем остаётся как есть). При 1 ничего не стоят.
- **Саженцы выращивают деревья мода** (0.0.31, [#41](https://github.com/Vladeezyy/reterraforged/issues/41), при включённых «Своих элементах биомов», в «По умолчанию» они включены): саженец дуба, берёзы, ели, джунглей, акации или тёмного дуба, в том числе с костной мукой, вырастает одним из деревьев-шаблонов мода вместо ванильного. Ель, джунгли и тёмный дуб растут 2 × 2 из четырёх саженцев. Рядом с цветами с шансом 5 % появляется улей, как в ванили. Ваниль по-прежнему выбирает дерево и возвращает саженцы, если дерево не помещается: деревья мода выше (джунгли 16–19 блоков, секвойя около 20), поэтому саженцу нужно больше места, чем в ванили. Саженцы других модов, вишня, мангровое дерево и азалия не меняются. При выключенных «Своих элементах биомов» саженцы растят ванильные деревья.
- **Пласты горных пород** ([#1](https://github.com/Vladeezyy/reterraforged/issues/1)): слои гранита, диорита, андезита
  и камня под землёй. Число слоёв и их относительная толщина настраиваются ползунками на странице «Прочее». Камни
  других модов добавляются через тег блоков `reterraforged:rock`; jasper, limestone и shale из Quark уже в нём.
  «Только камень для руд» (включено по умолчанию, как в TerraForged) оставляет в пластах только камни, в которых
  появляются руды (тег `reterraforged:ore_compatible`): с 1.18 руды не появляются в камнях Quark, поэтому чтобы они
  были в пластах, выключите его, руд в их слоях не будет.
- **Деревья мода** из шаблонов — дубы, берёзы, ели, секвойи, ивы, акации, джунгли — с ульями, как у ванильных.
- **Редактор пресетов:** больше 160 настроек на одиннадцати страницах — мир, поверхность, пещеры, климат, рельеф, острова, реки,
  фильтры, структуры и прочее — с картой-превью, которая перерисовывается при каждом изменении. Пресеты сохраняются,
  копируются и выгружаются как датапак.
- **Структуры:** для каждого набора структур можно задать расстояние, разнос и salt или отключить его. Деревни и
  другие наземные структуры не ставятся на горы, как в 0.0.6.
- **Пресеты:** «По умолчанию» из ReTerraForged 0.0.6 и классические пресеты TerraForged — Beautiful, Huge Biomes,
  Lite, Vanilla-ish.
- **11 языков:** русский, украинский, английский, китайский, немецкий, французский, испанский,
  португальский (Бразилия), японский, корейский, польский.

![Экран пресетов на русском](docs/preset-screen-ru.jpg)

## Что исправлено в форке

**Генерация как в 0.0.6.** Порт на 1.21.1, с которого начинался этот форк, менял генерацию сверх нужного: «Default»,
климат гор (ледяные пики рядом с пустыней), реки, озёра, болота, структуры на горах, склоны пустынь, кусты, глубину
пещерных биомов. Каждый класс мода сверен с исходником 0.0.6, и всё это возвращено. Вариант порта удалён (0.0.17):
переключателя «Оригинальная генерация (0.0.6)» больше нет, все миры генерируются как в 0.0.6. Новые чанки миров,
созданных на версиях форка до 0.0.11, тоже идут по 0.0.6, поэтому на границе возможен шов.

**Ошибки, из-за которых мир не работал:**
- новый мир не создавался — генерация падала из-за ошибки в миксине;
- на NeoForge деревья и другие элементы пресета не появлялись: модификаторы биомов лежали в несуществующем реестре;
- на 26.x деревья мода не росли: с 26.1 трава вышла из тега `#dirt`, а с 26.3 шаблоны деревьев читались как воздух;
- сервер на Fabric зависал при генерации Незера и Энда, одиночная игра на Fabric — на новых чанках;
- `/locate`, карты картографа и дельфины надолго вешали сервер;
- утечка памяти: кэши генератора не освобождались до закрытия игры;
- крайние значения настроек давали NaN в рельефе.

**Моды на биомы (Biomes O' Plenty и другие через TerraBlender), 0.0.15:**
- треть видов биомов Biomes O' Plenty не генерировалась (woodland, orchard, grassland, pasture, tundra и другие);
  теперь 52 из 61, в ванильном мире 55;
- на Fabric 1.21.10 и 1.21.11 регионы модов на биомы не следовали карте ReTerraForged;
- на 26.3 в чанках оказывались не те биомы, что ждали `/locate`, структуры и спаун, и поверхность модов на биомы
  (песок, грязь, вулканическая порода) будто пропадала.

**Швы на склонах, 0.0.17** ([#2](https://github.com/Vladeezyy/reterraforged/issues/2)): с модами, которые
перекрашивают поверхность гор, например William Wythers' Overhauled Overworld, камень крутых склонов выходил
квадратами и полосами по чанкам.

**Прочие исправления, 0.0.32**:
- **Болотные низины в климате другого региона** ([#42](https://github.com/Vladeezyy/reterraforged/issues/42), возможная причина): участок мира мог получить болотную низину и климат другого региона, например целые чанки болота посреди пустыни. Идентификаторы регионов повторяются, а кэш каждого потока генерации искал регион только по идентификатору. Зависело от порядка работы потоков и пропадало при пересоздании мира. Воспроизведением не подтверждено, поэтому причина не доказана.
- **Скорость генерации при том же мире:** кэш на каждый поток (16 столбцов болот и 64 ячейки рельефа вместо одной записи), округлённая высота столбца считается раз на столбец чанка, тайлы создаются вне общей блокировки, шаг фич не ждёт тайл, шаблоны деревьев читаются без общей блокировки на каждое дерево, а дерево проверяет структуры только своего чанка. Мир остаётся блок в блок тем же: отпечатки чанков пресета Default и пресета с «Болотными низинами» и «Болотами в низинах» дают те же 4501 строку, что и в 0.0.31. Замеры в разделе «Проверка»: на несколько процентов быстрее, заметного игроку ускорения нет.
- **Выход из мира** отменяет его незаконченные тайлы, поэтому они не задерживают первый тайл следующего мира.
- **Деревья из саженцев не заменяют блоки под корнями** (стекло, воду, цветы, саженцы, рельсы). Касалось шаблонов деревьев из датапаков, у которых на нижнем слое есть блоки в стороне от ствола; встроенные деревья мода не затронуты.
- **Шум в датапаках:** число октав и ступеней террасы ограничено (огромное или отрицательное значение вешало или роняло генерацию); селектор пластов меньше 0 больше не роняет игру; тип `simplex_ridge` можно записать.
- **Другие моды:** инъекции, которые конфликтовали бы с другим модом в том же месте, заменены на совместимые (wrap вместо redirect, без захвата локальных переменных), поля миксинов уникальны, а поиск точки спавна подменяется только в мирах с пресетом (в остальных ванильный). TerraBlender объявлен необязательной зависимостью. Диагностические счётчики TerraBlender, которые при установленных Darker Depths или Deeper Darker работали на каждом выборе биома и писали строки в лог, теперь включаются только с `-Drtf.tbdiag=true`.
- **«Снеговая линия по высоте мира»** действует только в измерении, которое строит пресет: раньше масштаб был один на всю игру, и снег и лёд сдвигались и в измерениях других модов.
- **Размер тайла хранится в мире** (`reterraforged/generation.json` в папке мира): раньше смена `tileSize` в `performance.conf` или другом конфиге на сервере давала швы эрозии между старыми и новыми чанками. Теперь конфиг действует на новые миры.
- **Экран пресета:** экспорт датапака идёт не в потоке рендера, поэтому «Создать мир», «Готово» и «Экспорт в датапак» больше не замораживают игру (на время экспорта показывается ванильный экран подготовки). Устаревший расчёт превью останавливается. Пять подсказок снова описывают свои настройки («Эрозия обычным камнем», две дистанции озёр, «Горные биомы» и «Вулканические биомы»): их тексты из 0.0.21 были потеряны во всех 11 языках.
- **Версия генерации 1, только для новых миров** (миры, созданные раньше, сохраняют каждый блок; пресет из библиотеки игрока получает её при создании нового мира; включается вместе с «Исправлениями генерации»): под реками и озёрами строятся глубокие структуры (крепости, древние города, испытательные камеры: раньше правило структур отказывало им на любой глубине, поэтому крепостей было меньше, а око Эндера пропускало их точку); пласты горных пород зависят от сида мира (раньше были одинаковыми во всех мирах); 64-битный сид даёт свой рельеф (раньше учитывались только младшие 32 бита; сиды, которые помещаются в 32 бита, включая все текстовые, дают тот же рельеф, что и раньше).
- **Сборка:** версии Loom и плагина Architectury закреплены (не снапшоты); токен CI только читает.

**Прочие исправления, 0.0.31**:
- **Эрозия не трогает поверхностные блоки других модов** ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)): тег размываемых блоков перечисляет земляные блоки ванили по именам, а не весь тег земли. Chalk Cliffs из Regions Unexplored держали 29,1 % мелового травяного блока (было 11,2 %, при 18,3 % породы пластов). Датапак может добавить блок мода в тег. Существующий мир сохраняет тег из датапака своего пресета, поэтому исправление действует в новых мирах.
- **Горизонтальный масштаб островов** ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)) больше не работает наоборот. **Крутые берега** получают климат каменистого берега, поэтому песок пляжа не выходит на склоны гор ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)), а Chalk Cliffs из Regions Unexplored генерируются ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)).
- **Регионы TerraBlender у других модов биомов:** регион биома теперь хэш его id по весам регионов. Regions Unexplored с Biomes O' Plenty: 89 из 121 вида биомов (было 76); на 26.x один Biomes O' Plenty: 51 из 59 (было 50). Миры без TerraBlender не меняются.
- **Экспорт пресета на запущенном сервере** с Regions Unexplored или Biomes O' Plenty падал с ошибкой `reterraforged:strata/depth is not valid in current registry set`. Теперь работает.
- **Flower Forest** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37)) имеет столько же цветов, сколько ваниль: 2,125 участка на чанк вместо 1,5. На 26.2 по трём сидам по 32 × 32 чанка вышло 60,3 цветка на 256 колонок, в ванили 60,0. «Плотность растений» их масштабирует.
- **Fabric: модификаторы биомов одного мира больше не переходят в следующий мир** той же игровой сессии. Модификаторы пресета добавлялись в общий список Fabric API, который живёт всю игру: после выхода из мира ReTerraForged следующий открытый ванильный мир получал деревья пресета, а другой мир ReTerraForged получал траву, эрозию и плотность растительности дважды. Теперь они применяются из копии списка.
- **Нет краша в других измерениях с деревьями пресета:** измерение мода или датапака с берёзовым лесом, рощей, ветреными холмами или старой тайгой, но без рельефа ReTerraForged, останавливало сервер с ошибкой «Feature placement» (UnsupportedOperationException). Теперь там деревья сохраняют свой обычный шанс.
- **Дерево из саженца не сносит соседние саженцы**, как у ванили (#41 в 0.0.31): дерево, выросшее из саженца, могло заменить соседние саженцы и цветы своим стволом или листвой, и ферма деревьев теряла саженцы. Теперь оно заменяет только то, что заменяют деревья ванили: на 1.21.1 цветы остаются; с 1.21.11 деревья ванили берут мелкие цветы, и деревья мода тоже. Генерация мира не меняется.
- **Экспорт пресета в папку, название которой заканчивается на «!»** (экземпляр «Pack!», пользователь Windows «Name!»): zip записывался по неверному пути, экспорт падал, а при такой временной папке новые миры создавались без пресета. Ошибка очистки после успешного экспорта (Windows, антивирус держит файл) больше не скрывает экспорт.
- **Карты рек:** карта рек удалялась через 5 минут после создания, даже пока использовалась, и создавалась заново под блокировкой, которую ждали все потоки генерации (при бесконечном континенте одна карта покрывает весь мир). Теперь она живёт, пока используется, и создаётся, не останавливая потоки, которым нужны другие карты.
- **Чужой генератор с рельефом мода** (случайное состояние, для которого ChunkMap не настроил контекст): чанки, сгенерированные во время сборки контекста, могли получить высоту 0 (плоское море). Потоки теперь ждут контекст.

**Плотность деревьев и травы, 0.0.30** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37), при включённых «Своих элементах биомов»):
в лесах, тайге и джунглях было вдвое меньше деревьев, чем в ванили: дерево не вырастало в колонке с травой и под листвой. Теперь
их примерно столько же, сколько в ванили, стоят ровно и без больших пустот. В рощах теперь растут ели, в саваннах и бамбуковых
джунглях деревьев примерно как в ванили, а берёзовые леса больше не заросли высокой травой. Новые ползунки меняют
количество травы, деревьев и прочих растений во всех биомах Верхнего мира, в том числе из других модов (их узнают по именам
элементов): 1 — обычное количество, 0 — нет совсем. Деревьев на 256 колонок земли (16 × 16 блоков), 0.0.29 → 0.0.30,
в ванили в скобках: тайга 2,95 → 5,58 (6,24), лес 2,49 → 5,62 (5,84), джунгли 2,33 → 4,83 (5,14), роща 0,11 → 5,46 (5,55),
саванна 0,08 → 1,10 (0,94). Доля пустых квадратов 16 × 16 в лесах упала с 3–5 % до 0.

**Прочие исправления, 0.0.30**:
- **1.21.11 с Terralith:** рельеф выходил ступенями по 8 блоков. Мод создавал новый сэмплер плотности на каждом проходе по настройкам шума, поэтому кэшируемая функция плотности Terralith делилась на несколько кэшей, и каждый хранил по одному значению на ячейку. Исправлено на всех версиях; только 1.21.11 это показывал (новый lithostitched скрывает). Новые чанки уже созданного мира 1.21.11 с Terralith на границе не совпадут со старыми.
- **26.2 с TerraBlender:** пресет не собирался, поэтому мир создать было нельзя; TerraBlender 26.2.0.0.2 оборачивает правила поверхности в собственное правило, мод теперь читает правило внутри.
- **26.1.2, 26.2, 26.3:** после неудачного тайла рельефа сервер никогда не выключался. Теперь он останавливается сам примерно за минуту, как на 1.21.x.
- **Болота в низинах** ([#28](https://github.com/Vladeezyy/reterraforged/issues/28)): теперь доходят и до сухих и холодных низин. С Biomes O' Plenty и Terralith, «Болота в низинах» 12 и «Самые высокие болота» 12 (блоки), Orchid Swamp у Terralith выросло с 0,03 до 0,69 % суши, Ice Marsh — с 0,02 до 0,72 %; болота Biomes O' Plenty остались прежними; все болота вместе 9,9 % суши вместо 7,1 %.

**Леса, 0.0.29** ([#37](https://github.com/Vladeezyy/reterraforged/issues/37), при включённых «Своих элементах биомов»): у края
климатического региона ошибка в формуле оставляла в лесах голые полосы; из лесов и тёмных лесов пропадала опавшая листва
(1.21.11 и новее). Ещё в 0.0.29, всё из 0.0.6: большие ели в пихтовых и секвойных лесах никогда
не ставились, пещеры карверов не слушались ползунка уровня лавы, континенты EXPERIMENTAL не запускались.

**Деревья и болота, 0.0.28** ([#34](https://github.com/Vladeezyy/reterraforged/issues/34)): деревья
болот, равнин, саванн, лугов, ветреных холмов и заснеженных равнин стояли в углу своего чанка, сеткой через 16 блоков;
у рек оставались ванильные дубы у воды; хижины ведьм (и болотные постройки других модов) не генерировались: для
построек заболоченные земли считались рекой.

**Экран пресетов:**
- на 1.21.11+ ползунки ничего не меняли;
- выбранный пресет не подсвечивался, правки и повторно применённый пресет терялись;
- «назад» перескакивал через страницу;
- на 1.21.6+ экран ронял игру;
- «Экспорт в датапак» уже экспортированного пресета оставлял в архиве то, что новые настройки выключили, например
  эрозию ([#5](https://github.com/Vladeezyy/reterraforged/issues/5)).

Ещё заработали страница структур и ползунки, которые раньше ничего не делали (горизонтальный масштаб холмов, долин,
плато, бесплодных земель, масштабы вулканов). Настройки, которые генерация не использует (и в 0.0.6 тоже), показаны
неактивными. Пресеты из файлов приводятся к допустимым значениям, сломанный файл в папке пресетов больше не роняет игру.

## Известные ограничения

- **Далеко от центра** (за 16,7 млн блоков, 2^24) координаты рельефа теряют точность, и земля идёт ступенями по 2 блока. Сбоев и дыр нет. Исправление
  изменило бы шум всех миров. Координаты считаются во float, как в оригинальном ReTerraForged, а искажения шума грубеют уже около 8 млн блоков:
  держите границу мира не дальше 8 млн блоков (`/worldborder`).
- **Сиды:** в мирах, созданных без версии генерации 1, рельеф использует младшие 32 бита сида, поэтому у сидов `x` и `x + 2^32` там почти
  одинаковый рельеф (99 % высот). Версия генерации 1 (новые миры с «Исправлениями генерации») использует весь 64-битный сид. Переход старых
  миров на старшие биты изменил бы рельеф всех миров с 64-битным сидом.
- **`tileSize` в `performance.conf`** меняет и часть блоков поверхности (различия есть в 7–11 % поверхностных чанков), потому что эрозия считается по
  тайлам. Мир хранит размер тайла, с которым создан (`reterraforged/generation.json`), а конфиг действует на новые миры. Держите одно значение
  на всех машинах модпака. Больше 5 обрезается до 5: для 8 требовалось около 9 ГБ.
- **«Болота в низинах» с модами биомов:** болота идут туда, где рельеф низкий, в основном в долины рек, потому что настройка не меняет форму суши. С
  Biomes O' Plenty и Terralith при 12 / 12 они занимают около 10 % суши, и широких среди них только около 3 %. Широкие болота даёт настройка «Болотные низины» (см.
  [#28](https://github.com/Vladeezyy/reterraforged/issues/28)).
- **Шахты под руслами рек у уровня моря** по-прежнему не строятся: точка старта выше порога глубины.
- **Поверхность у пляжей и на склонах** местами берёт биом без учёта эрозии тайла, как в оригинале. Не исправлено.

## Проблемы и исправления подробно

<details>
<summary><b>Болотные низины: климат другого региона</b> (0.0.32, возможная причина <a href="https://github.com/Vladeezyy/reterraforged/issues/42">#42</a>)</summary>

**Проблема.** Участок мира мог получить болотную низину и климат другого региона: например, целые чанки болота посреди пустыни. Зависело от порядка работы потоков и пропадало при пересоздании мира.

**Причина.** Идентификаторы регионов повторяются от региона к региону, а кэш каждого потока генерации искал регион только по идентификатору.

**Исправление.** Исправлено в 0.0.32. Воспроизведением причина не подтверждена, поэтому это возможная причина #42, а не доказанная.

</details>

<details>
<summary><b>Крепости под реками и озёрами</b> (0.0.32, версия генерации 1)</summary>

**Проблема.** Под реками и озёрами не строились крепости, древние города и испытательные камеры на любой глубине. Крепостей было меньше, а око Эндера пропускало их точку.

**Причина.** Правило структур отказывало глубоким структурам на любой глубине.

**Исправление.** Версия генерации 1 строит их под реками и озёрами. Действует только в новых мирах: мир, созданный раньше, сохраняет каждый блок. Её пласты горных пород тоже зависят от сида (см. ниже).

</details>

<details>
<summary><b>Пласты пород и 64-битные сиды</b> (0.0.32, версия генерации 1)</summary>

**Проблема.** Пласты горных пород были одинаковыми во всех мирах. Рельеф 64-битного сида использовал только младшие 32 бита, поэтому у сидов `x` и `x + 2^32` рельеф был почти одинаковым.

**Причина.** Пласты не зависели от сида, а рельеф читал только младшие 32 бита сида.

**Исправление.** В версии генерации 1 (новые миры) пласты следуют за сидом, а 64-битный сид даёт свой рельеф. Сиды, которые помещаются в 32 бита, включая все текстовые, дают тот же рельеф, что и раньше.

</details>

<details>
<summary><b>tileSize: швы после смены конфига</b> (0.0.32)</summary>

**Проблема.** Смена `tileSize` в `performance.conf` (или другого конфига на сервере) давала швы эрозии между чанками, созданными до и после смены.

**Причина.** Мир не хранил размер тайла, с которым был создан.

**Исправление.** Размер тайла хранится в `reterraforged/generation.json` в папке мира. Конфиг действует на новые миры.

</details>

<details>
<summary><b>Шум в датапаке: огромные значения вешали генерацию</b> (0.0.32)</summary>

**Проблема.** Датапак с огромным или отрицательным числом октав или ступеней террасы вешал или ронял генерацию. Селектор пластов меньше 0 ронял игру.

**Причина.** Значения не ограничивались.

**Исправление.** Число октав и ступеней террасы ограничено. Селектор пластов меньше 0 больше не роняет игру, а тип `simplex_ridge` можно записать.

</details>

<details>
<summary><b>Деревья из саженцев заменяли блоки под корнями</b> (0.0.32)</summary>

**Проблема.** Дерево, выросшее из саженца, заменяло блоки под корнями (стекло, воду, цветы, саженцы, рельсы). Так бывало с шаблонами деревьев из датапаков, у которых на нижнем слое есть блоки в стороне от ствола.

**Причина.** На нижнем слое таких шаблонов есть блоки рядом со стволом, и дерево их заменяло.

**Исправление.** Дерево больше не заменяет блоки под корнями. Встроенные деревья мода не затрагивались.

</details>

<details>
<summary><b>Миксины других модов</b> (0.0.32)</summary>

**Проблема.** Некоторые инъекции конфликтовали бы с инъекциями другого мода в том же месте.

**Причина.** Redirect и захват локальных переменных.

**Исправление.** Вместо redirect используется wrap, локальные переменные не захватываются, поля миксинов уникальны.

</details>

<details>
<summary><b>«Снеговая линия по высоте мира» в других измерениях</b> (0.0.32)</summary>

**Проблема.** Снеговая линия сдвигалась и в измерениях других модов: там сдвигались снег и лёд.

**Причина.** Настройка имела один масштаб на всю игру.

**Исправление.** Масштаб действует только в измерении, которое строит пресет.

</details>

<details>
<summary><b>Экран пресета: игра замирала при создании мира и экспорте</b> (0.0.32)</summary>

**Проблема.** При открытии «Создать мир», «Готово» или «Экспорт в датапак» игра замирала.

**Причина.** Датапак экспортировался в потоке рендера.

**Исправление.** Экспорт идёт вне потока рендера, на время экспорта показывается ванильный экран подготовки. Устаревший расчёт превью останавливается.

</details>

<details>
<summary><b>Различия между версиями</b> (0.0.32)</summary>

- **1.21.5 и новее:** дерево, выросшее из саженца, забирает мелкий цветок под корнями, как ванильные деревья: тег `#replaceable_by_trees` там включает `#small_flowers`.
- **26.3:** кэш высоты столбца не нужен и не перенесён: движок сам считает такие функции раз на столбец.

</details>

<details>
<summary><b>Швы по границам чанков на склонах</b> (0.0.17, <a href="https://github.com/Vladeezyy/reterraforged/issues/2">#2</a>)</summary>

**Проблема.** С William Wythers' Overhauled Overworld на вулканических Stony Peaks посреди блэкстоуна, туфа и
базальта оставались пятна диорита и камня, обрезанные по границам чанков; с другими модами, которые перекрашивают
склоны, то же самое. С увеличенным вертикальным или горизонтальным масштабом заметнее.

**Причина.** Эрозия (камень, гравий и осыпи на крутых склонах) шла первой фичей каждого чанка и переписывала до 32
блоков его колонок. Фичи соседних чанков заходят в чанк на несколько блоков, и фичи соседей, сгенерированных раньше,
там уже были: эрозия их затирала, а фичи соседей, сгенерированных позже, оставались. Уцелеет ли пятно, зависело от
порядка генерации чанков. В 0.0.6 та же ошибка.

**Как исправлено.** Эрозия идёт при постройке поверхности чанка, до любых фич его и соседей. Рельеф не меняется
(высоты те же везде); в мире без таких модов отличается около 0,1–0,2 % блоков поверхности, все у границ чанков, где
результат раньше зависел от порядка генерации. Старые миры сохраняют свои чанки; чанки, поверхность которых уже
построила старая версия, эродируются по-старому, поэтому шва на краю исследованной области нет. На 12 × 12 чанках
Stony Peaks с модом блок поверхности на границе чанков менялся в 1,41 раза чаще, чем внутри чанка, теперь в 0,82; с
вертикальным масштабом 1,5 и масштабом гор 2 (как в отчёте) было 1,93, стало 0,82.

</details>

<details>
<summary><b>Моды на биомы через TerraBlender</b> (0.0.15)</summary>

ReTerraForged выбирает ванильные биомы по своим типам биомов и отдаёт таблицам биомов только некоторые пары
температуры и влажности: на суше нет ни одной «тепло + средняя влажность». Ванильным биомам это подходит, а Biomes
O' Plenty ставит туда woodland, old growth woodland, grassland, orchard и другие: генерировалось 39 видов из 61 (в
ванильном мире с тем же сидом 55). Теперь для регионов других модов температура и влажность сдвигаются двумя
плавными шумами примерно на одну полосу; ванильный регион и миры без TerraBlender не меняются. Стало 52 из 61.

На Fabric 1.21.10 и 1.21.11 хук, который передаёт TerraBlender регионы ReTerraForged, не применялся (его цель
осталась в jar в мохжанговых именах), и TerraBlender выбирал регионы по своему шуму. На 26.3 игра заполняет биомы
чанка по объёмам климата, мимо этого хука, поэтому с Biomes O' Plenty чанки и `/locate` расходились в 130 местах из
289. Оба исправлены: совпадают 288 из 289.

Проверено с Biomes O' Plenty на 1.21.1, 1.21.11 и 26.3, NeoForge и Fabric: `MODS=biomes-o-plenty
tools/servertest.sh` сравнивает мир ReTerraForged с ванильным миром с тем же сидом и модами (виды биомов, их доля
суши, сколько их под водой, свои верхние блоки).

</details>

<details>
<summary><b>Ульи на деревьях мода</b> (0.0.12)</summary>

**Что было.** На деревьях ReTerraForged никогда не было ульев — ни в порте, ни в оригинальном 0.0.6. В равнинах,
цветочных лесах и на лугах, где у ванильных деревьев ульи есть, пчёл не было вообще.

**Почему.** Деревья мода — это шаблоны, а не ванильные деревья. Ванильный декоратор ульев ставит улей только под самыми
нижними листьями, а у шаблонов там вокруг ствола сплошные листья — места не находилось. Вдобавок декораторы для
отдельных биомов искались по имени реестра вместо имени биома.

**Как исправлено.** Свой декоратор ульев для шаблонов: улей ставится у ствола, лицом на юг, там, где есть место, с 2–3
пчёлами. Шансы как в ванилле: равнины 5 %, цветочные леса 2 %, леса и березняки 0,2 %, деревья на лугах — всегда.
Саженцы рядом с цветами (в том числе выращенные костной мукой) дают ульи как обычно. Самотест проверяет и то, и другое.

</details>

<details>
<summary><b>26.x: деревья мода не росли</b> (0.0.12)</summary>

**Что было.** На 26.1.2, 26.2 и 26.3 деревья ReTerraForged почти не появлялись, леса стояли пустыми. На 26.x
эрозия не трогала травяные склоны.

**Почему.** Две разные причины. С 26.1 Minecraft вынес траву, подзол и мицелий из тега `#dirt` в отдельный
`#grass_blocks` — мод проверял почву по `#dirt` и не ставил деревья на траву, а эрозия не считала траву размываемой.
С 26.3 Minecraft читает блоки из NBT по ключам `id` и `properties` вместо `Name` и `Properties`: каждый блок шаблонов
деревьев превращался в воздух, и вставка дерева ничего не ставила.

**Как исправлено.** Почва на 26.x проверяется по `#substrate_overworld` — он совпадает со старым `#dirt`. Палитра
шаблонов переводится в новый формат при чтении. Самотест теперь проверяет, что в лесу реально стоят стволы, а не только
что в биоме записаны деревья мода: прежняя проверка проходила, хотя деревьев не было.

</details>

<details>
<summary><b>Генерация отличалась от оригинального 0.0.6</b> (0.0.11–0.0.12)</summary>

**Что было.** Миры не были похожи на ReTerraForged: ледяные пики рядом с пустыней, тёплый океан вплотную к пустыне,
другие реки и озёра, деревни на горах.

**Почему.** Порт на 1.21.1, с которого начинался форк, менял генерацию сверх нужного для переноса:
- «Default» был заменён недоделанным вторым пресетом;
- горный регион брал один биом из климата центра региона;
- у равнин и холмов был другой weirdness (от него зависят варианты биомов);
- реки, озёра и болота затухали на высоте и в горах;
- структуры ставились на горы;
- склоны пустынь превращались в песчаник и терракоту;
- в некоторые биомы добавлялись кусты;
- пещерные биомы были выше;
- был другой генератор случайности поверхности.

Сверка каждого класса с исходником 0.0.6 нашла ещё два отличия: крутизна склонов не ограничивалась единицей (камень и
снег ложились иначе), а пропущенные материки в открытом океане получали чужую карту рек.

**Как исправлено.** Всё возвращено как в 0.0.6. С 0.0.17 вариант порта удалён совсем (переключатель «Оригинальная
генерация (0.0.6)» и его пресет): все миры генерируются как в 0.0.6.

</details>

<details>
<summary><b>NeoForge: деревья пресета не появлялись</b> (0.0.10)</summary>

**Что было.** На NeoForge в мире с пресетом росли ванильные деревья, а не деревья мода.

**Почему.** Модификаторы биомов (замена деревьев, трава в лесах, отключение ванильных источников) лежали в реестре
`forge:biome_modifier`. Такой реестр был в Forge, в NeoForge его нет, и модификаторы молча не применялись.

**Как исправлено.** На NeoForge модификаторы лежат в его собственном реестре `neoforge:biome_modifier`.

</details>

<details>
<summary><b>Экран пресетов: ползунки, подсветка, правки</b> (0.0.10)</summary>

**Что было.**
- На 1.21.11 и новее ни один ползунок ничего не менял.
- Выбранный пресет никак не подсвечивался.
- Повторно применённый пресет и правки встроенного пресета терялись.
- «<<» со страницы рек перескакивал через страницу, а со страницы мира уводил в начало списка.

**Почему.**
- В 1.21.11 метод ванильного ползунка `setValue` стал protected, и одноимённый метод мода его подменил — значения
  перестали доходить до настроек.
- Список пресетов не рисовал выделение и сбрасывал его при клике по любой кнопке.
- Экран создания мира не применял датапак с тем же именем второй раз.

**Как исправлено.**
- Ползунки работают на всех версиях.
- Выбранный пресет подсвечен жёлтым «»».
- Каждое применение пресета получает своё имя датапака, правки встроенных пресетов сохраняются.
- «Назад» ведёт ровно на страницу назад и возвращает к выбранному пресету.

</details>

<details>
<summary><b>Ползунки и страницы, которые ничего не делали</b> (0.0.11)</summary>

**Что было.** Страница структур (расстояние, разнос, salt, отключение) не действовала — ни в порте, ни в 0.0.6.
Горизонтальный масштаб холмов, долин, плато, бесплодных земель и торридона, базовый и вертикальный масштаб вулканов не
меняли ничего.

**Почему.** Настройки структур не попадали в датапак мира. У этих рельефов размеры шумов были зашиты в код.

**Как исправлено.** Изменённые наборы структур записываются в датапак пресета. Масштабы рельефов и вулканов работают, а
при значении 1 рельеф прежний. Настройки, которые генерация не использует (и в 0.0.6 тоже), показаны неактивными, с
подсказкой.

</details>

<details>
<summary><b>/locate, карты картографа и дельфины вешали сервер</b> (0.0.11)</summary>

**Что было.** Поиск хижины ведьмы командой `/locate` шёл больше 10 минут. Карты картографа и дельфины подвешивали сервер.

**Почему.** Для каждой проверяемой точки (а их тысячи) порт строил целый тайл рельефа с эрозией, хотя нужна была одна
высота.

**Как исправлено.** Высота точки читается напрямую из ячейки, как в 0.0.6. Поиск всех ванильных структур вокруг спавна
теперь занимает около 40 секунд.

</details>

<details>
<summary><b>Новый мир не создавался</b></summary>

**Что было.** Игра падала сразу при создании мира с пресетом.

**Почему.** В последнем коммите апстрима в миксин `MixinNoiseChunk` добавили не-`private` статический метод, а Mixin
такое запрещает.

**Как исправлено.** Метод вынесен в отдельный класс `C2MECompat`.

</details>

<details>
<summary><b>Fabric: зависания в Незере, Энде и на новых чанках</b></summary>

**Что было.** Сервер на Fabric зависал при генерации Незера и Энда. Одиночная игра на Fabric зависала на новых чанках с
ошибкой `Index -1 out of bounds`.

**Почему.** Фичи эрозии и снега мода попадали в биомы других измерений и бросали исключение — чанк так и не
догенерировался. Отдельно Fabric не пересчитывал у генератора список фич после того, как мод менял биомы.

**Как исправлено.** Вне генератора ReTerraForged эти фичи ничего не делают. Список фич пересчитывается после изменения
биомов.

</details>

<details>
<summary><b>Экран пресетов ронял игру на 1.21.6+</b></summary>

**Что было.** На 1.21.6 и новее экран пресетов падал с `Can only blur once per frame` и терял все надписи. В маленьком
окне кнопки обрезались.

**Почему.** Экран рисовал фон ещё раз поверх того, что уже нарисовал Minecraft, и размытие срабатывало дважды за кадр —
с 1.21.6 это запрещено. Правая колонка в узком окне сжималась так, что кнопки не помещались.

**Как исправлено.** Фон рисуется один раз, правая колонка не уже 150 пикселей.

</details>

<details>
<summary><b>/reload не перезагружал шаблоны деревьев</b></summary>

**Что было.** После `/reload` или смены датапаков шаблоны деревьев оставались старыми — везде, кроме NeoForge 1.21.1.

**Почему.** Перезагрузка держалась на внутреннем имени скомпилированной лямбды Minecraft, которое меняется от версии к
версии.

**Как исправлено.** Шаблоны перечитываются, когда меняется менеджер ресурсов сервера. Миксины, завязанные на полные
сигнатуры конструкторов, заменены устойчивыми.

</details>

<details>
<summary><b>Мод грузился на любой версии Minecraft</b></summary>

**Что было.** Jar для одной версии загружался на другой и падал там с непонятной ошибкой. В `neoforge.mods.toml` версия
мода была 0.0.6 вместо настоящей.

**Как исправлено.** В метаданных точные диапазоны версий Minecraft и загрузчика. Неподходящая версия сразу отклоняется
с понятным сообщением.

</details>

<details>
<summary><b>Сломанные файлы пресетов и крайние значения</b> (0.0.9–0.0.10)</summary>

**Что было.** Чужой или повреждённый файл в папке пресетов ронял игру при открытии экрана. Значения, которые не даёт
ни один ползунок (например, отредактированные вручную), ломали мир.

**Как исправлено.** Сломанный файл пропускается с сообщением в логе, остальные пресеты загружаются. Значения из файлов
приводятся к диапазонам ползунков, а встроенные пресеты при этом не меняются.

</details>

<details>
<summary><b>NaN в рельефе</b> (0.0.9)</summary>

**Что было.** При некоторых настройках рельеф ломался: дыры до бедрока или плоский мир на уровне моря.

**Почему.** В расчётах появлялось деление на ноль, а дальше по всем расчётам расползался NaN. Так было при нулевом
горизонтальном масштабе, совпадающих точках перехода «океан — суша», реках нулевой длины на крошечных материках и
минимальном глобальном масштабе у границы мира.

**Как исправлено.** Все эти места защищены, а последний рубеж заменяет нечисловую высоту уровнем моря. Стресс-тест
прогоняет каждую настройку на крайних значениях и сотни случайных комбинаций.

</details>

<details>
<summary><b>Утечка памяти</b> (0.0.9)</summary>

**Что было.** Память игры росла с каждым перестроением превью и каждым входом в мир. С тяжёлыми настройками игра
вылетала по нехватке памяти.

**Почему.** Кэши генератора (тайлы рельефа, карты рек) регистрировались в глобальном списке и в планировщике очистки и
не освобождались никогда — до закрытия игры.

**Как исправлено.** Планировщик держит кэши по слабой ссылке. Кэш, который больше никому не нужен (старое превью,
покинутый мир), собирается сборщиком мусора, и его очистка останавливается.

</details>

<details>
<summary><b>ModernFix: вылет при создании мира</b> (0.0.23)</summary>

**Проблема.** С ModernFix (или ModernFix-mVUS) игра вылетала сразу после «Создать мир»: «Exception initializing
level» ([#19](https://github.com/Vladeezyy/reterraforged/issues/19)).

**Почему.** Access widener мода делал `NoiseChunk.wrap` публичным. ModernFix подменяет этот метод и оставляет его
protected; Mixin не разрешает сужать доступ, и Minecraft вообще не мог загрузить `NoiseChunk`.

**Как исправлено.** Access widener больше не трогает этот метод: мод вызывает его только из своего миксина, которому
расширенный доступ не нужен. Проверено на 26.1.2 Fabric с C2ME и ModernFix-mVUS.

</details>

<details>
<summary><b>Модуль OpenCL у C2ME: вылет при создании мира</b> (0.0.24)</summary>

**Проблема.** С модулем OpenCL у C2ME (C2ME OpenCL Acceleration) игра останавливалась сразу после «Создать мир»:
«OpenCL codegen failed» ([#19](https://github.com/Vladeezyy/reterraforged/issues/19)).

**Почему.** Модуль переводит рельеф каждого мира на видеокарту и знает только ванильные функции плотности. На
функциях ReTerraForged он отказывается и, если в `config/c2me.toml` не включён `allowIncompatibilityFallback` (по
умолчанию выключен), останавливает сервер.

**Как исправлено.** Для мира с рельефом ReTerraForged мод заставляет модуль откатиться, как будто эта настройка
включена: обычный мир генерируется на процессоре (C2ME всё равно распараллеливает генерацию), Незер и Энд остаются на
видеокарте. Настраивать ничего не нужно. Проверено на серверах 1.21.1, 1.21.11, 26.1.2 и 26.2 (NeoForge и Fabric, где
модуль есть). Сборки модуля для 26.3 пока нет.

</details>

<details>
<summary><b>Ecliptic Seasons: нет дождя</b> (0.0.25)</summary>

**Проблема.** С Ecliptic Seasons в мирах ReTerraForged не было ни дождя, ни его звука, даже после `/weather rain`
([#22](https://github.com/Vladeezyy/reterraforged/issues/22)).

**Почему.** Пресеты ReTerraForged используют старый источник случайности (поверхность 0.0.6). С ним ваниль при
построении поверхности берёт биом столбца на y 0, а Ecliptic Seasons запоминает его как биом поверхности. Под
большей частью суши ReTerraForged на y 0 пещерный биом (капельниковые пещеры), и мод решал, что осадков нет.

**Как исправлено.** Другим модам здесь теперь отдаётся биом на поверхности; сама ваниль по-прежнему берёт биом на
y 0, так что рельеф не меняется. Чанки, созданные до обновления, сохраняют биом, который Ecliptic Seasons для них
запомнил. На 26.3 биом и так берётся на поверхности.

</details>

## Совместимость

- Работает вместе с **Distant Horizons**, **Iris** и **Sodium**.
- **Ecliptic Seasons**: дождь, снег и их звук работают с 0.0.25 (проверено с 0.15.5 на 1.21.1 NeoForge).
- **TerraBlender** и моды на биомы через него поддерживаются: регионы биомов берутся из карты ReTerraForged.
  Проверено с **Biomes O' Plenty** и **Terralith** вместе на 1.21.1, 1.21.11, 26.1.2, 26.2 и 26.3 (NeoForge и Fabric).
  На 26.3 TerraBlender ставьте сами: Biomes O' Plenty он нужен, но на его странице Modrinth не указан. С Terralith
  в ванильных лесах растут деревья Terralith, а не ReTerraForged: Terralith переписывает их списки фич.
- Для модов на пещерные биомы есть режим совместимости пещер на странице «Пещеры».
- **William Wythers' Overhauled Overworld** 2.6.7 (1.21.1 NeoForge и Fabric): его биомы и поверхности генерируются,
  швов на склонах нет с 0.0.17. В лесах растут его деревья вместо деревьев ReTerraForged. На Fabric ему нужны
  Fabric Loader 0.18.5 и Fabric API 0.116.12 или новее.

- **ModernFix** и **C2ME**: работают вместе с модом с 0.0.23 (проверено на 26.1.2 Fabric с C2ME 0.4.0-alpha и
  ModernFix-mVUS 5.27.19). **Модуль OpenCL** у C2ME (C2ME OpenCL Acceleration) ускоряет только ванильный рельеф: с
  0.0.24 мир ReTerraForged откатывается сам, обычный мир генерируется на процессоре (C2ME всё равно распараллеливает
  генерацию), а Незер и Энд остаются на видеокарте. С 0.0.23 поставьте `allowIncompatibilityFallback = true` в разделе
  `[openclAccel]` уже созданного `config/c2me.toml` (файл, написанный с нуля без `version = 3`, C2ME сбрасывает). На 1.21.1 (0.0.32) самотест с C2ME проходит.
- **TerraBlender** с 0.0.32 необязательная зависимость: мод запускается без неё, миры без неё не меняются.
- **Миксины других модов** (0.0.32): инъекции, которые конфликтовали бы с другим модом в том же месте, заменены на совместимые (wrap вместо redirect, без захвата локальных переменных).
- **Точка спавна** (0.0.32): поиск подменяется только в мирах с пресетом ReTerraForged; в остальных мирах ванильный.
- **Darker Depths и Deeper Darker** (0.0.32): при их установке диагностические счётчики TerraBlender работали на каждом выборе биома и писали строки в лог. Теперь только с `-Drtf.tbdiag=true`.

## Проверка

Каждый релиз перед публикацией проверяется на каждой версии Minecraft, для которой он выходит (сейчас пять), и на обоих загрузчиках. Тесты встроены в мод
(команды `rtf`, включаются только в тестовых запусках) и лежат в репозитории (`tools/*.sh`), так что их может
повторить кто угодно.

<details>
<summary><b>1. Самопроверка на сервере</b>: <code>tools/servertest.sh</code>, 42 пункта</summary>

Ставится настоящий выделенный сервер NeoForge или Fabric, экспортируется датапак пресета, создаётся новый мир с
пресетом «По умолчанию», и в нём запускается `rtf selftest`. Вывод на 1.21.11 NeoForge (список ниже неполный: он показывает 18 из 42 проверок):

```
[RTF selftest] ok   overworld uses the ReTerraForged generator
[RTF selftest] ok   the forest grows ReTerraForged trees, not vanilla ones [reterraforged:forest_trees]
[RTF selftest] ok   trees stand in the forest at -1920, 384 (150 trunk columns)
[RTF selftest] ok   the structure page sets the villages' spacing, separation and salt
[RTF selftest] 289 height samples in 11988 ms, ground 10 .. 154, water at 194 of 289 points
[RTF selftest] 81 chunks fully generated in 2608 ms
[RTF selftest] ok   terrain has relief (ground 10 .. 154)
[RTF selftest] ok   terrain reaches above sea level
[RTF selftest] ok   there is water (194/289)
[RTF selftest] ok   terrain stays inside the world height
[RTF selftest] ok   overworld bedrock floor is whole (holes: 0)
[RTF selftest] ok   spawn is on dry land (2640, 93, 672: Sand)
[RTF selftest] ok   bee nests grow on the mod's trees
[RTF selftest] ok   saplings grown next to flowers get bee nests (7 of 199)
[RTF selftest] ok   a village is found
[RTF selftest] ok   the village stands on land above the sea
[RTF selftest] ok   nether bedrock floor is whole (holes: 0)
[RTF selftest] ok   nether bedrock roof is whole (holes: 0)
[RTF selftest] ok   nether is made of netherrack as usual (22/90)
[RTF selftest] ok   the end island is there
[RTF selftest] PASS all 18
```

Ещё скрипт падает на любой ошибке мода в логе сервера.

</details>

<details>
<summary><b>2. Тест структур</b>: <code>rtf structures</code></summary>

Каждая ванильная структура ищется рядом со спавном, генерируется и измеряется: сколько частей висят в воздухе или
закопаны, а у деревень — сколько блоков тропинок на улицах. «Не рядом» — нормально (это решают биомы); висящая или
закопанная деревня — ошибка.

```
[RTF structures] village_plains at 3323 52 -819: 110 pieces, 0 floating, 0 buried, 1420 path blocks, terrain badlands
[RTF structures] village_desert at 2177 53 549: 79 pieces, 0 floating, 0 buried, 1040 path blocks, terrain coast
[RTF structures] village_taiga at 4199 67 -1020: 92 pieces, 0 floating, 0 buried, 558 path blocks, terrain hills
[RTF structures] monument at 1632 39 816: 1 pieces, terrain deep_ocean
[RTF structures] stronghold at 1758 2 -1201: 168 pieces, terrain deep_ocean
[RTF structures] trial_chambers at 2222 -56 574: 254 pieces, terrain coast
[RTF structures] PASS: 15 of 26 structures found in 49 s
```

Потом клиентский тест летит к деревне и делает снимок (1.21.11 NeoForge):

![Пустынная деревня, найденная тестом структур](docs/test-village.jpg)

</details>

<details>
<summary><b>3. Тест экрана пресетов кликами</b>: <code>tools/clienttest.sh &lt;загрузчик&gt; gui</code></summary>

Запускается настоящий клиент игры, и им управляют события мыши, как игрок: вкладка «Мир» → «Настроить» → клик по
«По умолчанию» → клик по ползунку масштаба континентов → `>>` по всем страницам (каждая сначала прокручена вниз) →
`<<` обратно с проверкой, что каждая страница прокручена туда же, где её оставили → «Готово» → создаётся мир, и
генератор должен использовать значение, выставленное кликом. Потом второй круг с другим значением. 12 скриншотов за
прогон.

```
[RTF clienttest] click Button 'Customize' at 480 80 on CreateWorldScreen
[RTF clienttest] selected after the click: Default
[RTF clienttest] continent scale 3000 -> 2530 (slider click)
[RTF clienttest] pages with >>: [WorldSettingsPage, SurfaceSettingsPage, CaveSettingsPage, ClimateSettingsPage, ...]
[RTF clienttest] round 0: the world will use the preset with continent scale 2530
```

| После клика по ползунку | Последняя страница |
|---|---|
| ![](docs/test-gui-slider.jpg) | ![](docs/test-gui-last-page.jpg) |

</details>

<details>
<summary><b>4. Стресс-тест</b>: <code>tools/stresstest.sh</code></summary>

`rtf stress` работает внутри настоящего сервера. Каждая настройка экрана пресетов проверяется на обоих концах ползунка, вместе с каждым встроенным пресетом, «всё на минимуме», «всё на максимуме» и случайными комбинациями. Каждый вариант проходит через кодек пресета, регистры датапака нового мира, превью и тайлы мира со всеми фильтрами, с ограничением по времени. Результаты 0.0.13 (1.21.1 NeoForge):

| Режим | Вариантов | Успешно | Ошибок | Зависаний | Время |
|---|---:|---:|---:|---:|---:|
| обычный: каждый конец ползунка, пресеты, 40 случайных комбинаций | 404 | 404 | 0 | 0 | 65 с |
| `abuse`: NaN, ±бесконечность, отрицательные и огромные числа, как в сломанном файле пресета | 935 | 935 | 0 | 0 | 126 с |

```
stress: 404 variants in 65 s: 404 ok, 0 failed, 0 hung; slowest terrain().general.terrainSeedOffset = 0 (1338 ms)
stress: 935 variants in 126 s: 935 ok, 0 failed, 0 hung; slowest world().continent.continentSkipping = 0.0 (720 ms)
```

`effects` проверяет, что каждая настройка доходит до мира: каждое значение сравнивается с Default в ячейках рельефа и в датапаке. 153 из 167 настроек меняют мир. Остальные 14 ничего не делают и в оригинальном 0.0.6, и экран пресетов показывает их неактивными: камень только для руд, эрозия простого камня, доля горных и вулканических биомов, пять размеров рек-притоков, расстояния начала озёр, контрольные точки островов и уровень лавы.

</details>

<details>
<summary><b>5. Производительность</b>: <code>tools/benchmark.sh</code>, spark и Java Flight Recorder</summary>

`tools/benchmark.sh` дважды запускает настоящий сервер NeoForge на одном сиде: с пресетом «По умолчанию» (ReTerraForged) и без него (ванильная генерация, точка отсчёта). В каждом мире `rtf bench` сначала прогревается, потом генерирует квадрат чанков далеко от спавна до полного статуса: по одному потоку на ядро запрашивают чанки, а сервер продолжает тикать. Сервер работает со [spark](https://spark.lucko.me) (`spark tps` во время генерации, вывод в консоль, ничего не загружается) и с записью Java Flight Recorder.

![Скорость генерации мира](docs/benchmark.png)

На 1.21.1 ReTerraForged примерно на 9 % медленнее ванили. Мод считает целую карту континентов, эрозию и фильтры поверх работы ванили. 0.0.13 сократил часть разрыва: эрозия спрашивала у рельефа каждой ячейки модификатор эрозии через цепочку делегатов, и в 0.0.12 на это уходило 4.5 % всего времени генерации. На 26.3, с новыми скомпилированными функциями плотности Mojang, мод работает так же быстро, как ваниль.

| Один прогон | Чанков | Время | Чанков/с | На чанк | Занято кучи |
|---|---:|---:|---:|---:|---:|
| 1.21.1 ваниль | 2304 | 27.1 с | 84.9 | 11.8 мс | 958 МБ |
| 1.21.1 ReTerraForged 0.0.13 | 2304 | 30.0 с | 76.7 | 13.0 мс | 1193 МБ |
| 26.3 ваниль | 4096 | 15.5 с | 264.0 | 3.8 мс | 1101 МБ |
| 26.3 ReTerraForged 0.0.13 | 4096 | 15.2 с | 270.1 | 3.7 мс | 1165 МБ |

**Скорость генерации, 0.0.32** (1.21.1 NeoForge, 1024 чанка на 16 ядрах, сборки чередовались; мс на чанк): Default — 15,2 в 0.0.31 против 14,8 в 0.0.32 (−2,6 %, разница в пределах разброса прогонов); пресет с «Болотными низинами» и «Болотами в низинах» — 15,3 против 14,6 (−4,6 %); ваниль на той же машине — 13,4. Сервер с 4 ядрами: без изменений (в среднем по 5 прогонам 25,1 против 25,4, разброс 2 мс). Генерация стала быстрее на несколько процентов, заметного игроку ускорения нет, мир тот же блок в блок.

**Кэш тайлов, 0.0.30.** Раньше тайл выбрасывался после 64-го чанка, и соседи считали его заново: в `rtf bench 96` 47 % тайлов считались дважды. Теперь таких нет: при
2 ГБ 53,1 чанка/с против 44,6 (пик кучи 1615 МБ против 1664), при 3 ГБ 60,2 против 58,8 (1786 МБ против 2575). `rtf bench 32` при 3 ГБ, по четыре прогона по очереди: 54,2
чанка/с против 50,6, в пределах шума такого короткого прогона. Ваниль в тех же условиях: 61,0 чанка/с, ReTerraForged 55,7 (TPS за минуту 17,76 и 17,74, медиана тика 5,3 и
5,7 мс, 95 % 49,4 и 58,9 мс).

**spark во время генерации** (`spark tps`, те же прогоны). 16 потоков запрашивают чанки одновременно, гораздо больше, чем игрок на элитрах, поэтому TPS на время теста проседает и у ванили и сразу после него возвращается к 20:

| | TPS 5 с / 10 с / 1 мин | Тик мин / медиана / 95 % / макс, мс | CPU (процесс) |
|---|---|---|---|
| 1.21.1 ваниль | 7.4 / 10.8 / 17.5 | 0.3 / 9.2 / 81.0 / 110.3 | 28 % |
| 1.21.1 ReTerraForged | 8.2 / 11.6 / 17.8 | 0.3 / 7.6 / 102.2 / 121.2 | 31 % |
| 26.3 ваниль | 10.2 / 13.5 / 18.5 | 0.1 / 3.5 / 20.3 / 45.7 | 49 % |
| 26.3 ReTerraForged | 9.7 / 13.0 / 18.4 | 0.1 / 3.9 / 21.5 / 42.8 | 50 % |

Сырой вывод spark (26.3, ReTerraForged):

```
> TPS from last 5s, 10s, 1m, 5m, 15m:
 9.65, 13.02, 18.36, 19.65, 19.88

> Tick durations (min/med/95%ile/max ms) from last 10s, 1m:
 0.1/3.9/21.5/42.8;  0.1/3.9/21.5/42.8

> CPU usage from last 10s, 1m, 15m:
 57%, 53%, 53%  (system)
 50%, 53%, 53%  (process)
```

**Куда уходит время** (Java Flight Recorder, `jfr view hot-methods`, доля всех сэмплов). Больше всего времени по-прежнему занимают ванильные водоносные слои и шум. Самый горячий метод мода — фильтр эрозии:

| Метод | 1.21.1 | 26.3 |
|---|---:|---:|
| ванильный `Aquifer.computeSubstance` | 15.0 % | 4.4 % |
| ванильный `Beardifier.compute` (структуры) | 6.1 % | 1.6 % |
| ReTerraForged `Erosion.applyDrop` | 5.0 % | 6.0 % |
| ReTerraForged `Perlin.compute` | 1.7 % | 2.9 % |
| ReTerraForged `ITerrain$Delegate.erosionModifier` | 4.5 % в 0.0.12, убран в 0.0.13 | – |

</details>

### 0.0.32, пять версий

На 1.21.1, 1.21.11, 26.1.2, 26.2 и 26.3, NeoForge и Fabric, на настоящих выделенных серверах:

- **Самотест:** 42 проверки проходят на каждой версии и загрузчике (на 1.21.1 с модами биомов: 41).
- **Структуры:** проверены на каждой версии и загрузчике.
- **Два загрузчика:** миры NeoForge и Fabric совпадают по высотам и биомам.
- **Отпечаток относительно 0.0.31:** пресет из 0.0.31 на этой сборке даёт ту же поверхность и высоты. Все 4501 строка совпадают на 1.21.1, 1.21.11 и 26.2. На 26.1.2 и 26.3 отличаются 1 и 4 полных чанка из 96: там фичи пересекают границу чанка и зависят от порядка генерации соседей.
- **Новый эталон:** новый эталон отпечатков отличается от старого только пластами камня; все 4225 строк высот и биомов совпадают на каждой версии.
- **Biomes O' Plenty с TerraBlender** (NeoForge): самотест и биомы модов проходят.
- **Отказ генерации тайла:** сервер завершается сам, за 47–48 с.
- **GUI-тест клиента** на русском на обоих загрузчиках: 31 скриншот, включая пять восстановленных подсказок.
- **Юнит-тесты:** 98 на каждой версии, кроме 26.3 (92).

Только на 1.21.1:

- **C2ME** на NeoForge и на Fabric (на Fabric вместе с ModernFix): самотест проходит.
- **Biomes O' Plenty с Regions Unexplored** на NeoForge: 89 из 121 вида биомов, как в 0.0.31.
- **Стресс-тест:** 467 вариантов пресета, без сбоев.
- **GUI-тест** на английском.
- **Мир, созданный 0.0.31,** открытый этой сборкой, даёт тот же рельеф (CI).

Не проверено: Biomes O' Plenty с Regions Unexplored на Fabric 1.21.1. Lithostitched 1.8.1 не грузится с Fabric Loader 0.18.3, а Regions Unexplored 0.6.3 требует Fabric Loader 0.17 или новее.


### Результаты 0.0.13

| Minecraft | Самопроверка, NeoForge | Самопроверка, Fabric | Тест структур | Тест кликами, NeoForge | Тест кликами, Fabric |
|---|:-:|:-:|:-:|:-:|:-:|
| 1.21.1 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.3 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.4 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.5 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.8 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.10 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 1.21.11 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 26.1.2 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 26.2 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |
| 26.3 | ✅ 18/18 | ✅ 18/18 | ✅ найдено 15 из 26 | ✅ 12 скриншотов | ✅ 12 скриншотов |

Ноль ошибок мода во всех логах серверов и клиентов. По дороге к этой таблице тесты нашли две вещи: на 26.3 левая кнопка мыши имеет номер 1 вместо 0, поэтому тест кликами ничего не нажимал, пока это не исправили, а на 1.21.4 Fabric тест кликами один раз упал и проходил при каждом повторе.

## Исходники

Ветка `main` — Minecraft 1.21.1, остальные версии — ветки `mc/<версия>`, по одному коммиту порта поверх предыдущей.

## Авторы и лицензия

TerraForged — dags; ReTerraForged — racoonman2; этот форк — Vladeezyy.

Лицензия: **LGPL-3.0-only** ([LICENSE](LICENSE), текст GPL-3.0 в [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. Форки и изменённые версии обязаны оставаться открытыми под той же лицензией и указывать авторов. Код, взятый из исходных проектов, сохраняет лицензию MIT и копирайты авторов ([LICENSE-MIT](licenses/LICENSE-MIT)). Пожалуйста, скачивайте мод только со страниц официальных релизов.

</details>
