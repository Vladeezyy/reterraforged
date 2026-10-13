# ReTerraForged (fork for 1.21.1+)

Рабочий форк [ReterraForged](https://github.com/Campione01/ReterraForged) (Campione01, поверх racoonman2 / TerraForged,
лицензия **MIT**) — той же сборки, что лежит на CurseForge как «ReTerraforged Unofficial» (автор пометил её брошенной).
NeoForge и Fabric, Minecraft 1.21.1. История коммитов авторов сохранена.

## Что исправлено в форке

- **Новый мир не создавался:** в последнем коммите апстрима в миксин `MixinNoiseChunk` добавили не-`private` статический
  метод, Mixin такое запрещает, и генерация падала сразу при создании мира (`contains non-private static method`).
  Метод вынесен в `C2MECompat`, юнит-тесты апстрима проходят.
- **Мод грузился на любой версии Minecraft** (NeoForge — от 1.21.1 и выше, Fabric — даже от 1.20.1) и падал на чужой
  вместо понятного сообщения. Теперь только 1.21.1, NeoForge 21.1.x, Fabric Loader 0.16+, Fabric API 0.102+.
- **Обрезанные кнопки** на экране пресетов в маленьком окне: правая колонка теперь не уже 150 пикселей.

## Проверка

`tools/servertest.sh` ставит настоящие серверы NeoForge и Fabric 1.21.1, кладёт мод и пресет (датапак, выгруженный из
экрана пресетов) в новый мир и выполняет `rtf selftest`: мир строит ReTerraForged, рельеф от 10 до 245, есть море,
спавн на суше, деревня на земле, бедрок в обычном мире и Незере целый, Энд на месте.

| Загрузчик | Результат | Ошибки в логе |
|---|---|---|
| NeoForge 21.1.201 | 13 / 13 | 0 |
| Fabric 0.16.3 + API 0.102 | 13 / 13 | 0 |

Сравнение 1024 сгенерированных чанков ванилы и ReTerraForged на одном сервере: ни одной ошибки
«setBlock in a far chunk» ни там, ни там (в одном из клиентских миров такие ошибки давали ванильные большие сталактиты).

С клиентом: экран пресетов с картой-превью, мир на пресете Default, горы, пустыня с дюнами и эрозией, река.

| ![Превью](docs/preset-preview.png) | ![Горы](docs/mountains.png) |
|---|---|
| ![Дюны](docs/desert-dunes.png) | ![Река](docs/river.png) |

---

# ReTerraForged

![ReTerraForged](neoforge/src/main/resources/logo.png)

Terrain generation for Minecraft 1.21.1 on NeoForge and Fabric.

## Island generation

- Optional archipelagos with configurable density, size, height, and terrain scale.
- Dedicated island mountains and volcano-shaped peaks.
- Adjustable offshore depth, beach width, beach coverage, and coast transitions.
- Island-aware climate routing and beach-biome selection that avoids stone-shore dominance.
