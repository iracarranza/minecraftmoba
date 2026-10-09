package probe;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dumps the signatures of the server internals a fake-player implementation
 * needs, so they are read rather than guessed. Writes
 * plugins/NmsProbe/nms-signatures.txt, then stops the server.
 */
public final class NmsProbe extends JavaPlugin {

    private static final String[][] TARGETS = {
        // class, regex over method names ("" = constructors and fields only, "*" = all)
        {"com.mojang.authlib.GameProfile", "*"},
        {"net.minecraft.server.level.ClientInformation", "*"},
        {"net.minecraft.server.level.ServerPlayer", "getBukkitEntity|restoreFrom|setGameMode|disconnect|resetLastActionTime|tick|doTick|absMoveTo|moveTo|snapTo|teleportTo|setPos|setYRot|setXRot|getYRot|getXRot|drop|getAbilities|setShiftKeyDown|setSprinting|swing|connection"},
        {"net.minecraft.server.network.CommonListenerCookie", "*"},
        {"net.minecraft.server.network.ServerGamePacketListenerImpl", "<init>|handleMovePlayer|handlePlayerInput|handlePlayerAction|handleAnimate|handleUseItem|handleUseItemOn|handleSetCarriedItem|handleInteract|handleAcceptTeleportPacket|tick|disconnect|send|onDisconnect|isAcceptingMessages"},
        {"net.minecraft.network.Connection", "channelActive|channelRead0|configurePacketHandler|setupInboundProtocol|setupOutboundProtocol|setListenerForServerboundHandshake|send|disconnect|isConnected|tick|handleDisconnection|setReadOnly|isMemoryConnection|getPacketListener|channelInactive|exceptionCaught"},
        {"net.minecraft.network.protocol.PacketFlow", "*"},
        {"net.minecraft.server.players.PlayerList", "placeNewPlayer|remove|respawn|getPlayer|load|save|canPlayerLogin|getPlayerForLogin|broadcastAll|getPlayers"},
        {"net.minecraft.server.MinecraftServer", "getPlayerList|getAllLevels|overworld|getConnection|isSameThread|execute"},
        {"net.minecraft.network.protocol.game.ServerboundMovePlayerPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos", "*"},
        {"net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot", "*"},
        {"net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Rot", "*"},
        {"net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$StatusOnly", "*"},
        {"net.minecraft.network.protocol.game.ServerboundPlayerInputPacket", "*"},
        {"net.minecraft.world.entity.player.Input", "*"},
        {"net.minecraft.network.protocol.game.ServerboundPlayerActionPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action", "*"},
        {"net.minecraft.network.protocol.game.ServerboundSwingPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundUseItemPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundUseItemOnPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundInteractPacket", "*"},
        {"net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket", "*"},
        {"net.minecraft.world.InteractionHand", "*"},
        {"io.netty.channel.embedded.EmbeddedChannel", ""},
    };

    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try { dump(); } catch (Throwable t) { getLogger().severe("probe failed: " + t); t.printStackTrace(); }
            finally { Bukkit.shutdown(); }
        }, 40L);
    }

    private void dump() throws Exception {
        File dir = getDataFolder(); dir.mkdirs();
        try (PrintWriter out = new PrintWriter(new File(dir, "nms-signatures.txt"))) {
            ClassLoader loader = Bukkit.getServer().getClass().getClassLoader();
            out.println("server=" + Bukkit.getVersion() + " | " + Bukkit.getBukkitVersion());
            out.println("craftserver=" + Bukkit.getServer().getClass().getName());
            Object handle = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            out.println("minecraftserver=" + handle.getClass().getName());
            Object world = Bukkit.getWorlds().get(0);
            Object level = world.getClass().getMethod("getHandle").invoke(world);
            out.println("serverlevel=" + level.getClass().getName());
            out.println();
            for (String[] t : TARGETS) describe(out, loader, t[0], t[1]);
        }
    }

    private static String sig(Class<?>[] types) {
        StringJoiner j = new StringJoiner(", ", "(", ")");
        for (Class<?> c : types) j.add(c.getTypeName());
        return j.toString();
    }

    private static void describe(PrintWriter out, ClassLoader loader, String name, String methodFilter) {
        out.println("=== " + name);
        Class<?> c;
        try { c = Class.forName(name, false, loader); }
        catch (Throwable t) { out.println("  MISSING: " + t); out.println(); return; }
        out.println("  " + Modifier.toString(c.getModifiers()) + (c.isRecord() ? " record" : c.isEnum() ? " enum" : c.isInterface() ? " interface" : " class")
                + " extends " + (c.getSuperclass() == null ? "-" : c.getSuperclass().getName()));
        if (c.isRecord()) for (RecordComponent rc : c.getRecordComponents()) out.println("  component " + rc.getType().getTypeName() + " " + rc.getName());
        if (c.isEnum()) out.println("  constants " + Arrays.toString(c.getEnumConstants()));
        for (Constructor<?> k : c.getDeclaredConstructors())
            out.println("  ctor " + Modifier.toString(k.getModifiers()) + " " + sig(k.getParameterTypes()));
        for (Field f : c.getDeclaredFields()) {
            if (c.isEnum() && f.isEnumConstant()) continue;
            if (methodFilter.equals("") || Modifier.isPublic(f.getModifiers()) || methodFilter.equals("*") || f.getName().matches(".*(" + methodFilter + ").*"))
                out.println("  field " + Modifier.toString(f.getModifiers()) + " " + f.getType().getTypeName() + " " + f.getName());
        }
        if (!methodFilter.equals("")) {
            Pattern p = methodFilter.equals("*") ? null : Pattern.compile(methodFilter);
            var methods = new ArrayList<>(Arrays.asList(c.getDeclaredMethods()));
            methods.sort(Comparator.comparing(Method::getName));
            for (Method m : methods)
                if (p == null || p.matcher(m.getName()).find())
                    out.println("  method " + Modifier.toString(m.getModifiers()) + " " + m.getReturnType().getTypeName() + " " + m.getName() + sig(m.getParameterTypes()));
        }
        out.println();
    }
}
