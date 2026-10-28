package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /// A player's death drops belong to nobody: the scope for nobody keeps any outer scope (a block
    /// break or kill that caused this death) from handing the inventory to another player.
    @WrapOperation(
            method = "die",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void brainagelib$keepDeathDropsInWorld(
            ServerPlayer player, ServerLevel level, DamageSource source, Operation<Void> original) {
        DropCapture.begin(null);
        try {
            original.call(player, level, source);
        } finally {
            DropCapture.end();
        }
    }
}
