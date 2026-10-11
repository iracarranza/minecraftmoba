package com.minecraftmoba.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.bukkit.entity.LivingEntity;

/** Named modifiers never overwrite another system's base attributes. */
final class KitAttributes {
    private KitAttributes() {}
    static void set(LivingEntity e, Attribute type, NamespacedKey key, double value, AttributeModifier.Operation operation) {
        var a = e.getAttribute(type); if (a == null) return;
        var old = a.getModifier(key); if (old != null) a.removeModifier(old);
        if (value != 0) a.addModifier(new AttributeModifier(key, value, operation));
    }
    static double maxHealth(LivingEntity e) { return e.getAttribute(Attribute.MAX_HEALTH).getValue(); }
}
