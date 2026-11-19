package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import io.github.brainage04.brainagelib.drops.SupportTicks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void brainagelib$captureDrop(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        boolean captured =
                entity instanceof ItemEntity itemEntity
                        ? DropCapture.capture(level, itemEntity)
                        : entity instanceof ExperienceOrb experienceOrb
                                && DropCapture.captureExperience(level, experienceOrb.getValue());
        if (captured) {
            callback.setReturnValue(false);
        }
    }

    /// A block tick whose scheduling a player's break caused runs inside that player's scope, so
    /// the blocks that break a tick after their support goes ({@link SupportTicks}) pay the player
    /// who took the support away. The scope is a break scope, so the next block's tick, scheduled
    /// by this one, is theirs too: a whole column or chorus tree carries the player up.
    @WrapMethod(method = "tickBlock")
    private void brainagelib$captureSupportTickDrops(
            BlockPos pos, Block type, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        ServerPlayer player = SupportTicks.owner(level, pos, type);
        if (player == null) {
            original.call(pos, type);
            return;
        }
        SupportTicks.enter(player);
        try {
            original.call(pos, type);
        } finally {
            SupportTicks.exit();
        }
    }
}
