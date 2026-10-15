# Litematica Online Hub

Browse the [Litematica Online Hub](https://litematica-online-hub.shop) gallery
from inside Minecraft. Look through schematic covers, read descriptions, filter
by tag and game version, and download schematics straight into Litematica's
schematics folder — without leaving the game.

## Features

- In-game gallery with cover images, descriptions, download and like counts
- Search by title, author or keyword
- Filter by tag and by Minecraft version
- Multi-cover carousel per publication
- One-key download that drops the file into the Litematica schematics directory
- Offline cache of the last loaded page, so the gallery opens without internet
- In-game update notice when a newer mod version is published
- Configurable keybind for opening the gallery

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.156.0+ |
| Litematica | 0.21.0+ |
| MaLiLib | 0.24.0+ |
| Java | 21+ |

## Default keybind

`Open Online Gallery` — rebind it in Minecraft's controls under the
*Keybinds* screen, category **Litematica Online Hub**.

## Building from source

1. Clone this repository.
2. Put the compile-time dependencies in `./libs`:
   - `litematica-fabric-26.2-0.28.8.jar`
   - `malilib-fabric-26.2-0.29.6.jar`

   Paths can be changed in `gradle.properties` via `litematica_jar` and
   `malilib_jar`.
3. Build:

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## License

[MIT](LICENSE) — Copyright (c) 2026 ponos_dinozavr