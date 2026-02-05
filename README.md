# Git Branch Border

PhpStorm/IntelliJ plugin that displays a colored border around the IDE window based on the current Git branch.

## Features

- Visual indication of current branch via colored window border
- Configurable colors per branch (exact match, prefix, regex)
- Optional blinking border for critical branches (e.g. production)
- Hot-reload - changes to config are applied immediately
- Supports multiple projects with different configurations

## Configuration

Create `.branch-colors.json` in your project root:

```json
{
  "borderWidth": 4,
  "defaultColor": "#FF6600",
  "branches": [
    { "pattern": "master", "match": "exact", "color": "#FF0000" },
    { "pattern": "main", "match": "exact", "color": "#FF0000" },
    { "pattern": "production", "match": "exact", "color": "#FF0000",
      "blinking": { "enabled": true, "interval": 300 } },
    { "pattern": "develop", "match": "exact", "color": "#00FF00" },
    { "pattern": "feature/", "match": "prefix", "color": "#9933FF" },
    { "pattern": "hotfix/", "match": "prefix", "color": "#FF0000" },
    { "pattern": "release/.*", "match": "regex", "color": "#FFCC00" }
  ]
}
```

### Match types

- `exact` - Branch name must match exactly
- `prefix` - Branch name must start with pattern
- `regex` - Branch name must match regular expression

### Options

- `borderWidth` - Border thickness in pixels (1-20)
- `defaultColor` - Color for branches not matching any rule (optional)
- `blinking` - Global blinking settings (optional, see below)
- `branches` - Array of rules (evaluated in order, first match wins)

### Blinking

Make the border blink to draw attention to critical branches:

```json
{
  "blinking": { "enabled": true, "interval": 500 }
}
```

- `enabled` - Enable/disable blinking (default: `false`)
- `interval` - Blink interval in milliseconds (100-5000, default: `500`)

Blinking can be set globally (applies to all branches) or per-branch (overrides global setting).

## Installation

Available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/XXXXX-git-branch-border)

Or install manually: Settings → Plugins → ⚙️ → Install Plugin from Disk

## License

MIT
