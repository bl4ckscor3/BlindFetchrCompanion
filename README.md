# "Blind Fetchr"-Companion

A commpanion mod for [NeunEinser's Fetchr](https://github.com/NeunEinser/bingo/), adding a UI for use in blind mode to keep track which items were already tried. With team synchronization support.

## Usage

- Make sure that the mod is installed on the server and all clients:
	- Installing the mod on the server but not on the client will work.
	- Installing the mod on the client but not on the server will **not** work.
- This mod is intended for use in Fetchr's blind mode, however it works in any mode.
- To open the item checklist, press `C` by default. This can be changed in the key binds settings.
- Simply click an item to mark it as checked. This will immediately synchronize to all players on the same team unless they have the checklist open as well.
- To reset every team's checklist, run the command `/blindfetchrcompanion_reset`. This is only available to OPs.
	- The checklists will also reset on datapack reload (`/reload`) and when the server first starts.