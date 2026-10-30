package com.deathdim.mace.mixin;

import com.deathdim.mace.MaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MaceCameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract Vec3d getCameraPos();
    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();

    @Inject(method = "update", at = @At("TAIL"))
    private void eyelid$maceCamera(CallbackInfo ci) {
        float w = MaceClient.camWeight();
        if (w <= 0f) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Vec3d base = getCameraPos();
        float baseYaw = getYaw();
        float basePitch = getPitch();
        Vec3d pivot = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.85, player.getZ());

        MaceClient.initAngle(base.x - pivot.x, base.z - pivot.z);
        double a = MaceClient.angle();
        Vec3d orbit = pivot.add(Math.cos(a) * MaceClient.RADIUS, MaceClient.HEIGHT, Math.sin(a) * MaceClient.RADIUS);

        double dx = pivot.x - orbit.x, dy = pivot.y - orbit.y, dz = pivot.z - orbit.z;
        float orbitYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float orbitPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        Vec3d pos = base.lerp(orbit, w);
        setPos(pos.x, pos.y, pos.z);
        setRotation(MathHelper.lerpAngleDegrees(w, baseYaw, orbitYaw), MathHelper.lerp(w, basePitch, orbitPitch));
    }
}
