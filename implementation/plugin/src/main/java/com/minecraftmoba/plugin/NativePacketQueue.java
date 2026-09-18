package com.minecraftmoba.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import org.bukkit.entity.Player;

/** Paper 1.21.11's packet queue, also used by vanilla clicks and movement. */
final class NativePacketQueue {
    private final Object processor, listener;
    private final Class<?> packetType;
    private final Method schedule;

    static NativePacketQueue forPlayer(Player player, Object listener) throws ReflectiveOperationException {
        Object server = player.getServer().getClass().getMethod("getServer").invoke(player.getServer());
        Object processor = server.getClass().getMethod("packetProcessor").invoke(server);
        ClassLoader loader = listener.getClass().getClassLoader();
        return new NativePacketQueue(processor, listener,
            Class.forName("net.minecraft.network.protocol.Packet", false, loader),
            Class.forName("net.minecraft.network.PacketListener", false, loader));
    }
    NativePacketQueue(Object processor, Object listener, Class<?> packetType, Class<?> listenerType)
            throws ReflectiveOperationException {
        this.processor = processor; this.listener = listener; this.packetType = packetType;
        schedule = processor.getClass().getMethod("scheduleIfPossible", listenerType, packetType);
    }
    void submit(Object original, BooleanSupplier consume) throws ReflectiveOperationException {
        // The proxy is queued internally only; it is never encoded or sent over the network.
        // Vanilla's processor retains its disconnection checks and error handling.
        Object wrapped = Proxy.newProxyInstance(packetType.getClassLoader(), new Class<?>[]{packetType},
            (proxy, method, args) -> {
                if (method.getName().equals("handle") && consume.getAsBoolean()) return null;
                try { return method.invoke(original, args); }
                catch (InvocationTargetException ex) { throw ex.getCause(); }
            });
        schedule.invoke(processor, listener, wrapped);
    }
}
