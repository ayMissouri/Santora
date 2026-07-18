# Santora Relay

A WebSocket relay for Santora's **Listen Together** parties. It matches
people with the same party code and forwards messages between them, stamping each with a
server timestamp so followers can stay in sync.

## Where to put the address

The mod comes with a public relay but if for whatever reason it does not work for you then you can host your own. </br> The relay address is **baked into the mod**, not configurable in game (yet?)

```java
// shared/src/main/java/dev/santora/party/PartyController.java
public static final String RELAY_URL = "wss://REPLACE_WITH_YOUR_URL";
```

Set it to your relay, and rebuild the mod (`./gradlew build`).

## Run it locally

```bash
cd relay
npm install
npm run dev
```