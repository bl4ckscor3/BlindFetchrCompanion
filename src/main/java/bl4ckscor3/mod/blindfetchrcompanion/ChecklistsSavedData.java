package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;

public class ChecklistsSavedData extends SavedData {
	private final Map<PlayerTeam, List<ItemState>> checklists = new HashMap<>();
	private boolean firstLoad = true;

	public static ChecklistsSavedData load(MinecraftServer server, CompoundTag tag, HolderLookup.Provider lookupProvider) {
		ChecklistsSavedData checklistsSavedData = new ChecklistsSavedData();
		ListTag teams = tag.getList("teams", Tag.TAG_COMPOUND);
		var decoder = lookupProvider.createSerializationContext(NbtOps.INSTANCE).withDecoder(ItemState.CODEC);

		for (int i = 0; i < teams.size(); i++) {
			CompoundTag teamInfo = teams.getCompound(i);
			PlayerTeam team = server.getScoreboard().getPlayerTeam(teamInfo.getString("team"));

			if (team == null)
				continue;

			List<ItemState> states = new ArrayList<>();

			teamInfo.getList("states", Tag.TAG_COMPOUND).forEach(stateTag -> states.add(decoder.apply(stateTag).getOrThrow().getFirst()));
			checklistsSavedData.checklists.put(team, states);
		}

		checklistsSavedData.firstLoad = false;
		return checklistsSavedData;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		ListTag teams = new ListTag();
		var encoder = lookupProvider.createSerializationContext(NbtOps.INSTANCE).withEncoder(ItemState.CODEC);

		for (Entry<PlayerTeam, List<ItemState>> entry : checklists.entrySet()) {
			CompoundTag teamInfo = new CompoundTag();
			ListTag states = new ListTag();

			entry.getValue().forEach(state -> states.add(encoder.apply(state).getOrThrow()));
			teamInfo.putString("team", entry.getKey().getName());
			teamInfo.put("states", states);
			teams.add(teamInfo);
		}

		tag.put("teams", teams);
		return tag;
	}

	public List<ItemState> put(PlayerTeam team, List<ItemState> states) {
		return checklists.put(team, states);
	}

	public List<ItemState> getOrDefault(PlayerTeam team) {
		return checklists.getOrDefault(team, new ArrayList<>());
	}

	public void clear() {
		checklists.clear();
	}

	public boolean isFirstLoad() {
		return firstLoad;
	}
}
