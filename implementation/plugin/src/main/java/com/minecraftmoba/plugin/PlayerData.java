package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Authoritative persistent state; mode never contains item state. */
public final class PlayerData {
    public record ChoiceRecord(int level, String choiceId) {}
    public final UUID uuid;
    public String classId;
    public int level = 1;
    public int xp;
    public final List<ChoiceRecord> choices = new ArrayList<>();
    /** Task progression tiers by domain name; see TaskEffects. */
    public final java.util.Map<String,Integer> task = new java.util.HashMap<>();
    public final ModeState modeState = new ModeState();
    public PlayerData(UUID uuid) { this.uuid = uuid; }
    public static final class ModeState {
        public boolean active;
        public long expiresAt;
        public void clear() { active = false; expiresAt = 0; }
    }
}
