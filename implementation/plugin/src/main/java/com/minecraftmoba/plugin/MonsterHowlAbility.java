package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class MonsterHowlAbility implements Ability {
    private final WerewolfState state;private final ConfigurationSection config;
    MonsterHowlAbility(WerewolfState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "monster_howl";}
    public String displayName(){return "Monster Howl";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("monster_howl", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
    public boolean execute(Player p,AbilityContext ctx){return state.howl(p);}
    public void tick(){state.tick();}
    public void forget(Player p){state.forget(p);}
}
