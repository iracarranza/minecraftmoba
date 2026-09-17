package com.minecraftmoba.probe;

import io.netty.channel.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Disposable, version-pinned diagnostic. Not a production packet dependency. */
public final class DropActionProbe extends JavaPlugin implements Listener, CommandExecutor {
    private static final String HANDLER = "moba_drop_probe";
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> drops = new ConcurrentHashMap<>();
    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("dropprobe")).setExecutor(this);
        getServer().getOnlinePlayers().forEach(this::attach);
        getLogger().warning("DIAGNOSTIC ONLY: cancels every received drop/swap action; remove after verification.");
    }
    @EventHandler public void join(PlayerJoinEvent e) { attach(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) { detach(e.getPlayer().getUniqueId()); }
    @EventHandler public void bukkitDrop(PlayerDropItemEvent e) {
        getLogger().info("BUKKIT_DROP uuid=" + e.getPlayer().getUniqueId());
        e.setCancelled(true);
    }
    private void attach(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            Object listener = handle.getClass().getField("connection").get(handle);
            Object connection = listener.getClass().getField("connection").get(listener);
            Channel channel = (Channel)connection.getClass().getField("channel").get(connection);
            var count = new AtomicLong();
            drops.put(p.getUniqueId(), count);
            channels.put(p.getUniqueId(), channel);
            channel.eventLoop().execute(() -> {
                channel.pipeline().addBefore("packet_handler", HANDLER, new ChannelInboundHandlerAdapter() {
                    @Override public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
                        if (packet.getClass().getSimpleName().equals("ServerboundPlayerActionPacket")) {
                            String action = packet.getClass().getMethod("getAction").invoke(packet).toString();
                            boolean drop = action.equals("DROP_ITEM") || action.equals("DROP_ALL_ITEMS");
                            boolean swap = action.equals("SWAP_ITEM_WITH_OFFHAND");
                            long n = drop ? count.incrementAndGet() : count.get();
                            getLogger().info("PACKET uuid=" + p.getUniqueId() + " action=" + action + " drops=" + n);
                            // Bukkit inventory reads happen only on the server thread.
                            getServer().getScheduler().runTask(DropActionProbe.this, () -> snapshot(p, "AFTER_" + action));
                            if (drop || swap) return; // control trials must never discard/swap an item
                        }
                        super.channelRead(ctx, packet);
                    }
                });
                getLogger().info("ATTACHED uuid=" + p.getUniqueId() + " before=packet_handler");
            });
        } catch (ReflectiveOperationException ex) {
            getLogger().severe("PROBE INVALID: cannot attach: " + ex);
        }
    }
    private void snapshot(Player p, String label) {
        getLogger().info("SNAPSHOT label=" + label + " uuid=" + p.getUniqueId()
            + " mainhand=" + p.getInventory().getItemInMainHand().getType()
            + " offhand=" + p.getInventory().getItemInOffHand().getType()
            + " mode=" + p.getGameMode()
            + " drops=" + drops.getOrDefault(p.getUniqueId(), new AtomicLong()).get());
    }
    private void detach(UUID id) {
        Channel channel = channels.remove(id);
        drops.remove(id);
        if (channel != null) channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(HANDLER) != null) channel.pipeline().remove(HANDLER);
        });
    }
    @Override public void onDisable() { Set.copyOf(channels.keySet()).forEach(this::detach); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dropprobe.admin")) return true;
        if (!(sender instanceof Player p) || args.length != 1) return false;
        snapshot(p, args[0]);
        sender.sendMessage("Probe boundary recorded: " + args[0]);
        return true;
    }
}
