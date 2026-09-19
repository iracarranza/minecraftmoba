package com.minecraftmoba.plugin;

import java.io.*;
import java.util.UUID;

/** Versioned binary PDC payload. Transient mode and derived capacity are excluded. */
public final class PlayerDataCodec {
    private static final int FORMAT_VERSION = 1;
    private PlayerDataCodec() {}
    public static byte[] encode(PlayerData data) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(FORMAT_VERSION);
            out.writeLong(data.uuid.getMostSignificantBits());
            out.writeLong(data.uuid.getLeastSignificantBits());
            out.writeBoolean(data.classId != null);
            if (data.classId != null) out.writeUTF(data.classId);
            out.writeInt(data.level);
            out.writeInt(data.xp);
            out.writeInt(data.choices.size());
            for (var choice : data.choices) { out.writeInt(choice.level()); out.writeUTF(choice.choiceId()); }
        }
        return bytes.toByteArray();
    }
    public static PlayerData decode(UUID expected, byte[] bytes, int maxLevel) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != FORMAT_VERSION) throw new IOException("Unknown player data format");
            var id = new UUID(in.readLong(), in.readLong());
            if (!id.equals(expected)) throw new IOException("Player UUID mismatch");
            var data = new PlayerData(id);
            data.classId = in.readBoolean() ? in.readUTF() : null;
            data.level = in.readInt();
            data.xp = in.readInt();
            if (data.level < 1 || data.level > maxLevel || data.xp < 0) throw new IOException("Invalid progression");
            int count = in.readInt();
            if (count < 0 || count > in.available()) throw new IOException("Invalid choice count");
            for (int i = 0; i < count; i++) {
                int level = in.readInt();
                String choice = in.readUTF();
                if (level < 1 || level > maxLevel || choice.isBlank()) throw new IOException("Invalid choice");
                data.choices.add(new PlayerData.ChoiceRecord(level, choice));
            }
            if (in.available() != 0) throw new IOException("Unexpected trailing data");
            return data;
        }
    }
}
