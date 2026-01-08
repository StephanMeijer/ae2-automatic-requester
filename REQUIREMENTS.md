# AE2 Autorequester - Requirements

## Overview

A machine block that integrates with Applied Energistics 2 ME networks to automatically request crafting jobs based on fully configurable multi-condition rules.

## Core Features

### Autorequester Block

A placeable block that:
- Connects to an AE2 ME network (requires channel)
- Monitors item counts in the network storage
- Triggers crafting jobs when all conditions are met
- Fully configurable through a GUI

### Crafting Rules

Each autorequester can have multiple crafting rules. A rule triggers crafting only when **all** its conditions are satisfied.

#### Rule Structure

```
Rule:
  ├── Name: [Optional custom label, e.g., "Iron Stock"]
  ├── Target Item: [Item to craft]
  ├── Batch Size: [Amount per craft request]
  └── Conditions: (ALL must be true)
      ├── Condition 1: [Item A] [operator] [value]
      ├── Condition 2: [Item B] [operator] [value]
      ├── Condition 3: [Item C] [operator] [value]
      └── ... (unlimited conditions)
```

#### Rule Processing
- **Parallel execution**: All rules with met conditions can trigger crafting jobs simultaneously
- Works with **all AE2 pattern types**: crafting, processing, and smithing table patterns
- **No explicit cooldown**: Rules can trigger every cycle unless blocked
- **Active job tracking**: A rule will NOT trigger if a crafting job for that item is already in progress on any connected Crafting CPU
- **No priority system**: All rules are equal, processed in parallel

#### Condition Structure

Each condition consists of:

| Field | Description |
|-------|-------------|
| **Item** | Any item to monitor (from ME network or manual input) |
| **Operator** | Comparison operator |
| **Value** | Threshold count (1 - 2,147,483,647) |

#### Operators

| Operator | Symbol | Description |
|----------|--------|-------------|
| Less than | `<` | True when item count < value |
| Less than or equal | `<=` | True when item count <= value |
| Greater than | `>` | True when item count > value |
| Greater than or equal | `>=` | True when item count >= value |
| Equal | `=` | True when item count = value |
| Not equal | `!=` | True when item count != value |

### Examples

#### Example 1: Maintain minimum stock
> Keep at least 1000 Iron Ingots in the system

- Target: Iron Ingot
- Batch Size: 64
- Conditions:
  - Iron Ingot `<` 1000

#### Example 2: Craft with resource limit
> Craft up to 3000 Glass, but only when Sand is abundant

- Target: Glass
- Batch Size: 64
- Conditions:
  - Glass `<` 3000
  - Sand `>=` 1000

#### Example 3: Complex production chain
> Craft Torches when Coal is plentiful, Sticks are available, and we need more Torches

- Target: Torch
- Batch Size: 64
- Conditions:
  - Torch `<` 500
  - Coal `>` 2000
  - Stick `>=` 100

#### Example 4: Overflow crafting
> Convert excess Cobblestone to Stone when storage is filling up

- Target: Stone
- Batch Size: 256
- Conditions:
  - Cobblestone `>` 10000

### GUI Features

**Style**: Match AE2's terminal aesthetic (color scheme, fonts, UI patterns)

**Access**: Right-click block to open GUI

#### Main Screen
- List of all configured crafting rules (shows custom name or target item)
- **Up/down arrow buttons** to reorder rules (Mekanism-style)
- Enable/disable toggle per rule
- Add / Edit / Delete / **Duplicate** rules
- Visual status indicator per rule (idle, crafting, conditions not met, error)
- Quick view of target item and condition count

#### Rule Editor
- **Rule Name**: Optional text field for custom label
- **Target Item**: Ghost slot with JEI/EMI drag support
- **Batch Size**: Input field with validation (default: 64)
- **Conditions List**:
  - Add condition button
  - Each condition row:
    - Item ghost slot (JEI/EMI drag support)
    - Operator dropdown (`<`, `<=`, `>`, `>=`, `=`, `!=`)
    - Value input field
    - Up/down arrow buttons to reorder
    - Remove condition button
- Save / Cancel buttons

#### Item Selection
- **Ghost slots**: Click to open item picker, or drag directly from JEI/EMI
- **All Minecraft items** available in picker (not limited to ME network contents)
- Shows item icon and name after selection
- **Warning indicator** if selected item has no pattern in the ME network

#### Audio
- **Silent operation**: No sound effects, visual feedback only

#### Status Display
- Real-time condition evaluation (shows which conditions pass/fail)
- Current item counts for all monitored items
- Active crafting jobs
- Last triggered timestamp
- Error messages (missing pattern, insufficient resources, etc.)

## Technical Requirements

### Block Design
- **New standalone block** that connects to the ME network
- Supports **multiple rules per block** (configurable max, default: 16)
- Requires 1 channel
- **Visual style**: AE2 machine aesthetic (similar to ME Interface / Pattern Provider)
- **Colored status light** for in-world feedback:
  - Green: Active (conditions met, crafting or ready)
  - Yellow: Warning (missing pattern, persistent failure)
  - Red: Error (no CPU available)
  - Off: Idle (no rules enabled or conditions not met)

### Crafting Recipe

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

**Total cost**: 2x Engineering Processor, 2x Calculation Processor, 4x Logic Processor, 1x Pattern Provider

### ME Network Integration
- Connect to ME network via cable
- Read access to network storage counts
- Submit crafting requests via AE2's native crafting system
- **Requires AE2 Crafting CPU** - show warning in GUI if no CPU available

### Error Handling

| Error Type | Behavior |
|------------|----------|
| **Missing Pattern** | Show warning in GUI, skip to next rule, retry later |
| **Insufficient Resources** | Attempt to reduce batch size; if not possible, skip to next rule, retry later |
| **No Crafting CPU** | Show warning in GUI, do not submit job, retry later |
| **Crafting CPU Busy** | Skip to next rule, retry later |

### Performance
- Configurable tick rate for condition checking (default: 20 ticks / 1 second)
- Efficient item count queries (batch queries, caching)
- Maximum rules per block (configurable, default: 16)
- Maximum conditions per rule (configurable, default: 8)

### Persistence
- Rules and conditions saved to block entity NBT
- Survive chunk unload/reload
- Survive server restart
- **Block breaking**: Rules saved in item NBT (like shulker boxes) - pick up and place elsewhere with rules intact

### Multiplayer
- **Open access**: Any player who can interact with the block can edit rules
- No ownership or permission system

### Network Disconnection
- If ME network connection is lost (cable broken, network offline):
  - Show "No Network" error state in GUI
  - Status light shows error (red)
  - Pause rule checking until reconnected
  - Resume normal operation when connection restored

## User Interface

### Tooltips
- Informative tooltips explaining each operator
- Tooltips on status indicators explaining current state

## Configuration (Server Config)

```toml
[autorequester]
# Maximum rules per autorequester block
maxRulesPerBlock = 16

# Maximum conditions per rule
maxConditionsPerRule = 8

# Tick interval for checking conditions (20 = 1 second)
checkInterval = 20

# Maximum batch size allowed
maxBatchSize = 10000

# Channel requirement
requiresChannel = true
```
