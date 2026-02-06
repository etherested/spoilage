# Spoilage

Adds food spoilage and preservation. Items, blocks, and crops decay over time with negative effects, visual decay, and environmental modifiers. Data-driven and fully configurable.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square) ![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219+-E04E14?style=flat-square)


## Features

### 🍖 food spoilage

all registered food items have a configurable lifespan — once it expires, food becomes inedible or transforms into a rotten replacement item, progressing through six freshness levels

| Level    | Freshness | Effects                                                            |
|----------|-----------|--------------------------------------------------------------------|
| Fresh    | 80–100%   | none                                                               |
| Good     | 60–79%    | none                                                               |
| Stale    | 40–59%    | 20% chance Hunger I (5s), 5% chance Weakness I (10s)               |
| Spoiling | 20–39%    | 50% chance Hunger I (15s), 20% Poison I (5s), 10% Nausea (7.5s)    |
| Rotten   | 1–19%     | Hunger II (30s), Poison I (10s), 30% chance Nausea (15s)           |
| Inedible | 0%        | Hunger III (60s), Poison II (20s), Nausea (20s), Weakness II (15s) |

nutrition drops to 25% and saturation to 10% at full spoilage

### 🧊 preservation system

multiple environmental factors combine to slow (or speed up) spoilage

- **depth (Y-level)** — storing food deeper underground slows spoilage, with three configurable tiers (deep, underground, shallow) each giving increasing preservation bonuses
- **biome temperature** — cold biomes slow spoilage, hot biomes speed it up, with fully configurable thresholds and multipliers
- **containers** — specific containers (chests, barrels, shulker boxes) provide preservation bonuses configurable per container type

all preservation factors stack multiplicatively and tooltips display active bonuses

### 🌾 crop lifecycle

crops follow a three-phase lifecycle

1. **growing** — planted seeds grow normally through vanilla growth stages
2. **fresh period** — fully grown crops remain at 100% freshness for a configurable duration (default 3 in-game days)
3. **rotting** — after the fresh period, crops gradually rot and visually regress through growth stages until inedible

applying bonemeal to a rotting crop resets its fresh timer (configurable)

### 🔥 crafting and cooking

- crafting with spoiled ingredients produces a result with weighted average spoilage
- furnace/smoker/blast furnace output inherits spoilage from the input
- crafting with fully rotten (inedible) ingredients is blocked entirely

### 🐄 entity behavior

- villagers refuse to pick up or trade with spoiled food
- animals get poisoned when fed rotten food
- spoiled items get a bonus composting chance — the more rotten, the easier to compost
- spoiled seeds and saplings can't be planted

### 🎨 visual feedback

- color tint overlay on items and blocks that progresses as food spoils (two styles — decay green/brown or warning yellow/red)
- texture blending system that transitions between fresh, stale, and rotten textures — defined via resource packs (`assets/<namespace>/spoilage/<item>.json`)
- modified eating particles for spoiled food

### 💬 tooltips

hovering over food items shows

- freshness level label (fresh, good, stale, etc)
- freshness percentage (optional)
- time remaining until fully spoiled
- active preservation bonuses (container, depth, biome)

each tooltip element can be individually toggled in the config

### 🎁 loot integration

- loot table items receive randomized freshness values per-item
- custom loot function available for datapack authors to set specific spoilage on loot entries

### 🏆 advancements

- **Questionable Taste** — triggered by eating food that has completely spoiled

### 📦 data-driven

spoilage data is loaded from datapacks

- `data/<namespace>/spoilage/<name>.json` — per-item spoilage definitions (lifetime, rotten replacement, texture stages)
- `data/<namespace>/spoilage/groups/<name>.json` — shared group settings (base lifetime, tooltip visibility)


## Mod Compatibility

| Mod               | Integration                                                                                                                                                                                                                   |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Cold Sweat**    | optional soft integration — when Cold Sweat is installed, its temperature system replaces vanilla biome temperatures for more granular preservation calculations, falling back to vanilla if unavailable, togglable in config |
| **EMI, JEI, REI** | spoilage tooltips are automatically hidden in recipe viewer screens to avoid clutter                                                                                                                                          |

the mod uses NeoForge's event system where possible and targeted mixins where necessary — mixin injections are precise and scoped to minimize conflicts with other mods


## Configuration

all settings live in `spoilage.toml` and there's also an in-game config screen

### ⚙️ general

| Setting                  | Default                              | Description                                              |
|--------------------------|--------------------------------------|----------------------------------------------------------|
| `enabled`                | `true`                               | master toggle for the entire spoilage system             |
| `globalSpeedMultiplier`  | `1.0`                                | global spoilage speed (0.01–100x)                        |
| `checkIntervalTicks`     | `100`                                | how often spoilage is processed in ticks (20 = 1 second) |
| `containerSpoilageRates` | shulker 0.85, barrel 0.9, chest 0.95 | per-container preservation multipliers                   |

### 🧊 preservation

| Setting                               | Default | Description                                       |
|---------------------------------------|---------|---------------------------------------------------|
| `yLevelPreservationEnabled`           | `true`  | enable depth-based preservation                   |
| `biomeTemperaturePreservationEnabled` | `true`  | enable biome temperature effects                  |
| `coldSweatIntegrationEnabled`         | `true`  | use Cold Sweat temperatures when available        |
| `yLevelDeep`                          | `0`     | y-level threshold for deep underground            |
| `yLevelUnderground`                   | `50`    | y-level threshold for underground                 |
| `yLevelSurface`                       | `63`    | y-level where preservation stops                  |
| `yLevelDeepMultiplier`                | `0.6`   | spoilage rate at deep level (0.6 = 40% slower)    |
| `yLevelUndergroundMultiplier`         | `0.8`   | spoilage rate underground (0.8 = 20% slower)      |
| `yLevelShallowMultiplier`             | `0.9`   | spoilage rate at shallow depth (0.9 = 10% slower) |
| `biomeColdThreshold`                  | `0.3`   | temperature below which cold bonus applies        |
| `biomeHotThreshold`                   | `0.9`   | temperature above which hot penalty applies       |
| `biomeColdMultiplier`                 | `0.7`   | spoilage rate in cold biomes (0.7 = 30% slower)   |
| `biomeHotMultiplier`                  | `1.3`   | spoilage rate in hot biomes (1.3 = 30% faster)    |

### 🎮 gameplay

| Setting                         | Default | Description                                         |
|---------------------------------|---------|-----------------------------------------------------|
| `offhandAutoCombineEnabled`     | `true`  | auto-merge picked up food with same food in offhand |
| `villagersIgnoreSpoiled`        | `true`  | villagers refuse spoiled food                       |
| `animalsPoisonedByRotten`       | `true`  | animals get poisoned by rotten food                 |
| `preventPlantingSpoiled`        | `true`  | block planting of spoiled seeds                     |
| `lootRandomizationEnabled`      | `true`  | randomize freshness in loot tables                  |

### 🌾 crops

| Setting                   | Default | Description                                           |
|---------------------------|---------|-------------------------------------------------------|
| `cropFreshPeriodTicks`    | `72000` | how long crops stay fresh after full growth (~3 days) |
| `cropRotPeriodTicks`      | `48000` | how long it takes crops to fully rot (~2 days)        |
| `cropMinimumHarvestStage` | `1`     | growth stage at/below which crops are inedible        |
| `bonemealResetsRot`       | `true`  | bonemeal restarts the fresh timer on rotting crops    |

### 💬 tooltips

| Setting                   | Default | Description                      |
|---------------------------|---------|----------------------------------|
| `showRemainingTime`       | `true`  | display time until fully spoiled |
| `showFreshnessWord`       | `true`  | display freshness label          |
| `showFreshnessPercentage` | `false` | display freshness as percentage  |

### 🎨 visuals

| Setting                      | Default | Description                                                   |
|------------------------------|---------|---------------------------------------------------------------|
| `showTintOverlay`            | `true`  | apply color overlay to spoiling items                         |
| `tintStyleRotten`            | `true`  | use decay colors (green/brown) or warning colors (yellow/red) |
| `useTextureBlending`         | `true`  | blend between fresh and spoiled textures                      |
| `blendStartThreshold`        | `0.2`   | spoilage % when blending begins                               |
| `blendFullThreshold`         | `1.0`   | spoilage % when fully rotten texture shown                    |


## FAQ

- **can i add spoilage to modded food items?**
yes — spoilage is data-driven, just create a json file at `data/<namespace>/spoilage/<item>.json` in a datapack to register any item

- **does food spoil while i'm offline?**
spoilage pauses when you log out and resumes when you log back in — no time is lost or gained while offline

- **can i disable spoilage entirely?**
set `enabled = false` in the config file or toggle it in the in-game config screen

- **does food spoil inside containers?**
yes — food in chests, barrels, and shulker boxes still spoils, but containers provide a configurable preservation bonus that slows the rate

- **how does crafting work with spoiled food?**
the result inherits a weighted average of all ingredient spoilage levels — fully rotten ingredients block crafting entirely

- **can i change how fast food spoils?**
use `globalSpeedMultiplier` to scale all spoilage speeds — individual items can have lifetime overrides in their datapack definitions

- **do crops rot on the vine?**
yes — fully grown crops have a fresh period, after which they gradually rot and regress through growth stages, and bonemeal can reset the timer

- **is Cold Sweat required?**
no — Cold Sweat integration is entirely optional, without it the mod uses vanilla biome temperature values


## All Rights Reserved

Copyright © 2026 etherested

This project and all associated files (source code, assets, documentation, and compiled binaries) are the sole property of the copyright holder.