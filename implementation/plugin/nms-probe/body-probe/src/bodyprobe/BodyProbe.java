package bodyprobe;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Spawns a real ServerPlayer over an EmbeddedChannel and checks each property a
 * fake-player implementation depends on. Writes plugins/BodyProbe/report.txt,
 * then stops the server. Prototype only.
 */
public final class BodyProbe extends JavaPlugin implements Listener {
    private PrintWriter out;
    private ClassLoader nms;
    private final Map<String, Integer> events = new LinkedHashMap<>();
    private final List<String> seenInbound = new ArrayList<>();
    private int pass, fail;

    // The body under test.
    private UUID id; private Object serverPlayer, connection; private EmbeddedChannel channel;

    @EventHandler public void j(PlayerJoinEvent e) { bump("join"); }
    @EventHandler public void q(PlayerQuitEvent e) { bump("quit"); }
    @EventHandler public void m(PlayerMoveEvent e) { bump("move"); }
    @EventHandler public void s(PlayerSwapHandItemsEvent e) { bump("swap"); }
    @EventHandler public void i(PlayerInputEvent e) { bump("input"); }
    @EventHandler public void a(PlayerAnimationEvent e) { bump("animation"); }
    private void bump(String k) { events.merge(k, 1, Integer::sum); }

    @Override public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getDataFolder().mkdirs();
        try { out = new PrintWriter(new File(getDataFolder(), "report.txt")); } catch (Exception e) { throw new RuntimeException(e); }
        int[] t = {40};
        schedule(t, 0, "spawn", this::spawn);
        schedule(t, 20, "pipeline", this::pipeline);
        schedule(t, 2, "attach the PacketInputs-style handler", this::attachHandler);
        schedule(t, 5, "accept the join teleport", this::acceptTeleport);
        schedule(t, 3, "client-loaded packet", this::clientLoaded);
        schedule(t, 10, "movement packet", this::move);
        schedule(t, 10, "swap-hand packet", this::swap);
        schedule(t, 10, "swap-hand through the server packet processor (NativePacketQueue path)", this::processorSubmit);
        schedule(t, 4, "processor result", this::processorResult);
        schedule(t, 10, "key-input packet", this::keys);
        schedule(t, 10, "swing packet", this::swing);
        schedule(t, 5, "outbound queue", this::outbound);
        schedule(t, 5, "despawn", this::despawn);
        schedule(t, 10, "four bodies spawn and despawn", this::churn);
        schedule(t, 10, "physics: spawn 15 blocks above ground", this::physSpawn);
        schedule(t, 25, "physics: did gravity act on the body?", this::physGravity);
        schedule(t, 5, "physics: setVelocity on a grounded body", this::physVelocity);
        schedule(t, 15, "physics: did the velocity move it?", this::physVelocityRead);
        schedule(t, 3, "physics: despawn", this::physDespawn);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            log("");
            log("SUMMARY pass=" + pass + " fail=" + fail + " events=" + events);
            out.close();
            Bukkit.shutdown();
        }, t[0] + 40);
    }

    private void schedule(int[] clock, int gap, String name, Callable<String> body) {
        clock[0] += gap;
        Bukkit.getScheduler().runTaskLater(this, () -> step(name, body), clock[0]);
    }

    private void step(String name, Callable<String> body) {
        try {
            String detail = body.call();
            boolean ok = detail == null || !detail.contains("FAIL");
            if (ok) pass++; else fail++;
            log((ok ? "PASS " : "FAIL ") + name + (detail == null ? "" : " :: " + detail));
        } catch (Throwable t) {
            fail++;
            StringWriter sw = new StringWriter(); t.printStackTrace(new PrintWriter(sw));
            Throwable root = t; while (root.getCause() != null) root = root.getCause();
            log("FAIL " + name + " :: " + root);
            String[] lines = sw.toString().split("\n");
            for (int k = 0; k < Math.min(lines.length, 14); k++) log("      " + lines[k]);
        }
    }

    private void log(String s) { out.println(s); out.flush(); getLogger().info(s); }

    // ---- reflection helpers -------------------------------------------------

    private Class<?> c(String n) throws Exception { return Class.forName(n, false, nms); }

    private static Object getField(Object o, String name) throws Exception {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try { Field f = k.getDeclaredField(name); f.setAccessible(true); return f.get(o); }
            catch (NoSuchFieldException ignore) { }
        }
        throw new NoSuchFieldException(name + " on " + o.getClass());
    }

    private Object enumConst(String cls, String name) throws Exception {
        for (Object o : c(cls).getEnumConstants()) if (o.toString().equals(name) || ((Enum<?>) o).name().equals(name)) return o;
        throw new NoSuchFieldException(cls + "." + name);
    }

    private Object serverHandle() throws Exception { return Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer()); }

    // ---- spawn -----------------------------------------------------------------

    private Object[] build(String name) throws Exception {
        nms = Bukkit.getServer().getClass().getClassLoader();
        Object world = Bukkit.getWorlds().get(0);
        Object level = world.getClass().getMethod("getHandle").invoke(world);
        UUID uid = UUID.randomUUID();
        Class<?> gp = Class.forName("com.mojang.authlib.GameProfile", false, nms);
        Object profile = gp.getConstructor(UUID.class, String.class).newInstance(uid, name);
        Object info = c("net.minecraft.server.level.ClientInformation").getMethod("createDefault").invoke(null);
        Object sp = c("net.minecraft.server.level.ServerPlayer").getConstructors()[0].newInstance(serverHandle(), level, profile, info);
        double gy = Bukkit.getWorlds().get(0).getHighestBlockYAt(0, 0) + 1;
        sp.getClass().getMethod("setPos", double.class, double.class, double.class).invoke(sp, 0.5, gy, 0.5);
        Object conn = c("net.minecraft.network.Connection").getConstructor(c("net.minecraft.network.protocol.PacketFlow"))
                .newInstance(enumConst("net.minecraft.network.protocol.PacketFlow", "SERVERBOUND"));
        EmbeddedChannel ch = new EmbeddedChannel();
        conn.getClass().getMethod("configurePacketHandler", ChannelPipeline.class).invoke(conn, ch.pipeline());
        ch.pipeline().fireChannelActive();
        Object cookie = c("net.minecraft.server.network.CommonListenerCookie").getMethod("createInitial", gp, boolean.class).invoke(null, profile, false);
        Object list = serverHandle().getClass().getMethod("getPlayerList").invoke(serverHandle());
        list.getClass().getMethod("placeNewPlayer", c("net.minecraft.network.Connection"), c("net.minecraft.server.level.ServerPlayer"),
                c("net.minecraft.server.network.CommonListenerCookie")).invoke(list, conn, sp, cookie);
        return new Object[]{uid, sp, conn, ch};
    }

    private String spawn() throws Exception {
        Object[] r = build("Body1");
        id = (UUID) r[0]; serverPlayer = r[1]; connection = r[2]; channel = (EmbeddedChannel) r[3];
        Player p = Bukkit.getPlayer(id);
        if (p == null) return "FAIL: Bukkit.getPlayer returned null after placeNewPlayer";
        return "online=" + p.isOnline() + " name=" + p.getName() + " world=" + p.getWorld().getName()
                + " loc=" + fmt(p.getLocation()) + " onlineCount=" + Bukkit.getOnlinePlayers().size()
                + " channelActive=" + channel.isActive() + " isConnected=" + connection.getClass().getMethod("isConnected").invoke(connection);
    }

    private static String fmt(Location l) { return String.format("%.1f,%.1f,%.1f", l.getX(), l.getY(), l.getZ()); }

    private String pipeline() throws Exception {
        List<String> names = channel.pipeline().names();
        Object ch = getField(connection, "channel");
        return "names=" + names + " connection.channel==embedded:" + (ch == channel)
                + (names.contains("packet_handler") ? "" : " | FAIL: no packet_handler");
    }

    /** The same reflection chain and handler PacketInputs uses on a real player. */
    private String attachHandler() throws Exception {
        Player p = Bukkit.getPlayer(id);
        Object handle = p.getClass().getMethod("getHandle").invoke(p);
        Object listener = handle.getClass().getField("connection").get(handle);
        Object conn = listener.getClass().getField("connection").get(listener);
        Channel ch = (Channel) conn.getClass().getField("channel").get(conn);
        ch.pipeline().addBefore("packet_handler", "probe_input", new ChannelInboundHandlerAdapter() {
            @Override public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
                seenInbound.add(packet.getClass().getSimpleName());
                super.channelRead(ctx, packet);
            }
        });
        return "PacketInputs reflection chain resolved; handler inserted before packet_handler; same channel=" + (ch == channel);
    }

    private String acceptTeleport() throws Exception {
        Player p = Bukkit.getPlayer(id);
        Object handle = p.getClass().getMethod("getHandle").invoke(p);
        Object listener = handle.getClass().getField("connection").get(handle);
        Object awaiting = getField(listener, "awaitingPositionFromClient");
        int tp = (Integer) getField(listener, "awaitingTeleport");
        String before = "awaitingPositionFromClient=" + awaiting + " awaitingTeleport=" + tp;
        send(c("net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket").getConstructor(int.class).newInstance(tp));
        Object after = getField(listener, "awaitingPositionFromClient");
        return before + " -> after accept: awaitingPositionFromClient=" + after + (after == null ? "" : " | FAIL: still awaiting");
    }

    private void send(Object packet) { channel.writeInbound(packet); channel.runPendingTasks(); }

    private String clientLoaded() throws Exception {
        Player p = Bukkit.getPlayer(id);
        Object handle = p.getClass().getMethod("getHandle").invoke(p);
        List<String> ms = new ArrayList<>();
        for (Method m : handle.getClass().getMethods()) if (m.getName().toLowerCase().contains("loaded")) ms.add(m.getName() + "()");
        Class<?> pk;
        try { pk = c("net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket"); }
        catch (ClassNotFoundException e) { return "FAIL: ServerboundPlayerLoadedPacket does not exist; loaded-ish methods on ServerPlayer: " + ms; }
        Object packet = pk.getConstructors().length > 0 ? pk.getConstructors()[0].newInstance() : pk.getField("INSTANCE").get(null);
        send(packet);
        return "sent " + pk.getSimpleName() + "; loaded-ish methods on ServerPlayer: " + ms;
    }

    private int swapBefore;
    private String processorSubmit() throws Exception {
        Player p = Bukkit.getPlayer(id);
        Object handle = p.getClass().getMethod("getHandle").invoke(p);
        Object listener = handle.getClass().getField("connection").get(handle);
        Object server = serverHandle();
        Object processor = server.getClass().getMethod("packetProcessor").invoke(server);
        Class<?> pkt = c("net.minecraft.network.protocol.Packet"), pl = c("net.minecraft.network.PacketListener");
        Method schedule = processor.getClass().getMethod("scheduleIfPossible", pl, pkt);
        p.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        swapBefore = events.getOrDefault("swap", 0);
        Object action = enumConst("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action", "SWAP_ITEM_WITH_OFFHAND");
        Object pos = c("net.minecraft.core.BlockPos").getField("ZERO").get(null);
        Object dir = enumConst("net.minecraft.core.Direction", "DOWN");
        Object packet = c("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket")
                .getConstructor(action.getClass(), pos.getClass(), dir.getClass()).newInstance(action, pos, dir);
        Object res = schedule.invoke(processor, listener, packet);
        return "scheduleIfPossible returned " + res + " (event result reported by the next step)";
    }

    private String processorResult() {
        Player p = Bukkit.getPlayer(id);
        int fired = events.getOrDefault("swap", 0) - swapBefore;
        return "PlayerSwapHandItemsEvent x" + fired + " offhand=" + p.getInventory().getItemInOffHand().getType()
                + (fired == 0 ? " | FAIL: swap not executed through the processor" : "");
    }

    private String move() throws Exception {
        Player p = Bukkit.getPlayer(id);
        Location b = p.getLocation(); int before = events.getOrDefault("move", 0);
        Class<?> posrot = c("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot");
        send(posrot.getConstructor(double.class, double.class, double.class, float.class, float.class, boolean.class, boolean.class)
                .newInstance(b.getX() + 0.8, b.getY(), b.getZ(), 90f, 10f, true, false));
        Location a = p.getLocation();
        int fired = events.getOrDefault("move", 0) - before;
        return "moved " + fmt(b) + " -> " + fmt(a) + " yaw " + b.getYaw() + " -> " + a.getYaw() + " PlayerMoveEvent x" + fired
                + (fired == 0 ? " | FAIL: no PlayerMoveEvent (movement not driven by packets)" : "");
    }

    private String swap() throws Exception {
        Player p = Bukkit.getPlayer(id);
        p.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        int before = events.getOrDefault("swap", 0); int seenBefore = seenInbound.size();
        Object action = enumConst("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action", "SWAP_ITEM_WITH_OFFHAND");
        Object pos = c("net.minecraft.core.BlockPos").getField("ZERO").get(null);
        Object dir = enumConst("net.minecraft.core.Direction", "DOWN");
        send(c("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket")
                .getConstructor(action.getClass(), pos.getClass(), dir.getClass()).newInstance(action, pos, dir));
        boolean sawPacket = seenInbound.subList(seenBefore, seenInbound.size()).contains("ServerboundPlayerActionPacket");
        int fired = events.getOrDefault("swap", 0) - before;
        return "handler saw ServerboundPlayerActionPacket=" + sawPacket + " | PlayerSwapHandItemsEvent x" + fired
                + " | offhand=" + p.getInventory().getItemInOffHand().getType()
                + (!sawPacket ? " | FAIL: input handler never saw the packet" : "");
    }

    private String keys() throws Exception {
        int before = events.getOrDefault("input", 0);
        Class<?> input = c("net.minecraft.world.entity.player.Input");
        Object in = input.getConstructors()[0].newInstance(true, false, false, false, false, false, false);
        send(c("net.minecraft.network.protocol.game.ServerboundPlayerInputPacket").getConstructor(input).newInstance(in));
        int fired = events.getOrDefault("input", 0) - before;
        return "PlayerInputEvent x" + fired + (fired == 0 ? " | FAIL: no PlayerInputEvent" : "");
    }

    private String swing() throws Exception {
        int before = events.getOrDefault("animation", 0);
        Object hand = enumConst("net.minecraft.world.InteractionHand", "MAIN_HAND");
        send(c("net.minecraft.network.protocol.game.ServerboundSwingPacket").getConstructor(hand.getClass()).newInstance(hand));
        int fired = events.getOrDefault("animation", 0) - before;
        return "PlayerAnimationEvent x" + fired + (fired == 0 ? " | FAIL: no PlayerAnimationEvent" : "");
    }

    private String outbound() {
        int n = channel.outboundMessages().size();
        Set<String> kinds = new TreeSet<>();
        for (Object o : channel.outboundMessages()) kinds.add(o.getClass().getSimpleName());
        channel.outboundMessages().clear();
        return "outbound packets queued on the embedded channel: " + n + " (" + kinds.size() + " kinds, e.g. " + new ArrayList<>(kinds).subList(0, Math.min(5, kinds.size())) + "); drained";
    }

    private String despawn() throws Exception {
        Object list = serverHandle().getClass().getMethod("getPlayerList").invoke(serverHandle());
        int before = Bukkit.getOnlinePlayers().size(); int quits = events.getOrDefault("quit", 0);
        list.getClass().getMethod("remove", c("net.minecraft.server.level.ServerPlayer")).invoke(list, serverPlayer);
        File data = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + id + ".dat");
        boolean wrote = data.exists();
        if (wrote) data.delete();
        return "online " + before + " -> " + Bukkit.getOnlinePlayers().size() + " | getPlayer=" + Bukkit.getPlayer(id)
                + " | PlayerQuitEvent +" + (events.getOrDefault("quit", 0) - quits) + " | playerdata file written=" + wrote + " (deleted)"
                + (Bukkit.getPlayer(id) != null ? " | FAIL: still online" : "");
    }

    private Object[] phys; private double physY0, physX0; private org.bukkit.scheduler.BukkitTask physTask; private boolean travelLogged;
    private String physSpawn() throws Exception {
        phys = build("Phys");
        Player p = Bukkit.getPlayer((UUID) phys[0]);
        // join handshake so the body is treated as loaded and accepted
        Object handle = p.getClass().getMethod("getHandle").invoke(p);
        Object listener = handle.getClass().getField("connection").get(handle);
        EmbeddedChannel ch = (EmbeddedChannel) phys[3];
        ch.writeInbound(c("net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket").getConstructor(int.class).newInstance((Integer) getField(listener, "awaitingTeleport")));
        ch.writeInbound(c("net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket").getConstructors()[0].newInstance());
        ch.runPendingTasks();
        p.teleport(new Location(p.getWorld(), 0.5, -60 + 15, 0.5));
        physY0 = p.getLocation().getY();
        // THE TEST: drive the entity's own movement step every tick, as a client would.
        final Object h = handle;
        final Class<?> vec = c("net.minecraft.world.phys.Vec3");
        final Object zero = vec.getField("ZERO").get(null);
        final Method travel = h.getClass().getMethod("travel", vec);
        physTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try { travel.invoke(h, zero); }
            catch (Throwable t) { if (!travelLogged) { travelLogged = true; log("      travel() threw: " + t + " / " + t.getCause()); } }
        }, 1L, 1L);
        return "start y=" + physY0 + " onGround=" + p.isOnGround();
    }
    private String physGravity() {
        Player p = Bukkit.getPlayer((UUID) phys[0]);
        double y = p.getLocation().getY();
        ((EmbeddedChannel) phys[3]).outboundMessages().clear();
        return "y " + physY0 + " -> " + y + " after ~25 ticks; velocity=" + p.getVelocity() + " onGround=" + p.isOnGround()
                + (Math.abs(y - physY0) < 0.01 ? " | FAIL: body did not fall; the server does not simulate its physics" : "");
    }
    private String physVelocity() {
        Player p = Bukkit.getPlayer((UUID) phys[0]);
        physX0 = p.getLocation().getX();
        p.setVelocity(new org.bukkit.util.Vector(0.6, 0, 0));
        return "x=" + physX0 + " then setVelocity(0.6,0,0)";
    }
    private String physVelocityRead() {
        Player p = Bukkit.getPlayer((UUID) phys[0]);
        double x = p.getLocation().getX();
        ((EmbeddedChannel) phys[3]).outboundMessages().clear();
        return "x " + physX0 + " -> " + x + " after ~15 ticks"
                + (Math.abs(x - physX0) < 0.01 ? " | FAIL: setVelocity did not move the body (client-simulated movement)" : "");
    }
    private String physDespawn() throws Exception {
        if (physTask != null) physTask.cancel();
        Object list = serverHandle().getClass().getMethod("getPlayerList").invoke(serverHandle());
        list.getClass().getMethod("remove", c("net.minecraft.server.level.ServerPlayer")).invoke(list, phys[1]);
        new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + phys[0] + ".dat").delete();
        return "online now " + Bukkit.getOnlinePlayers().size();
    }

    private String churn() throws Exception {
        List<Object[]> bodies = new ArrayList<>();
        for (int n = 0; n < 4; n++) bodies.add(build("Churn" + n));
        int online = Bukkit.getOnlinePlayers().size();
        Object list = serverHandle().getClass().getMethod("getPlayerList").invoke(serverHandle());
        for (Object[] b : bodies) {
            list.getClass().getMethod("remove", c("net.minecraft.server.level.ServerPlayer")).invoke(list, b[1]);
            File data = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + b[0] + ".dat"); data.delete();
        }
        int after = Bukkit.getOnlinePlayers().size();
        return "spawned 4: online=" + online + " | after despawn: online=" + after + (online != 4 || after != 0 ? " | FAIL: count mismatch" : "");
    }
}
