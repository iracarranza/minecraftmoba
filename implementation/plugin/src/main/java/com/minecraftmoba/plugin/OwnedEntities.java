package com.minecraftmoba.plugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
/** Shared allegiance for physical class vulnerabilities and manifestations. */
final class OwnedEntities {
 static final NamespacedKey KEY=new NamespacedKey("minecraftmoba","kit_owner");
 static void tag(Entity entity,Player owner){entity.getPersistentDataContainer().set(KEY,PersistentDataType.STRING,owner.getUniqueId().toString());}
 static Player owner(Entity entity){if(entity==null||entity.getPersistentDataContainer()==null)return null;String id=entity.getPersistentDataContainer().get(KEY,PersistentDataType.STRING);return id==null?null:Bukkit.getPlayer(java.util.UUID.fromString(id));}
}
