import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;

/** Standalone heap-layout probe using the exact Paper server classes, never a heap estimate. */
public final class PaperMemoryProbe {
    private static Instrumentation instrumentation;
    public static void premain(String args, Instrumentation value) { instrumentation = value; }
    private static long deep(Object object, Set<Object> seen) throws Exception {
        if (object == null || !seen.add(object)) return 0;
        long bytes = instrumentation.getObjectSize(object);
        Class<?> type = object.getClass();
        if (type.isArray()) {
            if (!type.componentType().isPrimitive()) for (Object child : (Object[])object) bytes += deep(child, seen);
        } else {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                field.setAccessible(true); bytes += deep(field.get(object), seen);
            }
        }
        return bytes;
    }
    private static long graph(Object root) throws Exception { return deep(root, Collections.newSetFromMap(new IdentityHashMap<>())); }
    public static void main(String[] args) throws Exception {
        var registryType = Class.forName("org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry");
        var containerType = Class.forName("org.bukkit.craftbukkit.persistence.DirtyCraftPersistentDataContainer");
        var tagType = Class.forName("net.minecraft.nbt.Tag");
        var arrayType = Class.forName("net.minecraft.nbt.ByteArrayTag");
        Object registry = registryType.getConstructor().newInstance();
        System.out.println("payloadBytes,containerAndRawMapGraphBytes,freshEmptyGraphBytes,afterRemovalGraphBytes,tagAndArrayBytes");
        for (String arg : args) {
            int length = Integer.parseInt(arg);
            Object container = containerType.getConstructor(registryType).newInstance(registry);
            var raw = (Map<?,?>)containerType.getMethod("getRaw").invoke(container);
            long shell = instrumentation.getObjectSize(container);
            long empty = shell + graph(raw);
            Object tag = arrayType.getConstructor(byte[].class).newInstance((Object)new byte[length]);
            containerType.getMethod("put", String.class, tagType).invoke(container, "minecraftmoba:placed_blocks_v1", tag);
            long full = shell + graph(raw), tagBytes = graph(tag);
            raw.clear();
            System.out.println(length + "," + full + "," + empty + "," + (shell + graph(raw)) + "," + tagBytes);
        }
    }
}
