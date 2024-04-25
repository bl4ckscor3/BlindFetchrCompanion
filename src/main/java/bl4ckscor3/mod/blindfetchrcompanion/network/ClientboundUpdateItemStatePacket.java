package bl4ckscor3.mod.blindfetchrcompanion.network;

import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundUpdateItemStatePacket(int slot, boolean newState) implements CustomPacketPayload {
	//@formatter:off
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateItemStatePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientboundUpdateItemStatePacket::slot,
			ByteBufCodecs.BOOL, ClientboundUpdateItemStatePacket::newState,
			ClientboundUpdateItemStatePacket::new);
	//@formatter:on

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return BlindFetchrCompanion.UPDATE_ITEM_STATE_MESSAGE;
	}
}
