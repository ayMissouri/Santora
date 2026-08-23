package dev.santora.engine;

import dev.santora.core.update.ModVersion;
import dev.santora.mixin.MusicManagerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class VanillaMusicGain {

	private static final String FIRST_VERSION = "1.21.4";
	private static Boolean supported;

	private VanillaMusicGain() {
	}

	public static void reset() {
		if (!supported()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			((MusicManagerAccessor) mc.getMusicManager()).santora$setCurrentGain(1.0f);
		}
	}

	public static boolean supported() {
		if (supported == null) {
			supported = isAtLeast(FIRST_VERSION);
		}
		return supported;
	}

	public static boolean isAtLeast(String version) {
		return FabricLoader.getInstance().getModContainer("minecraft")
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.flatMap(ModVersion::parse)
				.flatMap(running -> ModVersion.parse(version).map(min -> running.compareTo(min) >= 0))
				.orElse(false);
	}
}
