package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

			state.setChecked(!state.isChecked());

			if (player.level().isClientSide)
				BlindFetchrCompanionClient.playSound(state.isChecked());
		}
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
