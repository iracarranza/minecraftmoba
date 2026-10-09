package com.minecraftmoba.plugin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake players that are real {@link Player}s, connected by an embedded netty
 * channel, so every rule the plugin enforces on a human runs on them unchanged.
 *
 * <h2>Why this and not a mock</h2>
 *
 * An NPC or a mock forces an {@code if (isBot)} branch into game code, and those
 * multiply until the harness is testing a special case of the game. A body is a
 * {@code ServerPlayer} placed with {@code PlayerList.placeNewPlayer}, so
 * enrolment, {@link PlayerData}, {@link Capacity}, {@link AbilityInputs},
 * {@link Stun} and {@link Provenance} apply with no special casing. The few
 * places a body does need to be recognised are listed under "Where bodies are
 * special", and each is a deliberate exception.
 *
 * <h2>What the prototype established (docs/audit/2026-10-09-bodies-spawn-prototype.md)</h2>
 *
 * <ol>
 * <li>A body's pipeline is {@code [hackfix, packet_handler]}, so
 *     {@link PacketInputs} attaches to it unchanged.</li>
 * <li>1.21.x ignores movement and actions until the client reports it has
 *     loaded, and until the join teleport is accepted. Both are sent.</li>
 * <li><b>A body does not move itself.</b> Players are client-authoritative, so a
 *     body has no gravity and {@code setVelocity} does nothing. A per-tick
 *     {@code travel(Vec3.ZERO)} supplies both. One shared task drives every body.</li>
 * <li><b>A new body's input is silently dropped for its first few ticks.</b>
 *     Measured with the real plugin loaded: 6 ticks fails, 7 works. {@link
 *     #SETTLE_TICKS} covers it and {@link #inject} refuses before then, because
 *     a dropped input looks exactly like one that works and has no effect.</li>
 * <li>An embedded channel retains every outbound packet (hundreds per second),
 *     so a sink handler discards them instead of letting them accumulate.</li>
 * <li>{@code PlayerList.remove} writes playerdata, stats and advancements files,
 *     which are deleted so a body leaves nothing behind.</li>
 * </ol>
 *
 * <h2>{@link #connected()} is verified, not assumed</h2>
 *
 * The first call spawns a throwaway body, injects a movement packet and checks
 * that {@link PlayerMoveEvent} fires. If it does not, {@code connected()} is
 * false, and a scenario that needs real movement refuses to run instead of
 * passing without the rule ever executing.
 *
 * <h2>Where bodies are special</h2>
 *
 * Only {@link LobbyWorld#join} (which would teleport a body to the lobby) and
 * {@link ResourcePackPush#join} (which would push a pack to something that
 * cannot answer) check {@link #isBody}. Everything else runs unchanged.
 *
 * <h2>Fragility</h2>
 *
 * This is reflective access to server internals and WILL break on Paper
 * updates. Every needed class and signature is resolved once, up front, so a
 * mismatch fails with the name of what moved. Re-run implementation/plugin/
 * nms-probe to see what changed.
 *
 * Verified against a loaded {@link MobaPlugin} on Paper 1.21.11-132 by the fixture in
 * implementation/plugin/nms-probe/bodies-acceptance (18 of 18); see
 * docs/audit/2026-10-09-nms-bodies-acceptance.md. NOT yet exercised: team
 * assignment into a running match, walking input, and collision.
 */
public final class NmsBodies implements Bodies {

    private static final Set<UUID> BODIES = ConcurrentHashMap.newKeySet();

    /** Whether this player is a body. The one question a few join handlers ask. */
    public static boolean isBody(UUID id) { return id != null && BODIES.contains(id); }

    static void register(UUID id) { BODIES.add(id); }
    static void unregister(UUID id) { BODIES.remove(id); }

    // ---- server internals, resolved once ------------------------------------

    private static final class Nms {
        final Constructor<?> serverPlayer, connection;
        final Method createInfo, createCookie, placeNewPlayer, removePlayer, configurePacketHandler,
                setPos, travel, getPlayerList, getHandle;
        final Class<?> gameProfile;
        final Object serverbound, vecZero;
        final Class<?> acceptTeleport, playerLoaded, movePos, movePosRot, moveRot, swing, action, carried, playerInput, input, useItem;
        final Class<?> hand, blockPos, direction, actionEnum;
        final Object blockPosZero;

        Nms(ClassLoader l) throws ReflectiveOperationException {
            Class<?> sp = Class.forName("net.minecraft.server.level.ServerPlayer", false, l);
            Class<?> server = Class.forName("net.minecraft.server.MinecraftServer", false, l);
            Class<?> level = Class.forName("net.minecraft.server.level.ServerLevel", false, l);
            Class<?> info = Class.forName("net.minecraft.server.level.ClientInformation", false, l);
            Class<?> conn = Class.forName("net.minecraft.network.Connection", false, l);
            Class<?> flow = Class.forName("net.minecraft.network.protocol.PacketFlow", false, l);
            Class<?> cookie = Class.forName("net.minecraft.server.network.CommonListenerCookie", false, l);
            Class<?> list = Class.forName("net.minecraft.server.players.PlayerList", false, l);
            Class<?> vec = Class.forName("net.minecraft.world.phys.Vec3", false, l);
            gameProfile = Class.forName("com.mojang.authlib.GameProfile", false, l);
            serverPlayer = sp.getConstructor(server, level, gameProfile, info);
            connection = conn.getConstructor(flow);
            createInfo = info.getMethod("createDefault");
            createCookie = cookie.getMethod("createInitial", gameProfile, boolean.class);
            placeNewPlayer = list.getMethod("placeNewPlayer", conn, sp, cookie);
            removePlayer = list.getMethod("remove", sp);
            configurePacketHandler = conn.getMethod("configurePacketHandler", ChannelPipeline.class);
            setPos = sp.getMethod("setPos", double.class, double.class, double.class);
            travel = sp.getMethod("travel", vec);
            getPlayerList = server.getMethod("getPlayerList");
            getHandle = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer", false, l).getMethod("getHandle");
            serverbound = enumConst(flow, "SERVERBOUND");
            vecZero = vec.getField("ZERO").get(null);

            String g = "net.minecraft.network.protocol.game.";
            acceptTeleport = Class.forName(g + "ServerboundAcceptTeleportationPacket", false, l);
            playerLoaded = Class.forName(g + "ServerboundPlayerLoadedPacket", false, l);
            movePos = Class.forName(g + "ServerboundMovePlayerPacket$Pos", false, l);
            moveRot = Class.forName(g + "ServerboundMovePlayerPacket$Rot", false, l);
            movePosRot = Class.forName(g + "ServerboundMovePlayerPacket$PosRot", false, l);
            swing = Class.forName(g + "ServerboundSwingPacket", false, l);
            action = Class.forName(g + "ServerboundPlayerActionPacket", false, l);
            actionEnum = Class.forName(g + "ServerboundPlayerActionPacket$Action", false, l);
            carried = Class.forName(g + "ServerboundSetCarriedItemPacket", false, l);
            playerInput = Class.forName(g + "ServerboundPlayerInputPacket", false, l);
            useItem = Class.forName(g + "ServerboundUseItemPacket", false, l);
            input = Class.forName("net.minecraft.world.entity.player.Input", false, l);
            hand = Class.forName("net.minecraft.world.InteractionHand", false, l);
            blockPos = Class.forName("net.minecraft.core.BlockPos", false, l);
            direction = Class.forName("net.minecraft.core.Direction", false, l);
            blockPosZero = blockPos.getField("ZERO").get(null);
        }
    }

    static Object enumConst(Class<?> type, String name) {
        for (Object o : type.getEnumConstants()) if (((Enum<?>) o).name().equals(name)) return o;
        throw new IllegalStateException(type.getName() + " has no constant " + name);
    }

    private static Object field(Object o, String name) throws ReflectiveOperationException {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try { Field f = k.getDeclaredField(name); f.setAccessible(true); return f.get(o); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name + " on " + o.getClass().getName());
    }

    /** Discards everything the server sends a body. Nothing is listening. */
    static final class Sink extends ChannelOutboundHandlerAdapter {
        @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            ReferenceCountUtil.release(msg);
            promise.setSuccess();
        }
    }

    private record Handle(Object serverPlayer, EmbeddedChannel channel, long spawnedAt) {}

    /**
     * Ticks before a new body's input is honoured.
     *
     * Measured on Paper 1.21.11-132 with the real plugin loaded: movement sent 1,
     * 5 and 6 ticks after spawn produced no event and no motion, and from 7 ticks
     * onward it worked. The server drops it SILENTLY, which looks exactly like
     * input that works and has no effect, so {@link #inject} refuses instead. Ten
     * leaves a margin over the observed seven.
     */
    public static final int SETTLE_TICKS = 10;

    /** Pure so the boundary is tested: a body is settled once SETTLE_TICKS have passed. */
    static boolean settled(long spawnedAt, long now) { return now - spawnedAt >= SETTLE_TICKS; }

    // ---- state ------------------------------------------------------------------

    private final MobaPlugin plugin;
    private Nms nms;
    private String unavailable;
    private Boolean connected;
    private String connectedWhy = "not yet verified";
    private final Map<UUID, Handle> handles = new LinkedHashMap<>();
    private final Set<UUID> physicsFailed = ConcurrentHashMap.newKeySet();
    private BukkitTask physics;

    public NmsBodies(MobaPlugin plugin) { this.plugin = plugin; }

    private Nms nms() {
        if (nms != null) return nms;
        if (unavailable != null) throw new IllegalStateException(unavailable);
        try {
            nms = new Nms(Bukkit.getServer().getClass().getClassLoader());
            return nms;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            unavailable = "Server internals this Paper build does not match (" + Bukkit.getVersion()
                    + "): " + ex + ". Re-run implementation/plugin/nms-probe to see what moved.";
            throw new IllegalStateException(unavailable, ex);
        }
    }

    // ---- Bodies -----------------------------------------------------------------

    @Override public Body spawn(String id, Team team, String classId, int level, Location at) {
        requireMainThread();
        Player p = place(id, at != null ? at : defaultLocation());
        UUID uuid = p.getUniqueId();
        try {
            if (!plugin.enrolled(p)) p.performCommand("moba join");
            if (!plugin.enrolled(p))
                throw new IllegalStateException("the body was not enrolled; join handling refused it");
            if (classId != null) plugin.applyDraftedClass(p, classId);
            if (level > 0)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "moba setlevel " + p.getName() + " " + level);
            // Only a running match takes participants; outside one a body is
            // still a valid target for abilities, just not a team member.
            if (team != null && plugin.match() != null && plugin.match().running())
                plugin.match().add(p, team);
        } catch (RuntimeException ex) {
            remove(uuid);
            throw ex;
        }
        return new Body(id, p, team, classId, level);
    }

    @Override public void despawn(Body body) {
        if (body == null || body.player() == null) return;
        remove(body.player().getUniqueId());
    }

    /**
     * Whether movement packets written into a body drive the same events a client's do.
     *
     * Verification takes ticks (see {@link #SETTLE_TICKS}), so the first call starts
     * it and returns false with the reason "verification in progress". Call
     * {@link #prepare} early, at lab entry or plugin start, and wait for it, so a
     * scenario that needs real movement finds the answer ready. False is never
     * "unknown": a scenario that requires connected bodies refuses to run on it.
     */
    @Override public synchronized boolean connected() {
        if (connected == null) prepare(null);
        return connected != null && connected;
    }

    @Override public String describe() {
        return "NMS fake players over an embedded channel (" + Bukkit.getVersion() + "), connected="
                + (connected == null ? "unverified" : connected) + " (" + connectedWhy + ")";
    }

    /** Verified yet, one way or the other? */
    public synchronized boolean verified() { return connected != null; }

    private boolean verifying;
    private final java.util.List<java.util.function.Consumer<Boolean>> waiters = new java.util.ArrayList<>();

    /**
     * Run the connected() self-test across ticks, then call back with the result.
     *
     * Spawns a throwaway body, lets it settle, injects a movement packet and checks
     * that {@link PlayerMoveEvent} fires. Safe to call repeatedly: concurrent callers
     * share one run, and a finished answer is returned immediately.
     */
    public synchronized void prepare(java.util.function.Consumer<Boolean> done) {
        requireMainThread();
        if (connected != null) { if (done != null) done.accept(connected); return; }
        if (done != null) waiters.add(done);
        if (verifying) return;
        verifying = true;
        connectedWhy = "verification in progress; ready in about " + (SETTLE_TICKS + 2) + " ticks";
        Player p;
        try { p = place("selftest", defaultLocation()); }
        catch (RuntimeException ex) { finishVerification(false, "could not spawn the self-test body: " + ex); return; }
        MoveWatch watch = new MoveWatch(p.getUniqueId());
        Bukkit.getPluginManager().registerEvents(watch, plugin);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean ok = false; String why;
            try {
                Body body = new Body("selftest", p, null, null, 0);
                Location l = p.getLocation();
                moveTo(body, l.getX() + 0.3, l.getY(), l.getZ());
                ok = watch.moved;
                why = ok ? "a movement packet fired PlayerMoveEvent" : "a movement packet fired no PlayerMoveEvent";
            } catch (RuntimeException ex) { why = "self-test failed: " + ex; }
            finally { HandlerList.unregisterAll(watch); remove(p.getUniqueId()); }
            finishVerification(ok, why);
        }, SETTLE_TICKS + 2L);
    }

    private synchronized void finishVerification(boolean ok, String why) {
        connected = ok; connectedWhy = why; verifying = false;
        var copy = new java.util.ArrayList<>(waiters); waiters.clear();
        for (var w : copy) { try { w.accept(ok); } catch (RuntimeException ex) { plugin.getLogger().warning("[bodies] " + ex); } }
    }

    /** Whether this body has been alive long enough for its input to be honoured. */
    public boolean settled(Body b) {
        Handle h = handles.get(b.player().getUniqueId());
        return h != null && settled(h.spawnedAt(), Bukkit.getCurrentTick());
    }

    // ---- input ------------------------------------------------------------------
    //
    // Every verb is a packet written into the body's own channel, so it takes the
    // same path a client's input takes: PacketInputs' handler sees it first.

    /** F, which the plugin binds to ability activation. */
    public void swapHand(Body b) {
        Nms n = nms();
        inject(b, newInstance(n.action, new Class<?>[]{n.actionEnum, n.blockPos, n.direction},
                enumConst(n.actionEnum, "SWAP_ITEM_WITH_OFFHAND"), n.blockPosZero, enumConst(n.direction, "DOWN")));
    }

    public void dropItem(Body b) {
        Nms n = nms();
        inject(b, newInstance(n.action, new Class<?>[]{n.actionEnum, n.blockPos, n.direction},
                enumConst(n.actionEnum, "DROP_ITEM"), n.blockPosZero, enumConst(n.direction, "DOWN")));
    }

    public void swing(Body b) {
        Nms n = nms();
        inject(b, newInstance(n.swing, new Class<?>[]{n.hand}, enumConst(n.hand, "MAIN_HAND")));
    }

    public void use(Body b) {
        Nms n = nms();
        Location l = b.player().getLocation();
        inject(b, newInstance(n.useItem, new Class<?>[]{n.hand, int.class, float.class, float.class},
                enumConst(n.hand, "MAIN_HAND"), 0, l.getYaw(), l.getPitch()));
    }

    public void selectSlot(Body b, int slot) {
        Nms n = nms();
        inject(b, newInstance(n.carried, new Class<?>[]{int.class}, slot));
    }

    /** Walk or snap to a position, as a client reporting where it now is. */
    public void moveTo(Body b, double x, double y, double z) {
        Nms n = nms();
        inject(b, newInstance(n.movePos, new Class<?>[]{double.class, double.class, double.class,
                boolean.class, boolean.class}, x, y, z, b.player().isOnGround(), false));
    }

    /** As {@link #moveTo} but also turning. */
    public void moveTo(Body b, double x, double y, double z, float yaw, float pitch) {
        Nms n = nms();
        inject(b, newInstance(n.movePosRot, new Class<?>[]{double.class, double.class, double.class,
                float.class, float.class, boolean.class, boolean.class},
                x, y, z, yaw, pitch, b.player().isOnGround(), false));
    }

    /** Turn without moving. */
    public void look(Body b, float yaw, float pitch) {
        Nms n = nms();
        inject(b, newInstance(n.moveRot, new Class<?>[]{float.class, float.class, boolean.class, boolean.class},
                yaw, pitch, b.player().isOnGround(), false));
    }

    public void keys(Body b, boolean forward, boolean backward, boolean left, boolean right,
                     boolean jump, boolean shift, boolean sprint) {
        Nms n = nms();
        Object in = newInstance(n.input, new Class<?>[]{boolean.class, boolean.class, boolean.class,
                boolean.class, boolean.class, boolean.class, boolean.class},
                forward, backward, left, right, jump, shift, sprint);
        inject(b, newInstance(n.playerInput, new Class<?>[]{n.input}, in));
    }

    /** Write a serverbound packet into the body's channel, as a client would send it. */
    public void inject(Body b, Object packet) {
        Handle h = handles.get(b.player().getUniqueId());
        if (h == null) throw new IllegalStateException("body " + b.id() + " is not live");
        if (!settled(h.spawnedAt(), Bukkit.getCurrentTick()))
            throw new IllegalStateException("body " + b.id() + " has not settled: the server silently drops a new "
                    + "player's input for the first few ticks (" + SETTLE_TICKS + " to be safe). Wait for settled(body).");
        h.channel().writeInbound(packet);
        h.channel().runPendingTasks();
    }

    private static Object newInstance(Class<?> type, Class<?>[] params, Object... args) {
        try { return type.getConstructor(params).newInstance(args); }
        catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("cannot build " + type.getSimpleName() + ": " + ex, ex);
        }
    }

    // ---- placement --------------------------------------------------------------

    private Location defaultLocation() {
        var instance = plugin.worldInstance();
        org.bukkit.World w = instance == null ? null : instance.world();
        if (w == null) w = Bukkit.getWorlds().get(0);
        return w.getSpawnLocation().add(0.5, 0, 0.5);
    }

    private static String nameFor(String id) {
        String n = "B_" + id.replaceAll("[^A-Za-z0-9_]", "");
        return n.length() > 16 ? n.substring(0, 16) : n;
    }

    /** Create the ServerPlayer, join it, and make it accept the server's placement. */
    private Player place(String id, Location where) {
        Nms n = nms();
        UUID uuid = UUID.randomUUID();
        register(uuid);
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object level = where.getWorld().getClass().getMethod("getHandle").invoke(where.getWorld());
            Object profile = n.gameProfile.getConstructor(UUID.class, String.class).newInstance(uuid, nameFor(id));
            Object sp = n.serverPlayer.newInstance(server, level, profile, n.createInfo.invoke(null));
            // setPos, not snapTo: snapTo calls connection.resetPosition(), and
            // the connection does not exist until placeNewPlayer.
            n.setPos.invoke(sp, where.getX(), where.getY(), where.getZ());

            Object conn = n.connection.newInstance(n.serverbound);
            EmbeddedChannel ch = new EmbeddedChannel();
            n.configurePacketHandler.invoke(conn, ch.pipeline());
            ch.pipeline().addFirst("moba_body_sink", new Sink());
            ch.pipeline().fireChannelActive();
            Object cookie = n.createCookie.invoke(null, profile, false);
            n.placeNewPlayer.invoke(n.getPlayerList.invoke(server), conn, sp, cookie);

            Player p = Bukkit.getPlayer(uuid);
            if (p == null) throw new IllegalStateException("placeNewPlayer returned but the body is not online");
            handles.put(uuid, new Handle(sp, ch, Bukkit.getCurrentTick()));
            // The server may have moved a new player to the world spawn.
            p.teleport(where);
            admit(handles.get(uuid), sp);
            startPhysics();
            return p;
        } catch (InvocationTargetException ex) {
            remove(uuid);
            throw new IllegalStateException("could not create body '" + id + "': " + ex.getCause(), ex.getCause());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            remove(uuid);
            throw new IllegalStateException("could not create body '" + id + "': " + ex, ex);
        }
    }

    /**
     * Accept the server's teleport and report the client loaded.
     *
     * 1.21.x drops movement and actions from a client that has not done both,
     * silently, which looks exactly like input that works and has no effect.
     */
    private void admit(Handle h, Object sp) throws ReflectiveOperationException {
        Nms n = nms();
        Object listener = field(sp, "connection");
        if (field(listener, "awaitingPositionFromClient") != null) {
            int id = (Integer) field(listener, "awaitingTeleport");
            h.channel().writeInbound(n.acceptTeleport.getConstructor(int.class).newInstance(id));
        }
        h.channel().writeInbound(n.playerLoaded.getConstructors()[0].newInstance());
        h.channel().runPendingTasks();
    }

    private void remove(UUID uuid) {
        Handle h = handles.remove(uuid);
        try {
            if (h != null) {
                Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
                nms().removePlayer.invoke(nms().getPlayerList.invoke(server), h.serverPlayer());
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("[bodies] removing " + uuid + ": " + ex);
        } finally {
            deleteFiles(uuid);
            unregister(uuid);
            physicsFailed.remove(uuid);
            if (handles.isEmpty()) stopPhysics();
        }
    }

    /** removePlayer writes these; a body must leave nothing behind. */
    private static void deleteFiles(UUID uuid) {
        File root = Bukkit.getWorlds().get(0).getWorldFolder();
        for (String f : new String[]{"playerdata/" + uuid + ".dat", "playerdata/" + uuid + ".dat_old",
                "stats/" + uuid + ".json", "advancements/" + uuid + ".json"})
            new File(root, f).delete();
    }

    // ---- physics ----------------------------------------------------------------

    /** One task for every body, so cost does not scale with schedulers. */
    private void startPhysics() {
        if (physics != null) return;
        physics = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBodies, 1L, 1L);
    }

    private void stopPhysics() {
        if (physics != null) { physics.cancel(); physics = null; }
    }

    private void tickBodies() {
        Nms n;
        try { n = nms(); } catch (RuntimeException ex) { return; }
        for (var e : handles.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || p.isDead()) continue;
            try { n.travel.invoke(e.getValue().serverPlayer(), n.vecZero); }
            catch (ReflectiveOperationException ex) {
                if (physicsFailed.add(e.getKey()))
                    plugin.getLogger().warning("[bodies] physics step failed for " + e.getKey() + ": " + ex);
            }
        }
    }

    // ---- the connected() self-test ---------------------------------------------

    private final class MoveWatch implements Listener {
        final UUID target; boolean moved;
        MoveWatch(UUID target) { this.target = target; }
        @EventHandler public void move(PlayerMoveEvent e) { if (e.getPlayer().getUniqueId().equals(target)) moved = true; }
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Bodies must be created and removed on the server thread.");
    }

    /** Live bodies, for diagnostics and tests. */
    public int live() { return handles.size(); }
}
