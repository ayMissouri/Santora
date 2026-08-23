package dev.santora.ui;

import dev.santora.platform.SantoraPlatform;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

// this lets users open the menu from any screen, not only in game.
public final class MenuAccess {

	private static final int BUTTON_SIZE = 20;
	private static final int BUTTON_MARGIN = 4;
	private static final Component BUTTON_LABEL = Component.literal("♪");

	private MenuAccess() {
	}

	public static void install() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof SantoraScreenBase) {
				return;
			}

			SantoraPlatform.Holder.get().installOpenKeyListener(screen);

			if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
				addButton(screen, width);
			}
		});
	}

	public static boolean openKeyPressed(Screen screen) {
		if (!canOpenOver(screen)) {
			return false;
		}
		open(screen);
		return true;
	}

	private static void addButton(Screen screen, int width) {
		Button button = Button.builder(BUTTON_LABEL, ignored -> open(screen))
				.bounds(width - BUTTON_SIZE - BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE)
				.tooltip(Tooltip.create(Component.translatable("screen.santora.player")))
				.build();
		SantoraPlatform.Holder.get().widgetsOf(screen).add(button);
	}

	private static void open(Screen parent) {
		SantoraPlatform platform = SantoraPlatform.Holder.get();
		platform.openScreen(platform.createPlayerScreen(parent));
	}

	private static boolean canOpenOver(Screen screen) {
		if (screen.getFocused() instanceof EditBox box && box.canConsumeInput()) {
			return false;
		}
		return !(screen instanceof KeyBindsScreen
				|| screen instanceof AbstractSignEditScreen
				|| screen instanceof BookEditScreen
				|| screen instanceof ConnectScreen
				|| screen instanceof LevelLoadingScreen
				|| screen instanceof ProgressScreen
				|| screen instanceof GenericMessageScreen);
	}
}
