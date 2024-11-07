package bl4ckscor3.mod.blindfetchrcompanion.checklist;

import java.util.List;

import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanion;
import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanionClient;
import bl4ckscor3.mod.blindfetchrcompanion.network.ClientboundUpdateItemStatePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

public class ItemChecklistMenu extends AbstractContainerMenu {
	public static final int NUM_COLUMNS = 13;
	public static final int NUM_ROWS = 13;
	protected final List<ItemState> itemStates;

	public ItemChecklistMenu(int id, List<ItemState> itemStates) {
		super(BlindFetchrCompanion.CHECKLIST_MENU_TYPE, id);
		this.itemStates = itemStates;

		Container container = new SimpleContainer(itemStates.stream().map(ItemState::getStack).toArray(ItemStack[]::new));

		for (int y = 0; y < NUM_ROWS; y++) {
			for (int x = 0; x < NUM_COLUMNS; x++) {
				int index = y * NUM_COLUMNS + x;

				if (index < itemStates.size())
					addSlot(new Slot(container, index, 8 + x * 18, 17 + y * 18));
			}
		}
	}

	@Override
	public void clicked(int slot, int mouseButton, ClickType clickType, Player player) {
		if (clickType == ClickType.PICKUP && slot >= 0 && slot < itemStates.size()) {
			ItemState state = itemStates.get(slot);
			boolean shouldBeChecked = !state.isChecked();
			Level level = player.level();

			state.setChecked(shouldBeChecked);

			if (level.isClientSide)
				BlindFetchrCompanionClient.playSound(state.isChecked());
			else if (level.getServer() instanceof DedicatedServer server) {
				String name = player.getName().getString();
				Team team = player.getScoreboard().getPlayersTeam(name);
				PlayerList playerList = server.getPlayerList();

				for (String teamMemberName : team.getPlayers()) {
					ServerPlayer teamMember = playerList.getPlayerByName(teamMemberName);

					if (teamMember != null) {
						teamMember.sendSystemMessage(Component.translatable("blindfetchrcompanion." + (shouldBeChecked ? "item_checked" : "item_unchecked"), team.getColor() + name + ChatFormatting.RESET, state.getStack().getItemName()));

						if (teamMember != player)
							ServerPlayNetworking.send(teamMember, new ClientboundUpdateItemStatePacket(slot, shouldBeChecked));
					}
				}
			}
		}
	}

	public void updateState(int slot, boolean newState) {
		if (slot >= 0 && slot < itemStates.size())
			itemStates.get(slot).setChecked(newState);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		if (!player.level().isClientSide)
			BlindFetchrCompanion.setItemChecklistsDirty();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
