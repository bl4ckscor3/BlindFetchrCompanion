package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

public class ItemChecklistMenu extends AbstractContainerMenu {
	public static final int NUM_COLUMNS = 13;
	public static final int NUM_ROWS = 12;
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

			state.setChecked(shouldBeChecked);

			if (player.level().isClientSide)
				BlindFetchrCompanionClient.playSound(state.isChecked());
			else {
				String name = player.getName().getString();
				Team team = player.getScoreboard().getPlayersTeam(name);

				for (String teamMemberName : team.getPlayers()) {
					ServerPlayer teamMember = player.level().getServer().getPlayerList().getPlayerByName(teamMemberName);

					if (teamMember != null) {
						teamMember.sendSystemMessage(Component.translatable("%s %s [%s]", team.getColor() + name + ChatFormatting.RESET, shouldBeChecked ? "checked off" : "unchecked", Component.translatable(state.getStack().getDescriptionId())));
						ServerPlayNetworking.send(teamMember, BlindFetchrCompanion.UPDATE_ITEM_STATE, PacketByteBufs.create().writeVarInt(slot).writeBoolean(shouldBeChecked));
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
