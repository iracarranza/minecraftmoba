package com.minecraftmoba.plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.*;
final class AHeadOfScheduleAbility implements Ability {
    private final SkeletonCrewState state;private final ConfigurationSection config;
    AHeadOfScheduleAbility(SkeletonCrewState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "a_head_of_schedule";}
    public String displayName(){return "A-Head of Schedule";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("a_head_of_schedule", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE);}
    public boolean execute(Player p,AbilityContext ctx){return state.shift(p);}
    public Boolean combatActivation(Player p,AbilityContext ctx){return "COMBAT".equals(state.specialization(p));}
}
