package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class ItemState {
	//@formatter:off
	public static final Codec<ItemState> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					ItemStack.CODEC.fieldOf("stack").forGetter(ItemState::getStack),
					Codec.BOOL.fieldOf("checked").forGetter(ItemState::isChecked))
			.apply(instance, ItemState::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemState> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, ItemState::getStack,
			ByteBufCodecs.BOOL, ItemState::isChecked,
			ItemState::new);
	//@formatter:on
	public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemState>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
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
}