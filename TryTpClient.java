package com.example.trytp;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class TryTpClient implements ClientModInitializer {
    private static final long DELAY_MS = 100L;
    private static String pendingPlayer;
    private static boolean triggered;
    private static long connectionToken;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("trytp")
                .then(argument("username", StringArgumentType.word()).executes(context -> {
                    String username = StringArgumentType.getString(context, "username");
                    if (!username.matches("[A-Za-z0-9_]{1,16}")) {
                        context.getSource().sendError(Text.literal("Invalid Minecraft username."));
                        return 0;
                    }
                    pendingPlayer = username;
                    triggered = false;
                    connectionToken++;
                    context.getSource().sendFeedback(Text.literal("[TryTP] Your teleporthere is pending for " + username + ". Waiting for their TPA-here setting to become enabled..."));
                    return 1;
                }))
                .then(literal("cancel").executes(context -> {
                    pendingPlayer = null;
                    triggered = false;
                    connectionToken++;
                    context.getSource().sendFeedback(Text.literal("[TryTP] Pending request cancelled."));
                    return 1;
                }))
                .then(literal("status").executes(context -> {
                    context.getSource().sendFeedback(Text.literal(pendingPlayer == null
                        ? "[TryTP] No pending player."
                        : "[TryTP] Waiting for " + pendingPlayer + (triggered ? " (activation detected; sending shortly)." : " to enable TPA-here.")));
                    return 1;
                }))
        ));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (pendingPlayer == null || triggered) return;
            String plain = message.getString();
            if (looksLikeTargetEnabledTphere(plain, pendingPlayer)) {
                triggered = true;
                String target = pendingPlayer;
                long token = connectionToken;
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null && client.getNetworkHandler() != null) {
                        client.player.sendMessage(Text.literal("[TryTP] " + target + " appears to have enabled TPA-here. Sending /tpahere in 100 ms..."), false);
                    }
                });
                CompletableFuture.delayedExecutor(DELAY_MS, TimeUnit.MILLISECONDS).execute(() -> client.execute(() -> {
                    if (token != connectionToken || pendingPlayer == null || !pendingPlayer.equalsIgnoreCase(target)) return;
                    if (client.player != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("tpahere " + target);
                        client.player.sendMessage(Text.literal("[TryTP] Sent /tpahere " + target + "."), false);
                    }
                    pendingPlayer = null;
                    triggered = false;
                }));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getNetworkHandler() == null && pendingPlayer != null) {
                pendingPlayer = null;
                triggered = false;
                connectionToken++;
            }
        });
    }

    private static boolean looksLikeTargetEnabledTphere(String message, String player) {
        String text = message.toLowerCase(Locale.ROOT);
        String name = Pattern.quote(player.toLowerCase(Locale.ROOT));
        boolean namesTarget = Pattern.compile(".*\\b" + name + "\\b.*", Pattern.DOTALL).matcher(text).matches();
        boolean mentionsHere = text.contains("tpahere") || text.contains("tphere")
            || text.contains("teleport here") || text.contains("teleport-here") || text.contains("teleporthere");
        boolean saysEnabled = text.contains("enabled") || text.contains("turned on")
            || text.contains("now on") || text.contains("is on") || text.contains("allowing");
        return namesTarget && mentionsHere && saysEnabled;
    }
}
