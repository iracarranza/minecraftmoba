package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class CultivateAbility implements Ability {
    private final GardenerState state;private final ConfigurationSection config;
    CultivateAbility(GardenerState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "cultivate";}
    public String displayName(){return "Cultivate";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("cultivate", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
    public Map<String,String> branches(){return Map.of("torchflower","Torchflower","sweet_berry_bush","Sweet Berry Bush","giant_bamboo","Giant Bamboo");}
    public List<String> branchIds(){return GardenerState.TYPES;}
    public boolean execute(Player p,AbilityContext ctx){return state.cultivate(p);}
}
