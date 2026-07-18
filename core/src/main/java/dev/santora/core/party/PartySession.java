package dev.santora.core.party;

import java.util.ArrayList;
import java.util.List;

public final class PartySession {

	private PartyRole role = PartyRole.SOLO;
	private String code = "";
	private String selfId = "";
	private String selfName = "";
	private String hostId = "";
	private String hostName = "";

	private final List<PartyMember> members = new ArrayList<>();

	private final List<String> upcomingMirror = new ArrayList<>();
	private String mirrorContextId = "";

	private long lastNowSeq = -1;
	private long lastQueueSeq = -1;
	private PartyMessage.NowPlaying lastNow;
	private long lastNowReceiptMillis;

	// Role

	public PartyRole role() {
		return role;
	}

	public boolean inParty() {
		return role != PartyRole.SOLO;
	}

	public boolean isHost() {
		return role == PartyRole.HOST;
	}

	public boolean isMember() {
		return role == PartyRole.MEMBER;
	}

	public boolean canControlPlayback() {
		return role != PartyRole.MEMBER;
	}

	public String code() {
		return code;
	}

	public String selfId() {
		return selfId;
	}

	public String hostId() {
		return hostId;
	}

	public String hostName() {
		return hostName;
	}

	public List<PartyMember> members() {
		return List.copyOf(members);
	}

	public int memberCount() {
		return members.size();
	}

	public List<String> upcomingMirror() {
		return List.copyOf(upcomingMirror);
	}

	public String mirrorContextId() {
		return mirrorContextId;
	}

	public PartyMessage.NowPlaying lastNow() {
		return lastNow;
	}

	public long lastNowReceiptMillis() {
		return lastNowReceiptMillis;
	}

	// Transitions

	public void reset() {
		role = PartyRole.SOLO;
		code = "";
		selfId = "";
		selfName = "";
		hostId = "";
		hostName = "";
		members.clear();
		upcomingMirror.clear();
		mirrorContextId = "";
		lastNowSeq = -1;
		lastQueueSeq = -1;
		lastNow = null;
		lastNowReceiptMillis = 0;
	}

	public void onCreated(String code, String selfId, String selfName) {
		reset();
		this.role = PartyRole.HOST;
		this.code = code == null ? "" : code;
		this.selfId = selfId == null ? "" : selfId;
		this.selfName = selfName == null ? "" : selfName;
		this.hostId = this.selfId;
		this.hostName = this.selfName;
		members.add(new PartyMember(this.selfId, this.selfName));
	}

	public void onJoined(String code, String selfId, String selfName, String hostId, List<PartyMember> roster) {
		reset();
		this.role = PartyRole.MEMBER;
		this.code = code == null ? "" : code;
		this.selfId = selfId == null ? "" : selfId;
		this.selfName = selfName == null ? "" : selfName;
		this.hostId = hostId == null ? "" : hostId;
		if (roster != null) {
			members.addAll(roster);
		}
		if (findMember(this.selfId) == null) {
			members.add(new PartyMember(this.selfId, this.selfName));
		}
		PartyMember host = findMember(this.hostId);
		this.hostName = host == null ? "" : host.name();
	}

	public void onPeerJoin(PartyMember member) {
		if (member == null || findMember(member.id()) != null) {
			return;
		}
		members.add(member);
		if (member.id().equals(hostId)) {
			hostName = member.name();
		}
	}

	public boolean onPeerLeave(String id) {
		members.removeIf(m -> m.id().equals(id));
		if (role == PartyRole.MEMBER && id != null && id.equals(hostId)) {
			reset();
			return true;
		}
		return false;
	}

	public void onWelcome(PartyMessage.Welcome welcome, long receiptMillis) {
		if (welcome == null) {
			return;
		}
		if (welcome.now() != null) {
			onNow(welcome.now(), receiptMillis);
		}
		if (welcome.queue() != null) {
			onQueue(welcome.queue());
		}
	}

	public boolean onNow(PartyMessage.NowPlaying now, long receiptMillis) {
		if (now == null || now.seq() <= lastNowSeq) {
			return false;
		}
		lastNowSeq = now.seq();
		lastNow = now;
		lastNowReceiptMillis = receiptMillis;
		return true;
	}

	public boolean onQueue(PartyMessage.QueueSnapshot queue) {
		if (queue == null || queue.seq() <= lastQueueSeq) {
			return false;
		}
		lastQueueSeq = queue.seq();
		upcomingMirror.clear();
		upcomingMirror.addAll(queue.upcoming());
		mirrorContextId = queue.contextId();
		return true;
	}

	private PartyMember findMember(String id) {
		for (PartyMember member : members) {
			if (member.id().equals(id)) {
				return member;
			}
		}
		return null;
	}
}
