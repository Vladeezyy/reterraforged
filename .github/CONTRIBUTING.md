# Contributing to ReTerraForged

Thanks for helping! Bug reports, translations and fixes are all welcome.

## Reporting bugs and ideas

Use the [issue forms](https://github.com/Vladeezyy/reterraforged/issues/new/choose): Bug report, Compatibility issue or
Feature request. Please give the Minecraft version, the loader, the ReTerraForged version, the preset and the settings
you changed, and a log or crash report: most questions back are about these. Problems of this fork go here, not to
the original ReTerraForged or TerraForged.

## How the repository is laid out

- `main` is Minecraft 1.21.1. Every other version has its own branch: `mc/1.21.3`, `mc/1.21.4`, ... `mc/26.3`.
- Fixes are made on `main` first and then ported to each `mc/*` branch.
- `common/` holds the code shared by both loaders; `neoforge/` and `fabric/` the loader-specific parts.
- The world generation follows ReTerraForged 0.0.6 (TerraForged). A change must keep existing worlds as they are:
  new chunks next to old ones must match. New behaviour for old presets goes behind a setting or the preset's
  `settingsVersion` (see `PresetMigration`).

## Building

JDK 21 for 1.21.x, JDK 25 for 26.x:

```
./gradlew build
```

The jars end up in `neoforge/build/libs` and `fabric/build/libs`.

## Testing

The tests run the built jars on real servers and dev clients (Git Bash on Windows works):

- `tools/servertest.sh neoforge fabric` — the self test, the structures, the same world on both loaders.
  `SKIP_SELFTEST=1 SKIP_STRUCTURES=1` for a quick run; `HEIGHTS_REF=<file>` or `LEGACY=<version>` to check that
  worlds keep their terrain.
- `tools/clienttest.sh neoforge gui` — a new world made through the screens with mouse clicks.
- `tools/stresstest.sh neoforge effects` / `curves` — whether every setting of the preset screen changes the world.

## Translations

The mod has 11 languages (`common/src/main/resources/assets/reterraforged/lang`). A new text needs all of them,
with the terms of Minecraft's own translation for that language. Fixes to existing translations are very welcome.

## Pull requests

Keep a pull request to one change, describe what you checked (the template lists it), and write commit messages that
say what changed and why.

## License

ReTerraForged is LGPL-3.0-only. By contributing you agree that your contribution is released under the same license.
Code taken from the original projects keeps its license notice (`licenses/`).
