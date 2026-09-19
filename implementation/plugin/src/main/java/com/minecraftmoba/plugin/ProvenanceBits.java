package com.minecraftmoba.plugin;

import java.util.BitSet;

/** Chunk-local index is independent of the world's height and supports negative Y. */
public final class ProvenanceBits {
    private ProvenanceBits() {}
    public static int index(int x, int y, int z, int minHeight, int maxHeight) {
        if (y < minHeight || y >= maxHeight) throw new IllegalArgumentException("Y outside world");
        return Math.addExact(Math.multiplyExact(y - minHeight, 16 * 16), ((z & 15) * 16) + (x & 15));
    }
    public static byte[] change(byte[] bytes, int index, boolean placed) {
        var bits = bytes == null ? new BitSet() : BitSet.valueOf(bytes);
        bits.set(index, placed);
        return bits.toByteArray(); // shrinks after high bits clear; empty means remove PDC key
    }
    public static boolean contains(byte[] bytes, int index) {
        int octet = index / Byte.SIZE;
        return bytes != null && octet < bytes.length && (bytes[octet] & (1 << (index % Byte.SIZE))) != 0;
    }
}
