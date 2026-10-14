# ReTerraForged

TerraForged-style world generation for Minecraft **1.21.1 – 26.3** · **NeoForge** & **Fabric** · the generation of
ReTerraForged 0.0.6, fixed and ported.

![Mountains and a view to the horizon](docs/showcase-mountains.jpg)

| ![Cherry grove above the sea](docs/showcase-cherry-grove.jpg) | ![Valley at the foot of the mountains](docs/showcase-valley.jpg) |
|---|---|
| ![Coast](docs/showcase-coast.jpg) | ![Lake in a forest](docs/showcase-lake.jpg) |
| ![Flower forest on the slopes](docs/showcase-flower-forest.jpg) | ![Swamp and river](docs/showcase-swamp.jpg) |
| ![Wooded badlands](docs/showcase-badlands.jpg) | ![Preset screen with the preview map](docs/preset-screen.jpg) |

<details>
<summary><b>🇬🇧 English — click to open</b></summary>

<sub>Minecraft 26.3, ReTerraForged 0.0.12, the "Default" preset with world height 384, biome size 300 and mountains
×1.15. Complementary Reimagined shaders (Iris) and Distant Horizons with a 256 chunk view, just for the looks: the mod
does not depend on them.</sub>

TerraForged-style world generation: continents and oceans, mountain ranges, river networks with tributaries, lakes,
wetlands, slope erosion, rock strata, and a preset editor with a live map of the world to be. This is a working fork of
[ReterraForged](https://github.com/Campione01/ReterraForged) (Campione01, built on racoonman2 and TerraForged). The
generation is that of the original **ReTerraForged 0.0.6** for 1.20.1, without its bugs and on new game versions.

## Download

| Minecraft | Release | NeoForge | Fabric Loader / API | Java |
|---|---|---|---|---|
| 26.3 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

Every release has a NeoForge jar, a Fabric jar (Fabric API required) and a preset datapack for servers.

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
- **The mod's own trees** from templates (oak, birch, spruce, redwood, willow, acacia, jungle), with bee nests like the
  vanilla ones.
- **Preset editor:** more than 160 settings on eleven pages (world, surface, caves, climate, terrain, islands, rivers,
  filters, structures and more) with a preview map that redraws on every change. Presets can be saved, copied and
  exported as a datapack.
- **Structures:** spacing, separation and salt for every structure set, or disable it. Villages and other surface
  structures are not placed on mountains, as in 0.0.6.
- **Presets:** the "Default" of ReTerraForged 0.0.6 and the classic TerraForged presets (Beautiful, Huge Biomes, Lite,
  Vanilla-ish). The 1.21.1 port's variant is there too, as the last entry of the list.
- **10 languages:** English, Russian, Chinese, German, French, Spanish, Portuguese (Brazil), Japanese, Korean, Polish.

## What this fork fixes

**Generation as in 0.0.6.** The 1.21.1 port this fork started from changed the generation beyond what porting needed:
"Default", mountain climate (snowy peaks next to deserts), rivers, lakes, wetlands, structures on mountains, desert
slopes, bushes, cave biome depth. Every class of the mod was compared with the 0.0.6 source, and all of it is back. The
port's variant remains as the "Original Generation (0.0.6)" switch in Miscellaneous. Worlds created with earlier
versions of the fork keep their generation.

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

**Preset screen:**
- on 1.21.11+ the sliders changed nothing;
- the selected preset was not highlighted, edits and a re-applied preset were lost;
- "back" skipped a page;
- on 1.21.6+ the screen crashed the game.

The structure page and the sliders that did nothing before now work (horizontal scale of hills, dales, plateaus,
badlands, volcano scales). Settings the generation does not use (nor did 0.0.6) are shown as inactive. Presets read
from files are held to valid values, and a broken file in the presets folder no longer crashes the game.

## Compatibility

- Works with **Distant Horizons**, **Iris** and **Sodium**; the screenshots above were taken with them.
- **TerraBlender** and biome mods using it are supported: biome regions follow the ReTerraForged map.
- For cave biome mods there is a cave compatibility mode on the Caves page.

## Testing

Every version is tested on real NeoForge and Fabric servers (`tools/servertest.sh`). A world is created with the
"Default" preset, and `rtf selftest` checks 18 points:
- terrain, sea and a spawn on dry land;
- the mod's trees stand in the forest and have bee nests;
- bedrock, the Nether and the End;
- the structure page reaches the world.

`rtf structures` finds and builds every vanilla structure near the spawn and checks that villages stand on the ground
and have streets. The client tests (`tools/clienttest.sh`) create a world through the preset screen with real mouse
clicks and go through every slider and switch. The stress test (`tools/stresstest.sh`) runs every setting at its
extremes and hundreds of random combinations.

## Source

The `main` branch is Minecraft 1.21.1, the other versions are `mc/<version>` branches, one port commit each on top of
the previous one. Build: `./gradlew build` (Java 21 for 1.21.x, Java 25 for 26.x).

## Credits and license

TerraForged by dags; ReTerraForged by racoonman2; the 1.21.1 port by Campione01; this fork by Vladeezyy. MIT license.

</details>

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

<sub>Minecraft 26.3, ReTerraForged 0.0.12, пресет «По умолчанию» с высотой мира 384, размером биомов 300 и горами ×1.15.
Шейдеры Complementary Reimagined (Iris) и Distant Horizons с прорисовкой 256 чанков — только для красоты, мод от них
не зависит.</sub>

Генерация мира в духе TerraForged: материки и океаны, горные хребты, речные сети с притоками, озёра, болота, эрозия
склонов, пласты горных пород — и редактор пресетов с живой картой будущего мира. Это рабочий форк
[ReterraForged](https://github.com/Campione01/ReterraForged) (Campione01, поверх racoonman2 и TerraForged). Генерация —
как в оригинальном **ReTerraForged 0.0.6** для 1.20.1, только без его ошибок и на новых версиях игры.

## Скачать

| Minecraft | Релиз | NeoForge | Fabric Loader / API | Java |
|---|---|---|---|---|
| 26.3 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.3) | 26.3.0.76 | 0.19.5 / 0.162 | 25 |
| 26.2 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.2) | 26.2.0.90 | 0.19.5 / 0.161 | 25 |
| 26.1.2 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-26.1.2) | 26.1.2.116 | 0.19.5 / 0.155 | 25 |
| 1.21.11 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.11) | 21.11.45 | 0.17.3 / 0.141 | 21 |
| 1.21.10 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.10) | 21.10.64 | 0.17.3 / 0.138 | 21 |
| 1.21.8 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.8) | 21.8.54 | 0.17.2 / 0.136 | 21 |
| 1.21.5 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.5) | 21.5.98 | 0.16.14 / 0.128 | 21 |
| 1.21.4 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.4) | 21.4.158 | 0.16.14 / 0.119 | 21 |
| 1.21.3 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.3) | 21.3.97 | 0.16.14 / 0.114 | 21 |
| 1.21.1 | [0.0.12](https://github.com/Vladeezyy/reterraforged/releases/tag/v0.0.12-1.21.1) | 21.1.201 | 0.16.3 / 0.102 | 21 |

В каждом релизе — jar для NeoForge, jar для Fabric (нужен Fabric API) и датапак пресета для серверов.

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
- **Деревья мода** из шаблонов — дубы, берёзы, ели, секвойи, ивы, акации, джунгли — с ульями, как у ванильных.
- **Редактор пресетов:** больше 160 настроек на одиннадцати страницах — мир, поверхность, пещеры, климат, рельеф, острова, реки,
  фильтры, структуры и прочее — с картой-превью, которая перерисовывается при каждом изменении. Пресеты сохраняются,
  копируются и выгружаются как датапак.
- **Структуры:** для каждого набора структур можно задать расстояние, разнос и salt или отключить его. Деревни и
  другие наземные структуры не ставятся на горы, как в 0.0.6.
- **Пресеты:** «По умолчанию» из ReTerraForged 0.0.6 и классические пресеты TerraForged — Beautiful, Huge Biomes,
  Lite, Vanilla-ish. Есть и вариант порта на 1.21.1, последней строкой списка.
- **10 языков:** русский, английский, китайский, немецкий, французский, испанский, португальский (Бразилия),
  японский, корейский, польский.

![Экран пресетов на русском](docs/preset-screen-ru.jpg)

## Что исправлено в форке

**Генерация как в 0.0.6.** Порт на 1.21.1, с которого начинался этот форк, менял генерацию сверх нужного: «Default»,
климат гор (ледяные пики рядом с пустыней), реки, озёра, болота, структуры на горах, склоны пустынь, кусты, глубину
пещерных биомов. Каждый класс мода сверен с исходником 0.0.6, и всё это возвращено. Вариант порта остался
переключателем «Оригинальная генерация (0.0.6)» в разделе «Прочее». Миры, созданные на прошлых версиях форка,
сохраняют свою генерацию.

**Ошибки, из-за которых мир не работал:**
- новый мир не создавался — генерация падала из-за ошибки в миксине;
- на NeoForge деревья и другие элементы пресета не появлялись: модификаторы биомов лежали в несуществующем реестре;
- на 26.x деревья мода не росли: с 26.1 трава вышла из тега `#dirt`, а с 26.3 шаблоны деревьев читались как воздух;
- сервер на Fabric зависал при генерации Незера и Энда, одиночная игра на Fabric — на новых чанках;
- `/locate`, карты картографа и дельфины надолго вешали сервер;
- утечка памяти: кэши генератора не освобождались до закрытия игры;
- крайние значения настроек давали NaN в рельефе.

**Экран пресетов:**
- на 1.21.11+ ползунки ничего не меняли;
- выбранный пресет не подсвечивался, правки и повторно применённый пресет терялись;
- «назад» перескакивал через страницу;
- на 1.21.6+ экран ронял игру.

Ещё заработали страница структур и ползунки, которые раньше ничего не делали (горизонтальный масштаб холмов, долин,
плато, бесплодных земель, масштабы вулканов). Настройки, которые генерация не использует (и в 0.0.6 тоже), показаны
неактивными. Пресеты из файлов приводятся к допустимым значениям, сломанный файл в папке пресетов больше не роняет игру.

## Совместимость

- Работает вместе с **Distant Horizons**, **Iris** и **Sodium** — на этой связке сняты скриншоты выше.
- **TerraBlender** и моды на биомы через него поддерживаются: регионы биомов берутся из карты ReTerraForged.
- Для модов на пещерные биомы есть режим совместимости пещер на странице «Пещеры».

## Проверка

Каждая версия проверяется на настоящих серверах NeoForge и Fabric (`tools/servertest.sh`). Мир создаётся с пресетом
«По умолчанию», и `rtf selftest` проверяет 18 пунктов:
- рельеф, море и спавн на суше;
- деревья мода стоят в лесу, на них есть ульи;
- бедрок, Незер и Энд;
- страница структур доходит до мира.

`rtf structures` находит и строит каждую ванильную структуру рядом со спавном и проверяет, что деревни стоят на земле
и с улицами. Клиентские тесты (`tools/clienttest.sh`) создают мир через экран пресетов настоящими кликами мыши и
проходят каждый ползунок и переключатель. Стресс-тест (`tools/stresstest.sh`) прогоняет каждую настройку на крайних
значениях и сотни случайных комбинаций.

## Исходники

Ветка `main` — Minecraft 1.21.1, остальные версии — ветки `mc/<версия>`, по одному коммиту порта поверх предыдущей.
Сборка: `./gradlew build` (Java 21 для 1.21.x, Java 25 для 26.x).

## Авторы и лицензия

TerraForged — dags; ReTerraForged — racoonman2; порт на 1.21.1 — Campione01; этот форк — Vladeezyy. Лицензия MIT.

</details>
