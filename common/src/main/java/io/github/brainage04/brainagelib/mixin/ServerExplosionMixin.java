package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    /// Only the block step: mobs the explosion kills follow the mob rule, and the entities it
    /// breaks (item frames, armor stands, minecarts) drop where they were.
    @WrapOperation(
            method = "explode",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/ServerExplosion;interactWithBlocks(Ljava/util/List;)V"))
    private void brainagelib$captureBlockDrops(
            ServerExplosion explosion, List<BlockPos> positions, Operation<Void> original) {
        ServerPlayer cause = DropCapture.explosionCause(explosion);
        if (cause == null) {
            original.call(explosion, positions);
            return;
        }
        DropCapture.begin(cause);
        try {
            original.call(explosion, positions);
        } finally {
            DropCapture.end();
        }
    }
}
