package com.minecraftmoba.plugin;

import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Version-pinned Paper 1.21.11 adapter; all game state is read on the server thread. */
public final class PacketInputs implements Listener, AutoCloseable {
    private static final String NAME="moba_input";
    private final MobaPlugin plugin;
    private final AbilityInputs inputs;
    private final Map<UUID,Channel> channels=new ConcurrentHashMap<>();
    private volatile boolean closed;
    public PacketInputs(MobaPlugin plugin,AbilityInputs inputs) { this.plugin=plugin; this.inputs=inputs; }
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e) { attach(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) { detach(e.getPlayer().getUniqueId()); }
    public void attach(Player p) {
        try {
            Object handle=p.getClass().getMethod("getHandle").invoke(p);
            Object listener=handle.getClass().getField("connection").get(handle);
            Object connection=listener.getClass().getField("connection").get(listener);
            Channel channel=(Channel)connection.getClass().getField("channel").get(connection);
            channels.put(p.getUniqueId(),channel);
            channel.eventLoop().execute(()-> {
                if (closed || !channel.isActive()) return;
                channel.pipeline().addBefore("packet_handler",NAME,new ChannelInboundHandlerAdapter() {
                    @Override public void channelRead(ChannelHandlerContext ctx,Object packet) throws Exception {
                        if (!closed && packet.getClass().getSimpleName().equals("ServerboundPlayerActionPacket")) {
                            String action=packet.getClass().getMethod("getAction").invoke(packet).toString();
                            boolean swap=action.equals("SWAP_ITEM_WITH_OFFHAND");
                            boolean drop=action.equals("DROP_ITEM") || action.equals("DROP_ALL_ITEMS");
                            if (swap || drop) {
                                // Serial submissions from this channel preserve F -> Q ordering.
                                Bukkit.getScheduler().runTask(plugin,()-> {
                                    if (closed || !p.isOnline()) return;
                                    if (plugin.enrolled(p)) {
                                        boolean consumed=inputs.input(p,swap?AbilityInputs.Input.SWAP_HAND:AbilityInputs.Input.DROP);
                                        if (swap || consumed) return;
                                    }
                                    // Unconsumed user action resumes vanilla processing. No inventory writes.
                                    channel.eventLoop().execute(()-> { if (channel.isActive()) ctx.fireChannelRead(packet); });
                                });
                                return;
                            }
                        }
                        super.channelRead(ctx,packet);
                    }
                });
            });
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().severe("Required Paper 1.21.11 packet adapter failed: "+ex);
            p.kick(net.kyori.adventure.text.Component.text("MOBA packet adapter unavailable; contact admin."));
        }
    }
    private void detach(UUID id) {
        Channel c=channels.remove(id);
        if (c!=null) c.eventLoop().execute(()-> { if(c.pipeline().get(NAME)!=null) c.pipeline().remove(NAME); });
    }
    @Override public void close() { closed=true; Set.copyOf(channels.keySet()).forEach(this::detach); }
}
