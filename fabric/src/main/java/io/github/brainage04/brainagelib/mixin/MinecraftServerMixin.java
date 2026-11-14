package io.github.brainage04.brainagelib.mixin;

import io.github.brainage04.brainagelib.server.CurrentServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void brainagelib$becomeCurrent(CallbackInfo callback) {
        CurrentServer.serverThreadStarted((MinecraftServer) (Object) this);
    }

    @Inject(method = "runServer", at = @At("RETURN"))
    private void brainagelib$stopBeingCurrent(CallbackInfo callback) {
        CurrentServer.serverThreadEnded((MinecraftServer) (Object) this);
    }
}
