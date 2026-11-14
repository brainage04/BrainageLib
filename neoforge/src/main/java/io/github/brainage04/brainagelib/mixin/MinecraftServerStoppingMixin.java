package io.github.brainage04.brainagelib.mixin;

import io.github.brainage04.brainagelib.neoforge.NeoForgeServerPlatform;
import io.github.brainage04.brainagelib.platform.ServerPlatform;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerStoppingMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void brainagelib$runStoppingListeners(CallbackInfo callback) {
        ((NeoForgeServerPlatform) ServerPlatform.get())
                .beforeStopServer((MinecraftServer) (Object) this);
    }
}
