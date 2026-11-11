package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// Covers the hanging decorations a player breaks: item frames (their own item and the item they
/// hold), glow item frames, paintings and cushions, which all drop through `dropItem`. The other
/// callers of `dropItem` pass no cause (a decoration that lost its support, or one a piston moved
/// away), and a blast names the player only when they caused it.
@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {
    @WrapOperation(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    private void brainagelib$captureBrokenDecoration(
            BlockAttachedEntity decoration,
            ServerLevel level,
            @Nullable Entity causedBy,
            Operation<Void> original) {
        if (!(causedBy instanceof ServerPlayer player)) {
            original.call(decoration, level, causedBy);
            return;
        }
        DropCapture.begin(player);
        try {
            original.call(decoration, level, causedBy);
        } finally {
            DropCapture.end();
        }
    }
}
