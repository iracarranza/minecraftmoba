package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
/** Three actual inputs into Hollow's shared Heart/body state. */
class HollowAbility implements Ability {
 private final HollowState state;private final String id;private final ConfigurationSection config;
 HollowAbility(HollowState s,String id,ConfigurationSection config){state=s;this.id=id;this.config=config;}
 public String id(){return id;} public String displayName(){return config.getString("displayName");} public long cooldownTicks(){return config.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single(id,TargetForm.AREA_ENTITIES,InputForm.INSTANT,Recipients.ENEMIES);}
 public Map<String,String> branches(){return id.equals("wooden_maw")?Map.of("mangling_branch","Mangling Branch","eerie_resonance","Eerie Resonance","soresight","Soresight"):id.equals("seeking_heart")?Map.of("cursed_canopy","Cursed Canopy","drawn_together","Drawn Together","creaking_boards","Creaking Boards"):Map.of();}
 public List<String> branchIds(){return id.equals("wooden_maw")?List.of("mangling_branch","eerie_resonance","soresight"):id.equals("seeking_heart")?List.of("cursed_canopy","drawn_together","creaking_boards"):List.of();}
 public boolean execute(Player p,AbilityContext ctx){return switch(id){case "wooden_maw"->state.maw(p,ctx.branchFor(id));case "seeking_heart"->state.seek(p,ctx.branchFor(id));default->state.peek(p);};}
 public boolean cooldownExemptRecast(Player p,AbilityContext ctx){var s=state.state(p);return id.equals("wooden_maw")&&s.mode.equals("MAW")||id.equals("seeking_heart")&&(s.mode.equals("SEEK")||s.mode.equals("WOOD"));}
 public void tick(){if(id.equals("wooden_maw"))state.tick();}
 public void forget(Player p){if(id.equals("wooden_maw"))state.forget(p);}
 public void close(){if(id.equals("wooden_maw"))state.close();}
}
