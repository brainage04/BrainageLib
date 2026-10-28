package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// A bed used where beds explode passes no entity and a position-only damage source to its
/// explosion, so nothing in it names the player who clicked. The click is credited around the call
/// instead ({@link DropCapture#credited}), which leaves the explosion itself, its damage, knockback
/// and kill credit exactly the game's.
@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockMixin {
    @WrapOperation(
            method = "useWithoutItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/AbstractBedBlock;destroyOnUse(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult brainagelib$creditClicker(
            AbstractBedBlock bed,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            Operation<InteractionResult> original) {
        if (!(player instanceof ServerPlayer clicker)) {
            return original.call(bed, state, level, pos, player);
        }
        return DropCapture.credited(clicker, () -> original.call(bed, state, level, pos, player));
    }
}
