package dev.santora.mixin;

import dev.santora.engine.MusicEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

	@Inject(method = "stop()V", at = @At("HEAD"))
	private void santora$onStopAll(CallbackInfo ci) {
		MusicEngine.get().onExternalStop();
	}
}
