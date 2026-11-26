package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.SupportTicks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public abstract class LevelMixin {
    /// A block destroyed with no breaker at a position whose tick a player's break scheduled: the
    /// blocks that break a tick after their support goes ({@link SupportTicks}) pay the player who
    /// took the support away. The scope covers the whole call, including the `setBlock` that
    /// schedules the next block's tick, so a whole column or chorus tree carries the player up.
    ///
    /// <p>Only a destroy that actually happens is paid: the record is read here, not at the tick,
    /// so a scheduled tick that falls or re-places its block instead of destroying it spends
    /// nothing.
    @WrapMethod(
            method =
                    "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z")
    private boolean brainagelib$captureSupportBreakDrops(
            BlockPos pos,
            boolean dropResources,
            @Nullable Entity breaker,
            int updateLimit,
            Operation<Boolean> original) {
        if (breaker != null || !dropResources || !((Object) this instanceof ServerLevel level)) {
            return original.call(pos, dropResources, breaker, updateLimit);
        }
        BlockState state = level.getBlockState(pos);
        ServerPlayer player = SupportTicks.owner(level, pos, state.getBlock());
        if (player == null) {
            return original.call(pos, dropResources, breaker, updateLimit);
        }
        SupportTicks.enter(player);
        try {
            return original.call(pos, dropResources, breaker, updateLimit);
        } finally {
            SupportTicks.exit();
        }
    }
}
