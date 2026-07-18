package dev.santora.core.party;

import java.util.Objects;

public record PartyMember(String id, String name) {

	public PartyMember {
		Objects.requireNonNull(id, "id");
		name = name == null ? "" : name;
	}

	public String displayName() {
		return name.isEmpty() ? id : name;
	}
}
