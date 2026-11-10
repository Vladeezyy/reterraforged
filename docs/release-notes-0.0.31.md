# ReTerraForged 0.0.31

TerraForged-style terrain for Minecraft 1.21.1 – 26.3, NeoForge and Fabric. Full list of changes: README in the repository.

## What's new

- **Saplings grow the mod's trees** ([#41](https://github.com/Vladeezyy/reterraforged/issues/41)): with Custom Biome Features on (on in Default), oak, birch, spruce, jungle, acacia and dark oak saplings grow the mod's trees, bone meal too. Spruce, jungle and dark oak grow 2 × 2 from four saplings. Works in existing worlds made with Custom Biome Features.
- **Erosion leaves other mods' surface blocks alone** ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)): Regions Unexplored's Chalk Cliffs keep their chalk grass.
- **Island Horizontal Scale works the right way** ([#38](https://github.com/Vladeezyy/reterraforged/issues/38)); steep shores keep beach sand off mountain slopes, and Chalk Cliffs generate ([#40](https://github.com/Vladeezyy/reterraforged/issues/40)).
- **TerraBlender regions of other biome mods** follow the regions' weights: Regions Unexplored with Biomes O' Plenty gets 89 of 121 biome kinds (was 76).
- **Preset export on a started server** works with Regions Unexplored and Biomes O' Plenty.
- **Flower Forest** has as many flowers as vanilla ([#37](https://github.com/Vladeezyy/reterraforged/issues/37)).

## Versions

- **1.21.1:** NeoForge 21.1.201; Fabric Loader 0.16.3, Fabric API 0.102; Java 21
- **1.21.11:** NeoForge 21.11.45; Fabric Loader 0.17.3, Fabric API 0.141; Java 21
- **26.1.2:** NeoForge 26.1.2.116; Fabric Loader 0.19.5, Fabric API 0.155; Java 25
- **26.2:** NeoForge 26.2.0.90; Fabric Loader 0.19.5, Fabric API 0.161; Java 25
- **26.3:** NeoForge 26.3.0.76; Fabric Loader 0.19.5, Fabric API 0.162; Java 25

Each version has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.

## How to update

- Replace the old jar in `mods/` with the new one. New worlds are made with the preset as before.
- Saplings work right away, also in existing worlds.
- Old worlds keep their preset datapack, so the erosion fix (#40) applies to new worlds; a world's preset can be re-exported to get it.
