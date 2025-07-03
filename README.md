# Dry Rate Tracker Plugin

A RuneLite plugin that tracks your dry streaks for OSRS raids (Theatre of Blood, Tombs of Amascut, and Chambers of Xeric) using advanced in-game object detection.

## Features

- **Accurate Object Detection**: Uses actual in-game objects (chests, sarcophagus, lights) and varbits for 100% reliable detection
- **No Chat Dependencies**: Works without relying on chat messages, making it more reliable and language-independent
- **Multiple Raid Support**: Tracks ToB, ToA, and CoX with raid-specific detection methods
- **Team vs Personal Drops**: Distinguishes between personal and team unique drops
- **Comprehensive Statistics**: Shows current dry streak, total completions, unique drops, longest streak, and average streak
- **Data Persistence**: Your data is saved and persists between game sessions
- **Clean UI**: Sidebar panel with easy-to-read statistics for each raid
- **Manual Reset**: Reset individual dry streaks with confirmation dialogs
- **Flexible Configuration**: Choose whether team drops reset your personal streak

## How It Works

The plugin uses sophisticated detection methods specific to each raid type:

### Detection Methods

**Theatre of Blood:**
- Monitors loot chest object spawning (IDs: 32990-32993)
- Distinguishes between regular and purple (unique) chests
- Detects both personal and team drops
- Uses region ID 12613 to know when you're in ToB

**Tombs of Amascut:**
- Monitors sarcophagus wall object spawning (ID: 46221)
- Uses varbit 14373 to determine if sarcophagus contains unique loot
- Uses varbits 14356-14372 to determine if drop is personal vs team
- Uses region ID 14160 to know when you're in ToA

**Chambers of Xeric:**
- Monitors light object spawning (ID: 28848)
- Uses varbit 5456 to determine loot type (1=standard, 2=unique, 3=dust, 4=kit)
- Uses region ID 12889 to know when you're in CoX

### Raid Completion Detection
- Automatically detects when you complete a raid based on loot object spawning
- No longer depends on chat messages like "Your completed [Raid] count is: X"
- Works regardless of game language or chat settings

### Unique Drop Detection
The plugin automatically detects when unique drops are received:

**Theatre of Blood:**
- Scythe of vitur, Ghrazi rapier, Sanguinesti staff
- Justiciar faceguard, Justiciar chestguard, Justiciar legguards
- Avernic defender hilt, Sanguine ornament kit, Holy ornament kit, Sanguine dust

**Tombs of Amascut:**
- Tumeken's shadow, Elidinis' ward
- Masori mask, Masori body, Masori chaps
- Lightbearer, Osmumten's fang

**Chambers of Xeric:**
- Twisted bow, Elder maul, Kodai insignia
- Dragon hunter crossbow, Dinhs bulwark
- Ancestral hat, Ancestral robe top, Ancestral robe bottom
- Dragon claws, Twisted buckler, Twisted ancestral colour kit, Metamorphic dust

## Usage

1. Install and enable the plugin
2. Complete raids as normal - the plugin will automatically detect completions and drops
3. Check your dry streaks in the sidebar panel
4. Configure whether team drops reset your personal streak in settings
5. Use the reset buttons if you need to manually reset a counter

## Configuration

The plugin includes several configuration options:
- **Enable/disable tracking** for individual raids (ToB, ToA, CoX)
- **Show notifications** for dry streaks and unique drops
- **Team drops reset streaks**: Choose whether team member drops reset your personal dry streak
- **Confirmation dialogs** for manual resets

## Building

```bash
./gradlew build
```

## Installation

1. Build the plugin using the command above
2. The built JAR will be in `build/libs/`
3. Install through RuneLite's plugin hub or load as an external plugin

---

## Changelog

### v2.0.0 - Object Detection Rewrite
**🚀 Major Update: Complete detection system overhaul**

#### ✨ New Features
- **Object-based detection**: Complete rewrite from chat message parsing to actual in-game object detection
- **Team vs Personal drops**: Plugin now distinguishes between personal and team unique drops
- **Region-based raid detection**: Uses region IDs to automatically detect when you enter/leave raids
- **Raid-specific detection methods**: Each raid type uses its own optimized detection system
- **Team drop configuration**: New setting to choose whether team drops reset your personal streak

#### 🔧 Technical Improvements
- **100% reliable detection**: No longer depends on chat messages or game language
- **Varbit integration**: Uses game state varbits for accurate unique drop detection
- **Enhanced logging**: Comprehensive debug logging for troubleshooting
- **State management**: Improved tracking of raid state and chest handling

#### 🏗️ Detection Method Changes
- **ToB**: Now detects loot chest objects (32990-32993) instead of chat messages
- **ToA**: Now detects sarcophagus wall objects (46221) + varbits (14373) instead of chat messages  
- **CoX**: Now detects light objects (28848) + varbits (5456) instead of chat messages

#### 🐛 Fixes
- Fixed false positives from chat message parsing
- Fixed missed detections due to chat spam or language differences
- Improved accuracy of unique drop detection
- Better handling of team scenarios

#### ⚠️ Breaking Changes
- No longer requires specific chat messages to function
- Detection behavior may be more accurate, potentially showing different dry streak numbers
- Team drop handling now configurable (previously always counted team drops)