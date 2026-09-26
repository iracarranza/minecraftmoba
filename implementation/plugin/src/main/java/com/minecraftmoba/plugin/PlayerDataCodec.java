package com.minecraftmoba.plugin;

import java.io.*;
import java.util.UUID;

/** Versioned binary PDC payload. Transient mode and derived capacity are excluded. */
public final class PlayerDataCodec {
    private static final int FORMAT_VERSION = 2;
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
            out.writeInt(data.classState.size());
            for (var entry : data.classState.entrySet()) { out.writeUTF(entry.getKey()); out.writeUTF(entry.getValue()); }
        }
        return bytes.toByteArray();
    }
    public static PlayerData decode(UUID expected, byte[] bytes, int maxLevel) throws IOException {
        return decode(expected, bytes, maxLevel, null);
    }

    /** Decode persisted state and optionally rebuild derived Task tiers. */
    public static PlayerData decode(UUID expected, byte[] bytes, int maxLevel,
                                    TaskLedger taskLedger) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int version = in.readInt();
            if (version != 1 && version != FORMAT_VERSION) throw new IOException("Unknown player data format");
            var id = new UUID(in.readLong(), in.readLong());
            if (!id.equals(expected)) throw new IOException("Player UUID mismatch");
            var data = new PlayerData(id);
            data.classId = in.readBoolean() ? in.readUTF() : null;
            data.level = in.readInt();
            data.xp = in.readInt();
            // Lv0 is the enrolled starting state. Saves written before Lv0
            // existed carry level 1 and still decode; only fresh players change.
            if (data.level < 0 || data.level > maxLevel || data.xp < 0) throw new IOException("Invalid progression");
            int count = in.readInt();
            if (count < 0 || count > in.available()) throw new IOException("Invalid choice count");
            for (int i = 0; i < count; i++) {
                int level = in.readInt();
                String choice = in.readUTF();
                if (level < 1 || level > maxLevel || choice.isBlank()) throw new IOException("Invalid choice");
                data.choices.add(new PlayerData.ChoiceRecord(level, choice));
            }
            if (version >= 2) {
                int stateCount = in.readInt();
                if (stateCount < 0 || stateCount > 1024) throw new IOException("Invalid class state count");
                for (int i = 0; i < stateCount; i++) {
                    String key = in.readUTF(), value = in.readUTF();
                    if (key.isBlank() || key.length() > 128 || value.length() > 4096) throw new IOException("Invalid class state");
                    data.classState.put(key, value);
                }
            }
            if (in.available() != 0) throw new IOException("Unexpected trailing data");
            if (taskLedger != null) projectTask(data, taskLedger);
            return data;
        }
    }

    /**
     * ChoiceRecord is the persisted Task authority. The task map is a runtime
     * projection for TaskEffects, so it must never become stale after reload.
     * DAMAGE is retained as the persisted key for compatibility with older
     * saves; the Task design name for that tree is Slaying.
     */
    public static void projectTask(PlayerData data, TaskLedger taskLedger) {
        data.task.clear();
        var tiers = taskLedger.tiers(data.choices);
        data.task.put(TaskEffects.Domain.EFFICIENCY.name(),
            tiers.getOrDefault(TaskLedger.EFFICIENCY, 0));
        data.task.put(TaskEffects.Domain.YIELD.name(),
            tiers.getOrDefault(TaskLedger.YIELD, 0));
        data.task.put(TaskEffects.Domain.DAMAGE.name(),
            tiers.getOrDefault(TaskLedger.SLAYING, 0));
    }
}
