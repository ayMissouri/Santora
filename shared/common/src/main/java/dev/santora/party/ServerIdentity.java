package dev.santora.party;

import dev.santora.core.party.ServerKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerIdentity {

	private ServerIdentity() {
	}

	public static String key() {
		ServerData server = currentServer();
		return ServerKey.of(server == null ? ServerKey.SINGLEPLAYER : server.ip);
	}

	public static String scope() {
		ServerData server = currentServer();
		return server == null
				? (inOwnWorld() ? "in this world" : "in singleplayer")
				: "on " + address(server);
	}

	public static String address() {
		ServerData server = currentServer();
		return server == null ? "" : address(server);
	}

	private static String address(ServerData server) {
		return ServerKey.normalize(server.ip);
	}

	private static boolean inOwnWorld() {
		try {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft != null && minecraft.hasSingleplayerServer();
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static ServerData currentServer() {
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft == null) {
				return null;
			}
			ServerData server = minecraft.getCurrentServer();
			return server == null || server.ip == null || server.ip.isBlank() ? null : server;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
