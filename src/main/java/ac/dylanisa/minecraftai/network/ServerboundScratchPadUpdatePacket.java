package ac.dylanisa.minecraftai.network;

import ac.dylanisa.minecraftai.MinecraftAI;
import ac.dylanisa.minecraftai.data.PlayerDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sent from client to server when player edits the scratch pad.
 */
public record ServerboundScratchPadUpdatePacket(String content) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundScratchPadUpdatePacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("scratch_pad_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundScratchPadUpdatePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundScratchPadUpdatePacket::content,
            ServerboundScratchPadUpdatePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundScratchPadUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            PlayerDataManager dataManager = MinecraftAI.getDataManager();

            if (dataManager == null) {
                MinecraftAI.LOGGER.error("DataManager is null, cannot save scratch pad");
                return;
            }

            try {
                Path scratchFile = dataManager.getPlayerDir(player.getUUID())
                    .resolve("scratch.md");
                MinecraftAI.LOGGER.info("Saving scratch pad to: {} ({} chars)",
                    scratchFile, packet.content().length());
                Files.writeString(scratchFile, packet.content());
                MinecraftAI.LOGGER.info("Scratch pad saved successfully for {}",
                    player.getName().getString());
            } catch (Exception e) {
                MinecraftAI.LOGGER.error("Failed to save scratch pad: {}", e.getMessage(), e);
            }
        });
    }
}
