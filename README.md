# Santora

An in-game music player for Minecraft's own soundtrack. Browse every track as albums,
with play/pause/skip, shuffle, a queue, and crossfade.

Client-side only. No server needed, no audio shipped.

---

## Build

```bash
./gradlew buildAll          # both targets
./gradlew :v262:build       # just 26.2
./gradlew :v1211:build      # just 1.21.11
```

## Install

1. Install **Fabric Loader** for your Minecraft version.
2. Install **Fabric API**
3. Drop the matching `santora-mc<version>-1.0.0.jar` into `.minecraft/mods/`.
4. Launch. Press **M** to open the player.

## Using it

| Control | Action |
|---|---|
| **M** | Open / close |
| **Space** | Play / pause |
| **Q** | Toggle queue view |
| **Esc** | Close |