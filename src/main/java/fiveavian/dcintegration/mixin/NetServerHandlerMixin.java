package fiveavian.dcintegration.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fiveavian.dcintegration.api.ServerEvents;
import net.minecraft.core.net.packet.Packet3Chat;
import net.minecraft.server.net.handler.NetServerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(value = NetServerHandler.class, remap = false)
public class NetServerHandlerMixin {
    @Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/entity/player/EntityPlayerMP;getDisplayName()Ljava/lang/String;"))
    private void handleChatMessage(Packet3Chat packet, CallbackInfo ci, @Local String message) {
        for (BiConsumer<NetServerHandler, String> listener : ServerEvents.CHAT_MESSAGE) {
            listener.accept((NetServerHandler) (Object) this, message);
        }
    }
}
