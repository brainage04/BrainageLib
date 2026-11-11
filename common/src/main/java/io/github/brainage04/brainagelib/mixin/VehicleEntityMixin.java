package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;

/// Every vehicle a player breaks: boats and rafts, plain and chest variants, and every minecart.
/// The whole break runs inside `hurtServer`, which spawns the vehicle's own item (`destroy`) and,
/// for a chest boat, chest minecart or hopper minecart, the contents
/// (`ContainerEntity.chestVehicleDestroyed`), so one scope covers both. A furnace or TNT minecart
/// has no contents to hand over. The damage source names the player for a punch, their arrow or a
/// blast they caused; a fire or lava names nobody, and a creative player's break discards the
/// vehicle the way it always does, so both leave the drops in the world.
@Mixin(VehicleEntity.class)
public abstract class VehicleEntityMixin {
    @WrapMethod(method = "hurtServer")
    private boolean brainagelib$captureBrokenVehicle(
            ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        if (!(source.getEntity() instanceof ServerPlayer player) || player.getAbilities().instabuild) {
            return original.call(level, source, damage);
        }
        DropCapture.begin(player);
        try {
            return original.call(level, source, damage);
        } finally {
            DropCapture.end();
        }
    }
}
