package fiveavian.dcintegration.mixin;

import fiveavian.dcintegration.api.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(value = MinecraftServer.class, remap = false)
public class MinecraftServerMixin {
    @Inject(method = "startServer", at = @At("TAIL"))
    public void startServer(CallbackInfoReturnable<Boolean> cir) {
        for (Consumer<MinecraftServer> listener : ServerEvents.START) {
            listener.accept((MinecraftServer) (Object) this);
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    public void stopServer(CallbackInfo ci) {
        for (Consumer<MinecraftServer> listener : ServerEvents.STOP) {
            listener.accept((MinecraftServer) (Object) this);
        }
    }
}
