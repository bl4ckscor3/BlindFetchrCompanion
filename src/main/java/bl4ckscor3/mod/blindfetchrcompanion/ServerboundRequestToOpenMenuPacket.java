package bl4ckscor3.mod.blindfetchrcompanion;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundRequestToOpenMenuPacket() implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestToOpenMenuPacket> STREAM_CODEC = StreamCodec.unit(new ServerboundRequestToOpenMenuPacket());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return BlindFetchrCompanion.REQUEST_TO_OPEN_MENU_MESSAGE;
	}
}
