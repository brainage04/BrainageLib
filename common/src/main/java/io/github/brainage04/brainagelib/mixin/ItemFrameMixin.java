package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// The punch that pops the item out of an item frame before the frame itself breaks drops it
/// straight through the frame's private `dropItem`, which the frame break's own scope in
/// `BlockAttachedEntityMixin` never sees.
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @WrapOperation(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V"))
    private void brainagelib$captureDroppedItem(
            ItemFrame frame,
            ServerLevel level,
            @Nullable Entity causedBy,
            boolean withFrame,
            Operation<Void> original) {
        if (!(causedBy instanceof ServerPlayer player)) {
            original.call(frame, level, causedBy, withFrame);
            return;
        }
        DropCapture.begin(player);
        try {
            original.call(frame, level, causedBy, withFrame);
        } finally {
            DropCapture.end();
        }
    }
}
