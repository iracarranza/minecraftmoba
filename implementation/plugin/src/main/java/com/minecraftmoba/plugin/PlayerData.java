package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/** Authoritative persistent state; mode never contains item state. */
public final class PlayerData {
    public record ChoiceRecord(int level, String choiceId) {}
    public final UUID uuid;
    public String classId;
    /**
     * Enrolment starts at Lv0, which is a real progression state and not
     * bookkeeping: Lv0 holds the Passive alone, Lv1 earns Active 1 and Lv2
     * earns Active 2. Starting at 1 would erase the first observable
     * progression event.
     */
    public int level = 0;
    public int xp;
    public final List<ChoiceRecord> choices = new ArrayList<>();
    /** Task progression tiers by domain name; see TaskEffects. */
    public final java.util.Map<String,Integer> task = new java.util.HashMap<>();
    /** Level 6 fork result: an Infrastructure form, or MONSTER_COMBAT. Null until chosen. */
    public String contribution;
    /** Opaque, class-owned persistent state. The class framework owns its namespace, not its schema. */
    public final Map<String, String> classState = new HashMap<>();
    public final ModeState modeState = new ModeState();
    public PlayerData(UUID uuid) { this.uuid = uuid; }
    public static final class ModeState {
        public boolean active;
        public long expiresAt;
        public void clear() { active = false; expiresAt = 0; }
    }
}
