# ExtractTitles

ExtractTitles is a title management plugin for Paper 1.21.x (compatible with Purpur, designed with Folia-safe architecture in mind).

## Features
- Buy titles using Vault or PlayerPoints
- Permanent and temporary ownership
- Activate and deactivate titles
- Full GUI menu (`/titles`)
- PlaceholderAPI expansion
- Active title effects:
  - `max_health`
  - `potion`
  - `permission`
  - `command` (not executed on join/reapply)
  - `mining_3x3`
- Storage: SQLite or MySQL
- Fully configurable messages, menu, and title definitions

## Requirements
- Java 21
- Paper 1.21.x
- Recommended dependencies:
  - PlaceholderAPI
  - Vault (if using Vault economy)
  - PlayerPoints (if using PlayerPoints currency)

## Installation
1. Put `ExtractTitles.jar` in your `plugins` folder.
2. (Optional) Install `PlaceholderAPI`, `Vault`, and `PlayerPoints`.
3. Start the server once.
4. Configure files in `plugins/ExtractTitles/`.
5. Run `/extracttitles reload` or restart the server.

## Build From Source
```bash
gradle clean build
```
Built jar:
- `build/libs/ExtractTitles-1.0.0.jar`

## Commands
### Player
- `/titles` - open menu
- `/extracttitles menu` - open menu
- `/extracttitles buy <id>`
- `/extracttitles info <id>`
- `/extracttitles activate <id>`
- `/extracttitles deactivate`

### Admin
- `/extracttitles list`
- `/extracttitles reload`
- `/extracttitles give <player> <id>`
- `/extracttitles remove <player> <id>`
- `/extracttitles granttemp <player> <id> <1d>`
- `/extracttitles clearactive <player>`
- `/extracttitles debug [player]`

## Permissions
- `extracttitles.use`
- `extracttitles.admin.list`
- `extracttitles.admin.edit`
- `extracttitles.admin.give`
- `extracttitles.admin.remove`
- `extracttitles.admin.granttemp`
- `extracttitles.admin.clearactive`
- `extracttitles.admin.debug`
- `extracttitles.admin.buybypass`

## PlaceholderAPI
- `%extracttitles_active_title%`
- `%extracttitles_active_title_name%`
- `%extracttitles_active_title_raw%`
- `%extracttitles_owned_count%`
- `%extracttitles_total_count%`
- `%extracttitles_active_title_or_none%`
- `%extracttitles_active_title_or_blank%`
- `%extracttitles_has_title_<id>%`
- `%extracttitles_title_expiry_<id>%`
- `%extracttitles_title_remaining_<id>%`

Examples:
- `%extracttitles_has_title_warrior%`
- `%extracttitles_title_remaining_miner%`

## Config Files
Folder: `plugins/ExtractTitles/`
- `config.yml` - global plugin settings
- `storage.yml` - SQLite/MySQL configuration
- `messages.yml` - all plugin messages
- `menus.yml` - GUI layout and card formatting
- `titles.yml` - title definitions and purchase settings
- `effects.yml` - reusable effect templates

## Encoding
Edit all `.yml` files in UTF-8 without BOM.

## Support
If something is not working:
1. Verify Java 21
2. Verify YAML encoding (UTF-8 without BOM)
3. Verify dependencies are installed
4. Run `/extracttitles debug`
