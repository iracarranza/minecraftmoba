package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import java.util.*;

/** Per-slot protection projection leaves real items, durability and enchantments in their owners' hands. */
final class CoveredWithDiamondsAbility implements Ability {
    private final MobaPlugin plugin; private final ConfigurationSection config;
    private record Window(Player player, int until) {}
    private final Map<UUID,Window> windows = new HashMap<>(); private int clock;
    CoveredWithDiamondsAbility(MobaPlugin plugin,ConfigurationSection config) { this.plugin=plugin;this.config=config; }
    public String id() { return "covered_with_diamonds"; }
    public String displayName() { return "Covered With Diamonds"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() { return AbilityOutput.single("covered_with_diamonds", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true); }
    private NamespacedKey key(String suffix) { return new NamespacedKey(plugin,"diamond_"+suffix); }
    boolean projecting(Player p) { return windows.containsKey(p.getUniqueId()); }
    static double armor(ItemStack item,EquipmentSlot slot,Attribute attribute) {
        if(item==null || item.isEmpty())return 0;
        var meta=item.getItemMeta(); var modifiers=meta.hasAttributeModifiers()?meta.getAttributeModifiers(slot):item.getType().getDefaultAttributeModifiers(slot);
        if(modifiers==null)return 0;
        return modifiers.get(attribute).stream().filter(m->m.getOperation()==AttributeModifier.Operation.ADD_NUMBER).mapToDouble(AttributeModifier::getAmount).sum();
    }
    static double missing(double current,double projected) { return Math.max(0,projected-current); }
    public boolean execute(Player p,AbilityContext ctx) {
        if(projecting(p))return false;
        windows.put(p.getUniqueId(),new Window(p,clock+config.getInt("durationTicks")));apply(p);
        p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation().add(0,1,0),20,.5,.8,.5,0); return true;
    }
    private void apply(Player p) {
        double armor=0,toughness=0;
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        Material[] target={Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS};
        for(int i=0;i<slots.length;i++) {
            var real=p.getInventory().getItem(slots[i]);var diamond=new ItemStack(target[i]);
            armor+=missing(armor(real,slots[i],Attribute.ARMOR),armor(diamond,slots[i],Attribute.ARMOR));
            toughness+=missing(armor(real,slots[i],Attribute.ARMOR_TOUGHNESS),armor(diamond,slots[i],Attribute.ARMOR_TOUGHNESS));
        }
        KitAttributes.set(p,Attribute.ARMOR,key("armor"),armor,AttributeModifier.Operation.ADD_NUMBER);
        KitAttributes.set(p,Attribute.ARMOR_TOUGHNESS,key("toughness"),toughness,AttributeModifier.Operation.ADD_NUMBER);
        KitAttributes.set(p,Attribute.MOVEMENT_SPEED,key("speed"),config.getDouble("speed"),AttributeModifier.Operation.ADD_SCALAR);
        KitAttributes.set(p,Attribute.KNOCKBACK_RESISTANCE,key("knockback"),config.getDouble("knockbackResistance"),AttributeModifier.Operation.ADD_NUMBER);
    }
    public void tick() { clock++;for(var w:List.copyOf(windows.values())) {
        if(clock>=w.until() || !w.player().isOnline() || w.player().isDead())clear(w.player());else apply(w.player());
    } }
    public void close() { for(var w:List.copyOf(windows.values()))clear(w.player()); }
    public void forget(Player p) { clear(p); }
    private void clear(Player p) {
        if (windows.remove(p.getUniqueId()) == null) return;
        KitAttributes.set(p,Attribute.ARMOR,key("armor"),0,AttributeModifier.Operation.ADD_NUMBER);
        KitAttributes.set(p,Attribute.ARMOR_TOUGHNESS,key("toughness"),0,AttributeModifier.Operation.ADD_NUMBER);
        KitAttributes.set(p,Attribute.MOVEMENT_SPEED,key("speed"),0,AttributeModifier.Operation.ADD_SCALAR);
        KitAttributes.set(p,Attribute.KNOCKBACK_RESISTANCE,key("knockback"),0,AttributeModifier.Operation.ADD_NUMBER);
    }
}
