package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import io.github.brainage04.brainagelib.drops.LeafOwners;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin {
    /// A block broken for a player outside `ServerPlayerGameMode.destroyBlock`: a mod that calls
    /// `playerDestroy` itself, or breaks blocks with `Level.destroyBlock` naming the player. Inside
    /// a block break this is the same player's scope again.
    @WrapMethod(
            method =
                    "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;"
                            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V")
    private static void brainagelib$captureBreakerDrops(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemStack tool,
            Operation<Void> original) {
        if (!(breaker instanceof ServerPlayer player)) {
            original.call(state, level, pos, blockEntity, breaker, tool);
            return;
        }
        DropCapture.begin(player);
        try {
            original.call(state, level, pos, blockEntity, breaker, tool);
        } finally {
            DropCapture.end();
        }
    }

    /// The entity-less overload a leaf decays through: a leaf of a tree a player felled ({@link
    /// LeafOwners}) drops for that player.
    @WrapMethod(
            method =
                    "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                            + "Lnet/minecraft/core/BlockPos;)V")
    private static void brainagelib$captureFelledLeafDrops(
            BlockState state, Level level, BlockPos pos, Operation<Void> original) {
        ServerPlayer feller =
                state.is(BlockTags.LEAVES) && level instanceof ServerLevel server
                        ? LeafOwners.owner(server, pos)
                        : null;
        if (feller == null) {
            original.call(state, level, pos);
            return;
        }
        DropCapture.begin(feller);
        try {
            original.call(state, level, pos);
        } finally {
            DropCapture.end();
        }
    }
}
