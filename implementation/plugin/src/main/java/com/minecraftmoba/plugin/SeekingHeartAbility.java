package com.minecraftmoba.plugin;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
final class SeekingHeartAbility extends HollowAbility {
 SeekingHeartAbility(HollowState state,ConfigurationSection config){super(state,"seeking_heart",config);}
 @Override public List<AbilityOutput> outputs(){return AbilityOutput.single("seeking_heart", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.ENEMIES, true);}
}
