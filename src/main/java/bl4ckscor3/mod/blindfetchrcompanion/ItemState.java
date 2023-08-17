package bl4ckscor3.mod.blindfetchrcompanion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class ItemState {
	private final ItemStack stack;
	private boolean checked;

	public ItemState(ItemStack stack, boolean checked) {
		this.stack = stack;
		this.checked = checked;
	}

	public ItemStack getStack() {
		return stack;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public CompoundTag save(CompoundTag tag) {
		tag.put("stack", stack.save(new CompoundTag()));
		tag.putBoolean("checked", checked);
		return tag;
	}

	public static ItemState load(CompoundTag tag) {
		return new ItemState(ItemStack.of(tag.getCompound("stack")), tag.getBoolean("checked"));
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeItem(stack);
		buf.writeBoolean(checked);
	}

	public static ItemState read(FriendlyByteBuf buf) {
		return new ItemState(buf.readItem(), buf.readBoolean());
	}
}