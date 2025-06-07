# Dry Rate Tracker Plugin

A RuneLite plugin that tracks your dry streaks for OSRS raids (Theatre of Blood, Tombs of Amascut, and Chambers of Xeric).

## Features

- **Real-time Tracking**: Automatically detects raid completions and unique drops through chat messages
- **Multiple Raid Support**: Tracks ToB, ToA, and CoX separately
- **Comprehensive Statistics**: Shows current dry streak, total completions, unique drops, longest streak, and average streak
- **Data Persistence**: Your data is saved and persists between game sessions
- **Clean UI**: Sidebar panel with easy-to-read statistics for each raid
- **Manual Reset**: Reset individual dry streaks with confirmation dialogs

## How It Works

The plugin monitors your chat messages for:

### Raid Completions
- Theatre of Blood: "Your completed Theatre of Blood count is: X"
- Tombs of Amascut: "Your completed Tombs of Amascut count is: X"  
- Chambers of Xeric: "Your completed Chambers of Xeric count is: X"

### Unique Drops
**Theatre of Blood:**
- Scythe of vitur, Ghrazi rapier, Sanguinesti staff
- Justiciar faceguard, Justiciar chestguard, Justiciar legguards
- Avernic defender hilt

**Tombs of Amascut:**
- Tumeken's shadow, Elidinis' ward
- Masori mask, Masori body, Masori chaps
- Lightbearer, Osmumten's fang

**Chambers of Xeric:**
- Twisted bow, Elder maul, Kodai insignia
- Dragon hunter crossbow, Dinhs bulwark
- Ancestral hat, Ancestral robe top, Ancestral robe bottom
- Dragon claws, Twisted buckler

## Usage

1. Install and enable the plugin
2. Complete raids as normal - the plugin will automatically track your progress
3. Check your dry streaks in the sidebar panel
4. Use the reset buttons if you need to manually reset a counter

## Configuration

The plugin includes several configuration options:
- Enable/disable tracking for individual raids
- Show notifications for dry streaks and unique drops
- Confirmation dialogs for resets

## Building

```bash
./gradlew build
```

## Installation

1. Build the plugin using the command above
2. The built JAR will be in `build/libs/`
3. Install through RuneLite's plugin hub or load as an external plugin