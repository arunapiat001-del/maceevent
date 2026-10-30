package com.deathdim.mace.mixin;

import com.deathdim.mace.MaceClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BipedEntityModel.class, priority = 100000)
public abstract class MaceArmMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("HEAD"))
    private void eyelid$maceHead(BipedEntityRenderState state, CallbackInfo ci) {
        MaceClient.applyState(state);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("TAIL"))
    private void eyelid$maceCelebrate(BipedEntityRenderState state, CallbackInfo ci) {
        MaceClient.applyPose((BipedEntityModel<?>) (Object) this, state);
    }
}
