package ac.dylanisa.minecraftai.network;

import ac.dylanisa.minecraftai.MinecraftAI;
import ac.dylanisa.minecraftai.data.PlayerDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sent from client to server to request scratch pad content.
 */
public record ServerboundScratchPadRequestPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundScratchPadRequestPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("scratch_pad_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundScratchPadRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new ServerboundScratchPadRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundScratchPadRequestPacket packet, IPayloadContext context) {
        MinecraftAI.LOGGER.info("ServerboundScratchPadRequestPacket: Received request from client");
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftAI.LOGGER.info("ServerboundScratchPadRequestPacket: Processing for player {}",
                player.getName().getString());
            PlayerDataManager dataManager = MinecraftAI.getDataManager();

            if (dataManager == null) {
                MinecraftAI.LOGGER.error("DataManager is null, cannot load scratch pad");
                return;
            }

            try {
                Path scratchFile = dataManager.getPlayerDir(player.getUUID())
                    .resolve("scratch.md");

                String content = "";
                if (Files.exists(scratchFile)) {
                    content = Files.readString(scratchFile);
                    MinecraftAI.LOGGER.info("Loaded scratch pad for {}: {} chars",
                        player.getName().getString(), content.length());
                } else {
                    MinecraftAI.LOGGER.info("No scratch pad file for {}, sending empty",
                        player.getName().getString());
                }

                // Send content back to client
                PacketDistributor.sendToPlayer(player, new ClientboundScratchPadSyncPacket(content));
            } catch (Exception e) {
                MinecraftAI.LOGGER.error("Failed to load scratch pad: {}", e.getMessage(), e);
            }
        });
    }
}
