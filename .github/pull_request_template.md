## What and why

<!-- What this changes and why. Link the issue: "Part of #N" (the issue is closed after the release) or "Fixes #N". -->

## Checked

- [ ] Built (`./gradlew build`) for the versions it touches
- [ ] `tools/servertest.sh neoforge fabric` passes (both loaders, same world)
- [ ] Worlds made before keep their terrain (`HEIGHTS_REF=` against the last release, or `LEGACY=<last version>`)
- [ ] Changes on the preset screen: `tools/clienttest.sh neoforge gui` with the game in Russian (long labels fit)
- [ ] New or changed texts in all 11 languages (`common/src/main/resources/assets/reterraforged/lang`)

## Minecraft versions

<!-- 1.21.1 (main) only, or also ported to the mc/<version> branches. -->
