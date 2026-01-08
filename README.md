# AE2 Autorequester

A NeoForge mod for Minecraft 1.21.1 that adds conditional auto-crafting to Applied Energistics 2 ME networks.

![Demo](docs/demo.gif)

## Overview

The Autorequester is a machine block that monitors item counts in your ME network and automatically triggers crafting jobs based on configurable rules with multiple conditions. Perfect for maintaining stock levels, managing production chains, and automating resource conversion.

## Features

- **Multi-condition rules**: Each rule can have multiple conditions that all must be satisfied
- **Flexible operators**: `<`, `<=`, `>`, `>=`, `=`, `!=` for precise control
- **Parallel processing**: Multiple rules can trigger crafting jobs simultaneously
- **Active job tracking**: Rules won't trigger if a crafting job for that item is already in progress
- **NBT preservation**: Pick up the block with a wrench and all rules are preserved
- **JEI/EMI integration**: Drag items directly into slots from recipe viewers
- **Jade/WAILA support**: See rule count and connection status at a glance

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- Applied Energistics 2 19.2+

## Installation

1. Install NeoForge for Minecraft 1.21.1
2. Install Applied Energistics 2
3. Download the latest release and place it in your `mods` folder

## Usage

### Getting Started

1. Craft an Autorequester block (see recipe below)
2. Place it adjacent to an ME network cable
3. Right-click to open the configuration GUI
4. Create rules to automate your crafting

### Creating Rules

Each rule consists of:
- **Name** (optional): A custom label for the rule
- **Target Item**: The item to craft when conditions are met
- **Batch Size**: How many to craft per request (default: 64)
- **Conditions**: One or more conditions that must ALL be true

### Example Rules

**Maintain minimum stock:**
> Keep at least 1000 Iron Ingots in the system

- Target: Iron Ingot
- Batch Size: 64
- Condition: Iron Ingot `<` 1000

**Craft with resource limit:**
> Craft Glass only when Sand is abundant

- Target: Glass
- Batch Size: 64
- Conditions:
  - Glass `<` 3000
  - Sand `>=` 1000

**Overflow conversion:**
> Convert excess Cobblestone to Stone

- Target: Stone
- Batch Size: 256
- Condition: Cobblestone `>` 10000

### Wrench Support

Shift + right-click with any wrench tagged `c:tools/wrench` to pick up the block with all rules intact.

## Crafting Recipe

```
┌─────────────┬─────────────┬─────────────┐
│    Logic    │ Engineering │    Logic    │
│  Processor  │  Processor  │  Processor  │
├─────────────┼─────────────┼─────────────┤
│ Calculation │   Pattern   │ Calculation │
│  Processor  │  Provider   │  Processor  │
├─────────────┼─────────────┼─────────────┤
│    Logic    │ Engineering │    Logic    │
│  Processor  │  Processor  │  Processor  │
└─────────────┴─────────────┴─────────────┘
```

## Configuration

Server configuration file: `config/ae2_autorequester-server.toml`

```toml
# Tick interval for checking conditions (20 = 1 second)
checkInterval = 20

# Maximum batch size allowed per crafting request (-1 = unlimited)
maxBatchSize = -1

# Maximum number of rules per autorequester block (-1 = unlimited)
maxRules = -1

# Maximum number of conditions per rule (-1 = unlimited)
maxConditions = -1

# Whether the autorequester requires a channel to operate
requiresChannel = true
```

When limits are configured (not -1), the GUI displays them in tooltips.

## Compatibility

- **JEI/EMI**: Full drag-and-drop support for item selection
- **Jade/WAILA**: Shows rule count and network connection status
- Works with all AE2 pattern types: crafting, processing, and smithing

## License

GNU GPL 3.0

## Credits

Inspired by various AE2 autocrafting addons. Built for the Minecraft modding community.
