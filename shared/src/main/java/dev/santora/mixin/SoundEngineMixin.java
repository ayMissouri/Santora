package dev.santora.mixin;

import dev.santora.engine.MusicEngine;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

	@Inject(method = "resume", at = @At("TAIL"))
	private void santora$keepManualPause(CallbackInfo ci) {
		MusicEngine.get().reassertPause();
	}
}
