# Git Branch Border

PhpStorm/IntelliJ plugin that displays a colored border around the IDE window based on the current Git branch.

## Features

- Visual indication of current branch via colored window border
- Settings UI for easy configuration (Settings → Tools → Git Branch Border)
- Configurable colors per branch (exact match, prefix, regex)
- Solid or dashed border styles
- Optional blinking border for critical branches (e.g. production)
- Import from JSON for migration from older versions

## Configuration

Go to **Settings → Tools → Git Branch Border** to configure:

### General
- **Enable Git Branch Border** - Turn the plugin on/off
- **Border width** - Thickness in pixels (1-20)
- **Blink interval** - Blinking speed in milliseconds (100-5000)

### Default (branches without rule)
- **Color** - Default border color
- **Border style** - Solid or Dashed
- **Enable blinking** - Blink for unmatched branches
- **No border if unmatched** - Hide border when no rule matches

### Branch Rules
Add rules for specific branches:
- **Pattern** - Text to match against branch name
- **Match type** - `exact`, `prefix`, or `regex`
- **Color** - Border color for matching branches
- **Border style** - Solid, Dashed, or Default (inherit from settings)
- **Blinking** - Enable blinking for this rule

Rules are evaluated top to bottom. First match wins.

## Migration from v1.1.0

If you have an existing `.branch-colors.json` configuration:
1. Go to Settings → Tools → Git Branch Border
2. Click "Import from JSON..."
3. Select your `.branch-colors.json` file
4. Click Apply

## Installation

Available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/XXXXX-git-branch-border)

Or install manually: Settings → Plugins → ⚙️ → Install Plugin from Disk

## License

MIT
