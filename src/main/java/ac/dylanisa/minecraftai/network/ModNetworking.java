package ac.dylanisa.minecraftai.network;

import ac.dylanisa.minecraftai.MinecraftAI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all network packets for the mod.
 */
@EventBusSubscriber(modid = MinecraftAI.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MinecraftAI.MOD_ID);

        // Client -> Server packets
        registrar.playToServer(
            ServerboundChatPacket.TYPE,
            ServerboundChatPacket.STREAM_CODEC,
            ServerboundChatPacket::handle
        );

        registrar.playToServer(
            ServerboundNewConversationPacket.TYPE,
            ServerboundNewConversationPacket.STREAM_CODEC,
            ServerboundNewConversationPacket::handle
        );

        registrar.playToServer(
            ServerboundResumeConversationPacket.TYPE,
            ServerboundResumeConversationPacket.STREAM_CODEC,
            ServerboundResumeConversationPacket::handle
        );

        registrar.playToServer(
            ServerboundScratchPadUpdatePacket.TYPE,
            ServerboundScratchPadUpdatePacket.STREAM_CODEC,
            ServerboundScratchPadUpdatePacket::handle
        );

        registrar.playToServer(
            ServerboundScratchPadRequestPacket.TYPE,
            ServerboundScratchPadRequestPacket.STREAM_CODEC,
            ServerboundScratchPadRequestPacket::handle
        );

        registrar.playToServer(
            ServerboundRequestConversationsPacket.TYPE,
            ServerboundRequestConversationsPacket.STREAM_CODEC,
            ServerboundRequestConversationsPacket::handle
        );

        registrar.playToServer(
            ServerboundDeleteConversationPacket.TYPE,
            ServerboundDeleteConversationPacket.STREAM_CODEC,
            ServerboundDeleteConversationPacket::handle
        );

        // Server -> Client packets
        registrar.playToClient(
            ClientboundChatChunkPacket.TYPE,
            ClientboundChatChunkPacket.STREAM_CODEC,
            ClientboundChatChunkPacket::handle
        );

        registrar.playToClient(
            ClientboundChatCompletePacket.TYPE,
            ClientboundChatCompletePacket.STREAM_CODEC,
            ClientboundChatCompletePacket::handle
        );

        registrar.playToClient(
            ClientboundScratchPadSyncPacket.TYPE,
            ClientboundScratchPadSyncPacket.STREAM_CODEC,
            ClientboundScratchPadSyncPacket::handle
        );

        registrar.playToClient(
            ClientboundConversationListPacket.TYPE,
            ClientboundConversationListPacket.STREAM_CODEC,
            ClientboundConversationListPacket::handle
        );

        registrar.playToClient(
            ClientboundMessageHistoryPacket.TYPE,
            ClientboundMessageHistoryPacket.STREAM_CODEC,
            ClientboundMessageHistoryPacket::handle
        );

        MinecraftAI.LOGGER.info("Registered Minecraft AI network packets");
    }

    /**
     * Helper to create a ResourceLocation for packet types.
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MinecraftAI.MOD_ID, path);
    }
}
