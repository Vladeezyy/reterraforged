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
| 26.3 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

Every release has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.
Earlier versions stay on the [releases page](https://github.com/Vladeezyy/reterraforged/releases), and their source
on protected branches `release/<version>-<minecraft>` (for example `release/0.0.14-1.21.1`).

## Installation

**Singleplayer.** Put the jar into the `mods` folder. When creating a world: World tab → Customize → pick a preset →
Done. Without this step the world will be vanilla. The experimental settings warning is expected, press Proceed.

**Server.** Put the jar into `mods/`. For a new world, put the datapack `reterraforged-preset-<version>.zip` into
`world/datapacks/` before the first start. Your own preset can be exported with the "Export As Datapack" button on the
preset screen.

The mod only affects new worlds created with its preset. It does not change an existing world.

## Features

- **TerraForged terrain:** continents with coastlines, shallows and bays, mountain chains, plateaus, hills, dales,
  steppes, badlands, volcanoes.
- **Water:** river networks with main rivers and tributaries, lakes on rivers, wetlands; riverbed depth and width are
  configurable.
- **Surface:** water droplet erosion simulation, rock and scree on steep slopes, rock strata, snow only where it would
  settle naturally.
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
  exported as a datapack.
- **Structures:** spacing, separation and salt for every structure set, or disable it. Villages and other surface
  structures are not placed on mountains, as in 0.0.6.
- **Presets:** the "Default" of ReTerraForged 0.0.6 and the classic TerraForged presets (Beautiful, Huge Biomes, Lite,
  Vanilla-ish).
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

## Problems and fixes in detail

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

## Compatibility

- Works with **Distant Horizons**, **Iris** and **Sodium**.
- **TerraBlender** and biome mods using it are supported: biome regions follow the ReTerraForged map. Tested with
  **Biomes O' Plenty** on 1.21.1, 1.21.11 and 26.3 (NeoForge and Fabric). On 26.3 install TerraBlender yourself: Biomes
  O' Plenty needs it but its Modrinth page does not list it.
- For cave biome mods there is a cave compatibility mode on the Caves page.
- **William Wythers' Overhauled Overworld** 2.6.7 (1.21.1 NeoForge and Fabric): its biomes and surfaces generate,
  no seams on cliffs since 0.0.17. Its forests grow its own trees instead of the ReTerraForged ones. On Fabric it
  needs Fabric Loader 0.18.5 and Fabric API 0.116.12 or newer.

## Testing

Every release is checked on all 10 Minecraft versions and both loaders before it is published. The tests are part
of the mod (`rtf` commands, enabled only in test runs) and of the repository (`tools/*.sh`), so anyone can repeat them.

<details>
<summary><b>1. Server self test</b>: <code>tools/servertest.sh</code>, 18 checks</summary>

A real dedicated NeoForge or Fabric server is installed, the preset datapack is exported, a new world is created with
the "Default" preset and `rtf selftest` runs in it. Output of 1.21.11 NeoForge:

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
the previous one. Build: `./gradlew build` (Java 21 for 1.21.x, Java 25 for 26.x).

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
| 26.3 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.20](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.20-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

В каждом релизе — jar для NeoForge, jar для Fabric (нужен Fabric API) и датапак пресета для серверов.
Прошлые версии остаются на [странице релизов](https://github.com/Vladeezyy/reterraforged/releases), а их исходники —
в защищённых ветках `release/<версия>-<minecraft>` (например, `release/0.0.14-1.21.1`).

## Установка

**Одиночная игра.** Положите jar в папку `mods`. При создании мира: вкладка «Мир» → «Настроить» → выберите пресет →
«Готово». Без этого шага мир будет ванильным. Предупреждение об экспериментальных настройках — нормально, нажмите
«Продолжить».

**Сервер.** Jar — в `mods/`. Для нового мира положите датапак `reterraforged-preset-<версия>.zip` в
`world/datapacks/` до первого запуска. Свой пресет можно выгрузить кнопкой «Экспорт в датапак» на экране пресетов.

Мод влияет только на новые миры, созданные с его пресетом. Обычный мир он не меняет.

## Возможности

- **Рельеф TerraForged:** материки с береговыми линиями, отмелями и заливами, горные цепи, плато, холмы, долины,
  степи, бесплодные земли, вулканы.
- **Вода:** речные сети с главными реками и притоками, озёра на реках, болота, глубина и ширина русла настраиваются.
- **Поверхность:** симуляция эрозии каплями воды, камень и осыпи на крутых склонах, пласты горных пород, снег только
  там, где он лёг бы естественно.
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

## Проблемы и исправления подробно

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

## Совместимость

- Работает вместе с **Distant Horizons**, **Iris** и **Sodium**.
- **TerraBlender** и моды на биомы через него поддерживаются: регионы биомов берутся из карты ReTerraForged.
  Проверено с **Biomes O' Plenty** на 1.21.1, 1.21.11 и 26.3 (NeoForge и Fabric). На 26.3 TerraBlender ставьте сами:
  Biomes O' Plenty он нужен, но на его странице Modrinth не указан.
- Для модов на пещерные биомы есть режим совместимости пещер на странице «Пещеры».
- **William Wythers' Overhauled Overworld** 2.6.7 (1.21.1 NeoForge и Fabric): его биомы и поверхности генерируются,
  швов на склонах нет с 0.0.17. В лесах растут его деревья вместо деревьев ReTerraForged. На Fabric ему нужны
  Fabric Loader 0.18.5 и Fabric API 0.116.12 или новее.

## Проверка

Каждый релиз перед публикацией проверяется на всех 10 версиях Minecraft и обоих загрузчиках. Тесты встроены в мод
(команды `rtf`, включаются только в тестовых запусках) и лежат в репозитории (`tools/*.sh`), так что их может
повторить кто угодно.

<details>
<summary><b>1. Самопроверка на сервере</b>: <code>tools/servertest.sh</code>, 18 пунктов</summary>

Ставится настоящий выделенный сервер NeoForge или Fabric, экспортируется датапак пресета, создаётся новый мир с
пресетом «По умолчанию», и в нём запускается `rtf selftest`. Вывод на 1.21.11 NeoForge:

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
Сборка: `./gradlew build` (Java 21 для 1.21.x, Java 25 для 26.x).

## Авторы и лицензия

TerraForged — dags; ReTerraForged — racoonman2; этот форк — Vladeezyy.

Лицензия: **LGPL-3.0-only** ([LICENSE](LICENSE), текст GPL-3.0 в [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. Форки и изменённые версии обязаны оставаться открытыми под той же лицензией и указывать авторов. Код, взятый из исходных проектов, сохраняет лицензию MIT и копирайты авторов ([LICENSE-MIT](licenses/LICENSE-MIT)). Пожалуйста, скачивайте мод только со страниц официальных релизов.

</details>
