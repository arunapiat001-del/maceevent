package com.deathdim.mace.mixin;

import com.deathdim.mace.MaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class MaceArmMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("TAIL"))
    private void eyelid$maceCelebrate(BipedEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState ps)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || ps.id != mc.player.getId()) return; // hanya pemain sendiri

        float w = MaceClient.armWeight();
        if (w <= 0f) return;

        boolean right = ps.mainArm == Arm.RIGHT;
        ModelPart arm = right ? rightArm : leftArm;
        arm.pitch = MathHelper.lerp(w, arm.pitch, -2.9f);            // lengan lurus ke atas
        arm.yaw = MathHelper.lerp(w, arm.yaw, 0f);
        arm.roll = MathHelper.lerp(w, arm.roll, right ? -0.15f : 0.15f);
        head.pitch = MathHelper.lerp(w, head.pitch, -0.45f);         // kepala menengadah
    }
}
