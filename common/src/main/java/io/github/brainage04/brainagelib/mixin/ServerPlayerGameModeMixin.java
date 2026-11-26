package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.LeafOwners;
import io.github.brainage04.brainagelib.drops.SupportTicks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/// A player's block break: its drops go to the player inside a capture scope, a log they break
/// hands the leaves it held up to them ({@link LeafOwners}), and the blocks that break a tick after
/// their support goes pay them too ({@link SupportTicks}), whatever their mods' switches say.
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean brainagelib$captureBlockDrops(BlockPos pos, Operation<Boolean> original) {
        ServerLevel level = (ServerLevel) this.player.level();
        BlockState state = level.getBlockState(pos);
        boolean broken;
        SupportTicks.enter(this.player);
        try {
            broken = original.call(pos);
        } finally {
            SupportTicks.exit();
        }
        if (broken && state.is(BlockTags.LOGS)) {
            LeafOwners.felled(this.player, level, List.of(pos.immutable()));
        }
        return broken;
    }
}
