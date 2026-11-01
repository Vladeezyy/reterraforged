package raccoonman.reterraforged.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import raccoonman.reterraforged.RTFCommon;

/** Server to client on join: the world's altitude cooling scale (AltitudeCooling) */
public record AltitudeCoolingPayload(float scale) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AltitudeCoolingPayload> TYPE = new CustomPacketPayload.Type<>(RTFCommon.location("altitude_cooling"));
	public static final StreamCodec<FriendlyByteBuf, AltitudeCoolingPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, AltitudeCoolingPayload::scale, AltitudeCoolingPayload::new);

	@Override
	public CustomPacketPayload.Type<AltitudeCoolingPayload> type() {
		return TYPE;
	}
}
