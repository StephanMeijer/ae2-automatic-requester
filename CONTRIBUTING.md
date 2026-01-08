# Contributing

Thank you for your interest in contributing to AE2 Autorequester!

## Commit Messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for commit messages. This enables automatic changelog generation and semantic versioning.

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Changes that do not affect the meaning of the code (formatting, etc.) |
| `refactor` | A code change that neither fixes a bug nor adds a feature |
| `perf` | A code change that improves performance |
| `test` | Adding missing tests or correcting existing tests |
| `build` | Changes that affect the build system or external dependencies |
| `ci` | Changes to CI configuration files and scripts |
| `chore` | Other changes that don't modify src or test files |

### Scopes (optional)

- `gui` - GUI/screen related changes
- `block` - Block/BlockEntity changes
- `network` - ME network integration
- `crafting` - Crafting logic
- `config` - Configuration changes
- `compat` - Compatibility with other mods (JEI, EMI, Jade, etc.)
- `data` - Data classes (rules, conditions, etc.)

### Examples

```
feat(gui): add drag-drop support for item selection
fix(crafting): prevent duplicate crafting jobs for same item
docs: update README with installation instructions
refactor(block): extract common grid logic to base class
perf(network): cache item counts between ticks
build: update AE2 dependency to 19.2.10
chore: clean up unused imports
```

### Breaking Changes

For breaking changes, add `!` after the type/scope or include `BREAKING CHANGE:` in the footer:

```
feat(config)!: rename maxRules to rulesPerBlock

BREAKING CHANGE: Configuration key renamed, existing configs need update.
```

## Code Style

### DRY (Don't Repeat Yourself)

- **Interfaces for common behavior**: Extract shared functionality into interfaces
- **Generic classes**: Use generics to create reusable implementations
- **Helper methods**: Factor out repeated logic into private helper methods
- **Constants**: Define shared constants once and reference them

### No Magic Numbers

All UI dimensions, positions, and offsets must be defined as named constants at the top of the class. Never use hardcoded numbers directly in method bodies.

**Good:**
```java
private static final int PADDING = 8;
private static final int BUTTON_SIZE = 20;

addButton(leftPos + PADDING, bottomY, BUTTON_SIZE, BUTTON_SIZE);
```

**Bad:**
```java
addButton(leftPos + 8, bottomY, 20, 20);  // DON'T do this
```

### Event-Based architecture

Prefer event-based/pub-sub patterns over polling:
- Subscribe to ME network events rather than polling item counts every tick
- Only re-evaluate rules when relevant storage changes occur
- Use dirty flags and change notifications rather than constant syncing

### Data-Driven Over Control Flow

Prefer data-driven approaches over long if-else or switch chains. Extract varying parts into data structures.

## Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes following the coding guidelines
4. Ensure the build passes: `./gradlew build`
5. Submit a pull request with a clear description