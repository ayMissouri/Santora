package dev.santora.mixin;

import dev.santora.engine.MusicEngine;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Stops default Minecraft music from playing
@Mixin(MusicManager.class)
public class MusicManagerMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void santora$suppressVanillaMusic(CallbackInfo ci) {
		if (MusicEngine.get().isManualMode()) {
			ci.cancel();
		}
	}
}
