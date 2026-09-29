package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;

public final class AbilityInputs implements Listener {
    public enum Input { SWAP_HAND, LEFT_CLICK, RIGHT_CLICK, DROP }
    private record Channel(Location origin, long end, double distanceSquared) {}
    private final MobaPlugin plugin;
    private final Provenance provenance;
    private final Map<String, Ability> abilities;
    private final Map<String, Map<Input, Ability>> kits = new HashMap<>();
    private final Map<String, ClassDefinition> classes = new HashMap<>();
    /** The class definition for an id, or null when the class is unknown. */
    public ClassDefinition definition(String id) { return id == null ? null : classes.get(id); }

    /** Every class id the draft can offer, in a stable order. */
    public java.util.List<String> ids() {
        var out = new java.util.ArrayList<>(classes.keySet());
        java.util.Collections.sort(out);
        return out;
    }
    private final AbilityCooldowns cooldowns = new AbilityCooldowns();
    private final Map<UUID, Map<String, Long>> lastFire = new HashMap<>();
    private final Map<UUID, Channel> channels = new HashMap<>();
    /** Abilities being aimed rather than cast. See AimState. */
    private final Map<UUID, AimState> aiming = new HashMap<>();
    /**
     * Abilities RUNNING while the input stays held.
     *
     * Only for hold-dependent abilities, whose output depends on how long the
     * input is held. Tracked exactly like an aim -- same repeat packets, same
     * quiet window -- because the question is the same one: is the button
     * still down. The difference is only what the answer ends.
     */
    private final Map<UUID, AimState> sustaining = new HashMap<>();
    public AimState aiming(Player p) { return aiming.get(p.getUniqueId()); }
    private final Map<UUID, Map<String, Integer>> executionCounts = new HashMap<>();
    private final Input modeInput;
    /** Input -> the slot it drives, so an unlock level can be looked up. */
    private final Map<Input, String> slotOf = new EnumMap<>(Input.class);
    private final long timeout;
    /** Which abilities and branches are combat. Required in config; load fails without it. */
    private final AbilityCombat combatRules;
    public AbilityCombat combatRules() { return combatRules; }
    private long tick;
    /** Shared by Daredevil's three abilities; registered as a listener by the plugin. */
    private final DaredevilState daredevil;
    public DaredevilState daredevil() { return daredevil; }
    public AbilityInputs(MobaPlugin plugin, Provenance provenance) {
        this.plugin=plugin; this.provenance=provenance; abilities=new HashMap<>(TestAbilities.create(plugin));
        var tunnel = plugin.getConfig().getConfigurationSection("abilities.definitions.tunneling");
        if (tunnel != null) abilities.put("tunneling", new TunnelingAbility(plugin, tunnel));
        var bounding = plugin.getConfig().getConfigurationSection("abilities.definitions.bounding");
        if (bounding != null) abilities.put("bounding", new BoundingAbility(plugin, bounding));
        var drill = plugin.getConfig().getConfigurationSection("abilities.definitions.drill_rush");
        if (drill != null) abilities.put("drill_rush", new DrillRushAbility(plugin, drill));
        // Daredevil's three share one state object: momentum and airtime are
        // measured once, and the two damage exemptions must not be able to
        // disagree about how many are left.
        daredevil = new DaredevilState(plugin);
        var runway = plugin.getConfig().getConfigurationSection("abilities.definitions.runway");
        if (runway != null) abilities.put("runway", new RunwayAbility(plugin, runway, daredevil));
        var crash = plugin.getConfig().getConfigurationSection("abilities.definitions.crash_landing");
        if (crash != null) abilities.put("crash_landing", new CrashLandingAbility(plugin, crash, daredevil));
        var clutches = plugin.getConfig().getConfigurationSection("abilities.definitions.deathly_clutches");
        if (clutches != null) abilities.put("deathly_clutches", new DeathlyClutchesAbility(plugin, clutches, daredevil));
        var c=plugin.getConfig(); timeout=c.getLong("abilities.modeTimeoutTicks");
        combatRules=AbilityCombat.load(c.getConfigurationSection("abilities.definitions"));
        modeInput=Input.valueOf(c.getString("abilities.bindings.mode"));
        Set<Input> bindings = new HashSet<>(); bindings.add(modeInput);
        for (String slot : List.of("a1","a2","ult")) {
            Input bound = Input.valueOf(c.getString("abilities.bindings." + slot));
            if (!bindings.add(bound))
                throw new IllegalArgumentException("Ability input bindings must be distinct");
            slotOf.put(bound, slot);
        }
        var classes=Objects.requireNonNull(c.getConfigurationSection("abilities.classes"));
        for (String id : classes.getKeys(false)) {
            var classSection = classes.getConfigurationSection(id);
            var legacy = stringMap(section(classSection, "infrastructureProgression"));
            var definition = new ClassDefinition(id, text(classSection, "displayName"), text(classSection, "passiveHook"),
                text(classSection, "statGrowthProfile"), legacy,
                stringMap(section(classSection, "branches")), stringMap(section(classSection, "persistentStateDefaults")),
                // Authored packets win outright over translated legacy ones at
                // the same level, so a class being migrated does not end up
                // holding both its old single effect and its replacement.
                GrowthPacket.merge(GrowthPacket.fromLegacy(legacy),
                                   GrowthPacket.load(section(classSection, "growth"))));
            this.classes.put(id, definition);
            Map<Input, Ability> kit = new EnumMap<>(Input.class);
            for (String slot : List.of("a1","a2","ult")) {
                String abilityId=classes.getString(id+"."+slot);
                // An ABSENT slot is a class still being authored: it gets a
                // partial kit and the unbound input simply does nothing.
                // A slot naming an ability that does not exist is a typo and
                // still fails loudly. Collapsing the two would mean a
                // misspelled ability quietly disabled an input instead of
                // stopping the server, which is the failure this plugin keeps
                // finding elsewhere -- a fault reporting itself far from its
                // cause.
                if (abilityId == null) continue;
                Ability ability=abilities.get(abilityId);
                if (ability == null) throw new IllegalArgumentException("Unknown ability: " + abilityId);
                kit.put(Input.valueOf(c.getString("abilities.bindings."+slot)), ability);
            }
            if (kit.size() < 3)
                plugin.getLogger().info("class '" + id + "' has a partial kit ("
                        + kit.size() + "/3 slots); unbound inputs do nothing");
            kits.put(id, Map.copyOf(kit));
        }
        plugin.getLogger().info("Registered ability kits: " + kits.keySet());
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1); // server tick cadence, not a balance constant
    }
    public boolean active(Player p) { return plugin.enrolled(p) && plugin.data(p).modeState.active; }
    public boolean input(Player p, Input input) { return input(p, input, null, null, null); }

    public boolean input(Player p, Input input,
                         org.bukkit.block.Block block, org.bukkit.block.BlockFace face) {
        return input(p, input, block, face, null);
    }

    /**
     * An activation, optionally aimed at a block.
     *
     * The block is carried from the interact event rather than re-found at
     * execute time, because "used ON block a" and "whatever I am looking at
     * now" are different predicates that only usually agree.
     */
    public boolean input(Player p, Input input,
                         org.bukkit.block.Block block, org.bukkit.block.BlockFace face,
                         org.bukkit.entity.Entity entity) {
        if (!plugin.enrolled(p)) return false;
        if (isAbilityActive(p)) {
            // A second activation is the ability's to interpret before it is
            // the input layer's to discard. Releasing a charge and cancelling
            // a channel are the same keystroke; only the ability knows which
            // it meant.
            if (recastAbilities(p)) return true;
            cancelAbilities(p);
            return true;
        }
        // A sustain in progress owns the input before an aim or the kit does.
        AimState held = sustaining.get(p.getUniqueId());
        if (held != null && input != modeInput) {
            if (input == held.input()) {
                sustaining.put(p.getUniqueId(), held.refreshed(tick));
            } else {
                endSustain(p, held);
            }
            return true;
        }
        // An aim in progress owns the next input before the kit does.
        AimState aim = aiming.get(p.getUniqueId());
        if (aim != null && input != modeInput) {
            switch (aim.onInput(input)) {
                case FIRE -> { commitAim(p, aim); return true; }
                case CANCEL -> { aiming.remove(p.getUniqueId()); return true; }
                case HOLD -> {
                    logInputGap(p, aim);
                    aiming.put(p.getUniqueId(), aim.refreshed(tick));
                    return true;
                }
            }
        }
        var d=plugin.data(p);
        if (input == modeInput) {
            if (active(p)) exit(p, false);
            else {
                d.modeState.active=true; d.modeState.expiresAt=tick+timeout;
                sound(p,"enter"); bar(p);
            }
            return true;
        }
        if (!active(p)) return false;
        var kit=kits.get(d.classId);
        if (kit == null) return true;
        Ability ability=kit.get(input);
        if (ability == null) return true;
        int required=unlockLevel(slotOf.get(input));
        if (d.level < required) {
            // Refuse rather than silently doing nothing: an ability that is not
            // yet earned should say so, and the mode should not be spent on it.
            //
            // NOT the action bar. This used to go there, where bar() rewrites
            // the same surface on a cadence and overwrote it within a tick or
            // two -- reported, accurately, as flashing illegibly. The HUD
            // notice line has one writer.
            if (plugin.hudNotice() != null) plugin.hudNotice().notUnlocked(p, required);
            return true;
        }
        var last=lastFire.computeIfAbsent(p.getUniqueId(), k->new HashMap<>());
        // Say so rather than doing nothing. A cooldown that refuses silently
        // is indistinguishable from an input that was not registered, which is
        // the worse of the two to guess at mid-fight.
        long remaining = cooldowns.remaining(p.getUniqueId(), ability.id(), tick);
        if (remaining > 0) {
            if (plugin.hudNotice() != null) plugin.hudNotice().onCooldown(p, remaining);
            return true;
        }
        if (last.getOrDefault(ability.id(), Long.MIN_VALUE) == tick) return true;
        // Aim rather than cast, when the player asked for it AND the ability
        // has something to aim. An ability with no preview ignores cast modes
        // entirely rather than growing an empty one.
        // The ability's requirement overrides the preference, minimally.
        // Quick becomes Hold for a hold-dependent ability rather than being
        // refused: refusing leaves a player unable to cast because of a
        // setting, while a tap under Hold already behaves as Quick does.
        var formContext = new Ability.AbilityContext(plugin,provenance,this,classes.get(d.classId),d,block,face,entity);
        boolean holdDependent = ability.holdDependent(p, formContext);
        CastMode mode = d.castMode().effectiveFor(holdDependent);
        var aimContext = formContext;
        if (mode.previews()
                && (holdDependent || !ability.preview(p, aimContext).isEmpty())) {
            aiming.put(p.getUniqueId(), AimState.begin(ability.id(), input, mode, tick));
            return true;
        }
        last.put(ability.id(), tick);
        var context = aimContext;
        if (ability.execute(p,context)) {
            cooldowns.start(p.getUniqueId(),ability.id(),tick,ability.cooldownTicks());
            // Activating a COMBAT ability puts the caster in combat. Classified
            // from the declaration, per branch, so Mole tunnelling or a
            // Gardener clipping a plant is not treated as fighting.
            //
            // Marked on success only: a refused or no-op activation did not
            // happen, and should not gate the caster out of levelling.
            //
            // [OPEN] A context-sensitive ability -- Skeleton Crew's Graveyard
            // Shift either Strikes or Raises -- must report what it actually
            // did rather than carry this fixed answer. The seam is unresolved,
            // and no current ability needs it.
            if (plugin.combatState()!=null && combatRules.combat(ability.id(),context.branchFor(ability.id())))
                plugin.combatState().markAbilityActivation(p);
            executionCounts.computeIfAbsent(p.getUniqueId(),k->new HashMap<>()).merge(ability.id(),1,Integer::sum);
            if (plugin.getConfig().getBoolean("abilities.logExecutions"))
                plugin.getLogger().info("ABILITY player="+p.getName()+" id="+ability.id()+" tick="+tick);
            disarm(p); return true;
        }
        bar(p); return true;
    }

    /**
     * Drop out of ability mode after a cast.
     *
     * One arming buys one ability. Casting m1 puts the HUD away, and m2 or q
     * needs a fresh arming rather than chaining off the same one.
     *
     * Unlike {@link #exit}, this does not abort an in-flight channel. A channel
     * is the ability still resolving, not an input state, so a channelled cast
     * survives its own disarm; a deliberate exit still cancels it.
     */
    private void disarm(Player p) {
        if (plugin.enrolled(p)) plugin.data(p).modeState.clear();
        p.sendActionBar(Component.empty());
    }

    /**
     * The level at which a slot becomes usable.
     *
     * classes.md's working breakpoint table: Ability 1 at level 1, Ability 2 at
     * level 2, Ultimate at 15. Before this, every slot in a configured kit
     * worked from level 1, so a fresh player had their whole kit immediately
     * and the progression table described nothing the game enforced.
     *
     * The Ultimate's timing is marked unresolved in classes.md ("The exact
     * Ultimate unlock level remains unresolved"), so 15 is the table's working
     * value carried into config rather than a decision made here.
     */
    public int unlockLevel(String slot) {
        if (slot == null) return 1;
        return plugin.getConfig().getInt("abilities.unlockLevels." + slot, 1);
    }

    private static Map<String, String> stringMap(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<String,String> result = new HashMap<>();
        for (String key : section.getKeys(false)) result.put(key, Objects.requireNonNull(section.getString(key)));
        return result;
    }
    private static String text(org.bukkit.configuration.ConfigurationSection section, String key) {
        Object value = section.get(key);
        return value == null ? null : value.toString();
    }
    private static org.bukkit.configuration.ConfigurationSection section(org.bukkit.configuration.ConfigurationSection parent, String key) {
        Object value = parent.get(key);
        return value instanceof org.bukkit.configuration.ConfigurationSection child ? child : null;
    }
    public void setBranches(Player p, int a1, int a2) {
        var d = plugin.data(p);
        var kit = d == null ? null : kits.get(d.classId);
        if (kit == null) throw new IllegalArgumentException("Class has no configured ability kit.");
        select(d, kit.get(Input.valueOf(plugin.getConfig().getString("abilities.bindings.a1"))), "a1", a1);
        select(d, kit.get(Input.valueOf(plugin.getConfig().getString("abilities.bindings.a2"))), "a2", a2);
    }
    private void select(PlayerData data, Ability ability, String slot, int index) {
        if (index < 0 || index > 3) throw new IllegalArgumentException("Branch must be between 0 and 3.");
        if (ability == null) {
            if (index != 0) throw new IllegalArgumentException(slot + " has no configured ability.");
            return;
        }
        var branches = ability.branchIds();
        if (index > branches.size()) throw new IllegalArgumentException(slot + " has only " + branches.size() + " branches.");
        String key = "branch." + ability.id();
        if (index == 0) data.classState.remove(key);
        else data.classState.put(key, branches.get(index - 1));
    }
    public void exit(Player p, boolean silent) {
        cancelAbilities(p);
        if (plugin.enrolled(p)) plugin.data(p).modeState.clear();
        channels.remove(p.getUniqueId());
        p.sendActionBar(Component.empty());
        if (!silent) sound(p,"exit");
    }
    public void forget(Player p) {
        exit(p,true); cancelAbilities(p); cooldowns.clear(p.getUniqueId()); lastFire.remove(p.getUniqueId()); executionCounts.remove(p.getUniqueId());
    }
    /**
     * Advance every aim, drawing its preview and committing when it is due.
     *
     * Recomputed against the player's CURRENT aim each tick rather than the
     * one they started with -- a frozen preview would show where the ability
     * WAS going, which is the one thing aiming exists to change.
     */
    /** End a sustain, telling the ability it is over. */
    private void endSustain(Player p, AimState held) {
        sustaining.remove(p.getUniqueId());
        Ability ability = byId(held.abilityId());
        if (ability != null) ability.cancel(p);
    }

    /** Let go, or gone. Same quiet window as an aim, because it is the same question. */
    private void tickSustains() {
        if (sustaining.isEmpty()) return;
        for (var entry : new java.util.ArrayList<>(sustaining.entrySet())) {
            Player p = Bukkit.getPlayer(entry.getKey());
            AimState held = entry.getValue();
            if (p == null || !p.isOnline() || !plugin.inMatchState(p)) {
                sustaining.remove(entry.getKey());
                continue;
            }
            if (held.onTick(tick, graceTicks(held.input()), maxAimTicks()) != AimState.Decision.HOLD)
                endSustain(p, held);
        }
    }

    private void tickAims() {
        tickSustains();
        if (aiming.isEmpty()) return;
        for (var entry : new java.util.ArrayList<>(aiming.entrySet())) {
            Player p = Bukkit.getPlayer(entry.getKey());
            AimState aim = entry.getValue();
            if (p == null || !p.isOnline() || !plugin.inMatchState(p) || !active(p)) {
                aiming.remove(entry.getKey());
                continue;
            }
            switch (aim.onTick(tick, graceTicks(aim.input()), maxAimTicks())) {
                case FIRE -> commitAim(p, aim);
                case CANCEL -> aiming.remove(entry.getKey());
                case HOLD -> {
                    if (tick % plugin.targetPreview().cadenceTicks() != 0) continue;
                    Ability ability = byId(aim.abilityId());
                    if (ability == null) { aiming.remove(entry.getKey()); continue; }
                    plugin.targetPreview().draw(p, ability.preview(p, contextFor(p)));
                }
            }
        }
    }

    /**
     * The quiet window, PER INPUT, because the client repeats them differently.
     *
     * Right-click is throttled to roughly four ticks, so its window has to
     * clear that or a genuine hold fires between packets. Left-click repeats
     * far faster while held, so it can be much shorter -- and shorter is
     * better, because this window is also the floor on how fast a TAP fires.
     *
     * [FIXTURE -- expect to tune] Both numbers are inferred from vanilla's use
     * cadence rather than measured. Set abilities.aim.logInputGaps and hold
     * each button to read the real gaps out of the log.
     */
    private long graceTicks(AbilityInputs.Input input) {
        var section = plugin.getConfig().getConfigurationSection("abilities.aim.grace");
        long fallback = input == Input.RIGHT_CLICK ? 5 : 2;
        return section == null ? fallback : section.getLong(input.name().toLowerCase(Locale.ROOT), fallback);
    }
    private long maxAimTicks() { return plugin.getConfig().getLong("abilities.aim.maxTicks", 200); }

    /**
     * How long since the same input last arrived, logged so the grace windows
     * can be set from data rather than from vanilla's documented cadence.
     *
     * Off by default. This is the measurement that decides whether a tap can
     * fire in two ticks or has to wait five.
     */
    private void logInputGap(Player p, AimState aim) {
        if (!plugin.getConfig().getBoolean("abilities.aim.logInputGaps", false)) return;
        plugin.getLogger().info("[aim] " + p.getName() + " " + aim.input()
                + " repeat gap " + (tick - aim.lastInputTick()) + " ticks");
    }

    private Ability byId(String id) { return abilities.get(id); }

    private Ability.AbilityContext contextFor(Player p) {
        var d = plugin.data(p);
        return new Ability.AbilityContext(plugin, provenance, this,
                d == null ? null : classes.get(d.classId), d);
    }

    /** Cast what was being aimed, exactly as an unaimed activation would. */
    private void commitAim(Player p, AimState aim) {
        aiming.remove(p.getUniqueId());
        Ability ability = byId(aim.abilityId());
        if (ability == null) return;
        var d = plugin.data(p);
        if (d == null) return;
        var context = contextFor(p);
        if (!ability.execute(p, context)) return;
        // A hold-dependent ability keeps running while the input stays down.
        // Under Double cast this is what makes the SECOND press holdable --
        // without it, press two would fire and forget and carry the same
        // zero-hold defect Quick has, one press later.
        if (ability.holdDependent(p, context))
            sustaining.put(p.getUniqueId(),
                    AimState.begin(ability.id(), aim.input(), CastMode.HOLD, tick));
        lastFire.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(ability.id(), tick);
        cooldowns.start(p.getUniqueId(), ability.id(), tick, ability.cooldownTicks());
        // Exactly what the unaimed path does. A cast that went through an aim
        // is still a cast, and marking combat differently would make the cast
        // mode a balance setting.
        if (plugin.combatState() != null
                && combatRules.combat(ability.id(), context.branchFor(ability.id())))
            plugin.combatState().markAbilityActivation(p);
    }

    private boolean isAbilityActive(Player p) { return abilities.values().stream().anyMatch(a -> a.active(p)); }
    private void cancelAbilities(Player p) { abilities.values().forEach(a -> a.cancel(p)); }

    /** Offer the recast to whichever ability is active; true if one took it. */
    private boolean recastAbilities(Player p) {
        var d = plugin.data(p);
        var context = new Ability.AbilityContext(plugin, provenance, this,
                d == null ? null : classes.get(d.classId), d);
        for (Ability ability : abilities.values())
            if (ability.active(p) && ability.recast(p, context)) return true;
        return false;
    }

    /** The charge running on whichever ability is active, for the HUD. */
    public Charge charging(Player p) {
        for (Ability ability : abilities.values()) {
            if (!ability.active(p)) continue;
            Charge charge = ability.charging(p);
            if (charge != null) return charge;
        }
        return null;
    }
    public void channel(Player p,long duration,double threshold) {
        channels.put(p.getUniqueId(), new Channel(p.getLocation().clone(),tick+duration,threshold*threshold));
        p.sendMessage("Channel started");
    }
    public String debug(Player p) { return "kitConfigured="+kits.containsKey(plugin.data(p).classId)+" executions="+executionCounts.getOrDefault(p.getUniqueId(),Map.of())+" channel="+channels.containsKey(p.getUniqueId()); }
    private void tick() {
        tick++;
        abilities.values().forEach(Ability::tick);
        tickAims();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (active(p) && tick > plugin.data(p).modeState.expiresAt) exit(p,true);
            Channel channel=channels.get(p.getUniqueId());
            if (channel != null) {
                // Mode is deliberately not a condition here: a cast disarms the
                // player, and that must not abort the cast it just started.
                // exit() removes the channel itself for a deliberate cancel.
                if (p.isDead() || !p.getWorld().equals(channel.origin.getWorld())
                        || p.getLocation().distanceSquared(channel.origin)>channel.distanceSquared) {
                    channels.remove(p.getUniqueId()); p.sendMessage("Channel aborted");
                } else if (tick>=channel.end) {
                    channels.remove(p.getUniqueId()); p.sendMessage("Channel complete");
                }
            }
            if (active(p)) bar(p);
        }
    }
    private void sound(Player p,String event) {
        var c=plugin.getConfig(); String path="abilities.feedback."+event;
        p.playSound(p.getLocation(),Objects.requireNonNull(c.getString(path+".sound")),
            (float)c.getDouble(path+".volume"),(float)c.getDouble(path+".pitch"));
    }
    private void bar(Player p) {
        var kit=kits.get(plugin.data(p).classId);
        Component bar=Component.empty();
        for (String slot : List.of("a1","a2","ult")) {
            Input input=Input.valueOf(plugin.getConfig().getString("abilities.bindings."+slot));
            Ability ability=kit==null?null:kit.get(input);
            String label=switch(input) { case LEFT_CLICK -> "M1"; case RIGHT_CLICK -> "M2"; case DROP -> "Q"; case SWAP_HAND -> "F"; };
            boolean cooling=ability!=null && !cooldowns.ready(p.getUniqueId(),ability.id(),tick);
            String name = ability == null ? "—" : ability.displayName() + branchSuffix(p, ability);
            bar=bar.append(Component.text(label+" "+name+"   ",cooling?NamedTextColor.GRAY:NamedTextColor.WHITE));
        }
        p.sendActionBar(bar);
    }
    private String branchSuffix(Player p, Ability ability) {
        String selected = plugin.data(p).classState.get("branch." + ability.id());
        if (selected == null) return "";
        return ability.branches().getOrDefault(selected, " (" + selected + ")")
            .transform(label -> " — " + label);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent e) {
        if (plugin.enrolled(e.getPlayer())) { e.setCancelled(true); input(e.getPlayer(),Input.SWAP_HAND); }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent e) {
        if (isAbilityActive(e.getPlayer())) { e.setCancelled(true); cancelAbilities(e.getPlayer()); return; }
        boolean inMode=active(e.getPlayer());
        if (inMode) e.setCancelled(true);
        Input input=switch(e.getAction()) {
            case LEFT_CLICK_AIR,LEFT_CLICK_BLOCK -> Input.LEFT_CLICK;
            case RIGHT_CLICK_AIR,RIGHT_CLICK_BLOCK -> Input.RIGHT_CLICK;
            default -> null;
        };
        if (input != null && input(e.getPlayer(), input, e.getClickedBlock(), e.getBlockFace()))
            e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void attack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && (isAbilityActive(p) || input(p,Input.LEFT_CLICK))) { e.setCancelled(true); if (isAbilityActive(p)) cancelAbilities(p); }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void entity(PlayerInteractEntityEvent e) {
        if (active(e.getPlayer())) e.setCancelled(true);
        // The entity is carried, not re-found. "Used ON that player" and
        // "whatever my raycast finds now" are different predicates, and a heal
        // that re-found its target could heal whoever stepped into the line
        // after the click.
        if (input(e.getPlayer(), Input.RIGHT_CLICK, null, null, e.getRightClicked()))
            e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void entityAt(PlayerInteractAtEntityEvent e) { entity(e); }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent e) { if (active(e.getPlayer()) || plugin.isMap(e.getItemDrop().getItemStack())) e.setCancelled(true); }
    @EventHandler public void death(PlayerDeathEvent e) { exit(e.getPlayer(),true); }
    @EventHandler public void quit(PlayerQuitEvent e) { forget(e.getPlayer()); }
}
