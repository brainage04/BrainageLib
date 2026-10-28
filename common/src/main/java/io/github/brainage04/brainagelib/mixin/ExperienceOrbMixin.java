package io.github.brainage04.brainagelib.mixin;

import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
    private static void brainagelib$captureExperience(
            ServerLevel level, Vec3 position, Vec3 direction, int amount, CallbackInfo callback) {
        if (DropCapture.captureExperience(level, amount)) {
            callback.cancel();
        }
    }
}
