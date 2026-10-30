# TryTP (Fabric 1.21.11)

Client-side Fabric mod for DonutSMP-style teleport requests.

## Command

- `/trytp <username>` — wait for a matching server message that says this player enabled TPA-here, then send `/tpahere <username>` after 100 ms.
- `/trytp status` — show the current wait state.
- `/trytp cancel` — cancel the wait.

## Important detection note

A client mod cannot read another player's private teleport-toggle state. This mod therefore watches visible server system/game chat for a message that includes the target's exact username, a TPA-here term (`tpahere`, `tphere`, or `teleport here`), and an enabled/on term. If DonutSMP does not broadcast such a message, the client cannot reliably detect the toggle and this automatic trigger will not fire. It does not poll, access private server data, or bypass server rules.

## Build

Use Java 21 and Gradle 9.2.1:

```bash
cd trytp-mod
gradle build
```

The output jar is `trytp-mod/build/libs/trytp-1.0.0.jar`.

GitHub Actions builds the jar on every push or via **Actions → Build TryTP → Run workflow** and uploads it as the `trytp-jar` artifact.
