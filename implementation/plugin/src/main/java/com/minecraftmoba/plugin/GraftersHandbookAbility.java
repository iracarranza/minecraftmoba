package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class GraftersHandbookAbility implements Ability {
    private final GardenerState state;private final ConfigurationSection config;
    GraftersHandbookAbility(GardenerState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "grafters_handbook";}
    public String displayName(){return "Grafter's Handbook";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("grafters_handbook", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
    public boolean execute(Player p,AbilityContext ctx){return state.handbook(p,ctx.branchFor("cultivate"));}
}
