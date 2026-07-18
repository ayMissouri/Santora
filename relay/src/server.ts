import { WebSocketServer, WebSocket, type RawData } from 'ws';

const PORT = Number(process.env.PORT) || 8080;
const MAX_ROOMS = 500;
const MAX_MEMBERS = 20;
const RATE_BURST = 40;
const RATE_REFILL_PER_SEC = 20;
const NAME_MAX = 24;
const CODE_LEN = 6;
// No 0/O/1/I so the codes are clear.
const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

interface Conn {
	ws: WebSocket;
	id: string;
	name: string;
	roomCode: string | null;
	tokens: number;
	last: number;
}

interface Room {
	code: string;
	hostId: string;
	members: Map<string, Conn>;
}

const rooms = new Map<string, Room>();
const conns = new Map<WebSocket, Conn>();
let idCounter = 0;

const wss = new WebSocketServer({ port: PORT, maxPayload: 16 * 1024 });

wss.on('listening', () => console.log(`[santora-relay] listening on ws://localhost:${PORT}`));

wss.on('connection', (ws) => {
	const conn: Conn = {
		ws,
		id: newId(),
		name: 'Player',
		roomCode: null,
		tokens: RATE_BURST,
		last: Date.now(),
	};
	conns.set(ws, conn);
	(ws as any).isAlive = true;

	ws.on('message', (data) => onMessage(conn, data));
	ws.on('pong', () => ((ws as any).isAlive = true));
	ws.on('close', () => onClose(conn));
	ws.on('error', () => {});
});

const heartbeat = setInterval(() => {
	for (const ws of wss.clients) {
		if ((ws as any).isAlive === false) {
			ws.terminate();
			continue;
		}
		(ws as any).isAlive = false;
		try {
			ws.ping();
		} catch {
			// ignore
		}
	}
}, 30_000);
wss.on('close', () => clearInterval(heartbeat));

function onMessage(conn: Conn, data: RawData): void {
	if (!allowByRate(conn)) {
		return;
	}
	let msg: any;
	try {
		msg = JSON.parse(data.toString());
	} catch {
		return;
	}
	if (typeof msg !== 'object' || msg === null) {
		return;
	}
	switch (msg.t) {
		case 'create':
			return onCreate(conn, msg);
		case 'join':
			return onJoin(conn, msg);
		case 'leave':
			return leaveRoom(conn);
		case 'msg':
			return onRelay(conn, msg);
		default:
			return;
	}
}

function onCreate(conn: Conn, msg: any): void {
	if (conn.roomCode) {
		leaveRoom(conn);
	}
	if (rooms.size >= MAX_ROOMS) {
		send(conn.ws, { t: 'error', reason: 'server_full' });
		return;
	}
	conn.name = cleanName(msg.name);
	const code = newCode();
	const room: Room = { code, hostId: conn.id, members: new Map() };
	room.members.set(conn.id, conn);
	conn.roomCode = code;
	rooms.set(code, room);
	send(conn.ws, { t: 'created', code, self: conn.id });
}

function onJoin(conn: Conn, msg: any): void {
	const code = String(msg.code ?? '').toUpperCase();
	const room = rooms.get(code);
	if (!room) {
		send(conn.ws, { t: 'error', reason: 'bad_code' });
		return;
	}
	if (room.members.size >= MAX_MEMBERS) {
		send(conn.ws, { t: 'error', reason: 'room_full' });
		return;
	}
	if (conn.roomCode) {
		leaveRoom(conn);
	}
	conn.name = cleanName(msg.name);
	conn.roomCode = code;
	room.members.set(conn.id, conn);

	const members = [...room.members.values()].map((c) => ({ id: c.id, name: c.name }));
	send(conn.ws, { t: 'joined', code, self: conn.id, host: room.hostId, members });
	broadcast(room, { t: 'peer_join', member: { id: conn.id, name: conn.name } }, conn.id);
}

function onRelay(conn: Conn, msg: any): void {
	if (!conn.roomCode) {
		return;
	}
	const room = rooms.get(conn.roomCode);
	if (!room) {
		return;
	}
	const data = msg.data;
	if (typeof data !== 'object' || data === null) {
		return;
	}
	const out = { t: 'msg', from: conn.id, ts: Date.now(), data };
	const to = msg.to;
	if (to === undefined || to === '*') {
		broadcast(room, out, conn.id);
	} else {
		const target = room.members.get(String(to));
		if (target) {
			send(target.ws, out);
		}
	}
}

function leaveRoom(conn: Conn): void {
	const code = conn.roomCode;
	conn.roomCode = null;
	if (!code) {
		return;
	}
	const room = rooms.get(code);
	if (!room) {
		return;
	}
	room.members.delete(conn.id);
	broadcast(room, { t: 'peer_leave', id: conn.id });

	if (conn.id === room.hostId || room.members.size === 0) {
		for (const member of room.members.values()) {
			member.roomCode = null;
		}
		rooms.delete(code);
	}
}

function onClose(conn: Conn): void {
	leaveRoom(conn);
	conns.delete(conn.ws);
}

function allowByRate(conn: Conn): boolean {
	const now = Date.now();
	const elapsed = (now - conn.last) / 1000;
	conn.last = now;
	conn.tokens = Math.min(RATE_BURST, conn.tokens + elapsed * RATE_REFILL_PER_SEC);
	if (conn.tokens < 1) {
		return false;
	}
	conn.tokens -= 1;
	return true;
}

function cleanName(name: unknown): string {
	if (typeof name !== 'string') {
		return 'Player';
	}
	let out = '';
	for (const ch of name) {
		const code = ch.codePointAt(0) ?? 0;
		if (code >= 0x20 && code !== 0x7f) {
			out += ch;
		}
	}
	out = out.trim().slice(0, NAME_MAX);
	return out || 'Player';
}

function newCode(): string {
	for (let attempt = 0; attempt < 50; attempt++) {
		let code = '';
		for (let i = 0; i < CODE_LEN; i++) {
			code += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
		}
		if (!rooms.has(code)) {
			return code;
		}
	}
	return CODE_CHARS[0].repeat(CODE_LEN);
}

function newId(): string {
	idCounter += 1;
	return `${Date.now().toString(36)}-${idCounter.toString(36)}-${Math.floor(Math.random() * 1e6).toString(36)}`;
}

function send(ws: WebSocket, obj: unknown): void {
	if (ws.readyState === WebSocket.OPEN) {
		ws.send(JSON.stringify(obj));
	}
}

function broadcast(room: Room, obj: unknown, exceptId?: string): void {
	const str = JSON.stringify(obj);
	for (const member of room.members.values()) {
		if (member.id === exceptId) {
			continue;
		}
		if (member.ws.readyState === WebSocket.OPEN) {
			member.ws.send(str);
		}
	}
}
