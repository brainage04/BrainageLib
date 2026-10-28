package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// A charged respawn anchor used where anchors do not work explodes with no entity and a
/// position-only damage source, like a bed ({@link AbstractBedBlockMixin}); the click is credited
/// around the call the same way.
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {
    @WrapOperation(
            method = "useWithoutItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/RespawnAnchorBlock;explode(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"))
    private void brainagelib$creditClicker(
            RespawnAnchorBlock anchor,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Operation<Void> original,
            @Local(argsOnly = true) Player player) {
        if (!(player instanceof ServerPlayer clicker)) {
            original.call(anchor, state, level, pos);
            return;
        }
        DropCapture.credited(clicker, () -> original.call(anchor, state, level, pos));
    }
}
