package dev.santora.core.party;

public record PublicParty(String code, String hostName, int memberCount) {

	public PublicParty {
		code = code == null ? "" : code;
		hostName = hostName == null ? "" : hostName;
		memberCount = Math.max(1, memberCount);
	}

	public String displayHost() {
		return hostName.isEmpty() ? "Someone" : hostName;
	}
}
