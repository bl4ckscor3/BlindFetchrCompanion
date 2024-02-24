# "Blind Fetchr"-Companion

A companion mod for [NeunEinser's Fetchr](https://github.com/NeunEinser/bingo/), adding a UI for use in blind mode to keep track which items were already tried. With team synchronization support.

## Usage

- This mod is made for Fabric and requires [Fabric API](https://modrinth.com/mod/fabric-api) as well as [Cloth Config](https://modrinth.com/mod/cloth-config).
- Make sure that the mod is installed on the server and all clients for best results. More specifically:
	- If the server has the mod installed, clients that don't have the mod installed won't be able to use the checklist.
	- If the server does **not** have the mod installed, clients that *do* have it installed won't be able to open the checklist.
- The mod is intended for use in Fetchr's blind mode, however it works in any mode.
- The item checklist will only show up for players who are part of a team. If a player is not playing the game (aka they're not in a team), the checklist will be empty.
- To open the item checklist, press `C` by default. This can be changed in the key binds settings.
- Simply click an item to mark it as checked. This will immediately synchronize to all players on the same team unless they have the checklist open as well.
- After the game is over, the checklists need to be manually reset before the next game starts. To reset every team's checklist, run the command `/blindfetchrcompanion_reset`. This is only available to OPs.
	- The checklists will also reset on datapack reload (`/reload`).
- If the singleplayer world/multiplayer server is shut down at any point during or after the game, all teams' checklists will be preserved.

### Troubleshooting

- If a team that is in-game does not have any items show up in their checklist, an operator can run the command `/blindfetchrcompanion_reset <team>`, replacing `<team>` with the team whose checklist is empty. This will reset only that team's checklist, and should fix the issue.
- Have a question, suggestion, or found a bug? Open an issue!