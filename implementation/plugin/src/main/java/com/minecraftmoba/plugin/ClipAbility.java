package com.minecraftmoba.plugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class ClipAbility implements Ability {
    private final GardenerState state;private final ConfigurationSection config;
    ClipAbility(GardenerState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "clip";}
    public String displayName(){return "Clip";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("clip", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.NONE, true);}
    public Map<String,String> branches(){return Map.of("specimen","Specimen","proliferation","Proliferation","collection","Collection");}
    public List<String> branchIds(){return List.of("specimen","proliferation","collection");}
    private Block target(Player p,AbilityContext ctx){return ctx.onBlock()?ctx.block():p.getTargetBlockExact(config.getInt("range"));}
    public List<Need> needs(Player p,AbilityContext ctx) {
        return List.of(new Need("a living plant to clip, within "+config.getInt("range")+" blocks: aim at one",
                (pl,c)->{Block b=target(pl,c);return b!=null && GardenerState.livingPlant(b.getType());},
                (pl,c)->{Block b=target(pl,c);return b==null?"you are aiming at nothing in range":"you are aiming at "+b.getType().name().toLowerCase().replace('_',' ');}));
    }
    public Collection<Block> preview(Player p,AbilityContext ctx){Block b=target(p,ctx);return b!=null && GardenerState.livingPlant(b.getType())?List.of(b):List.of();}
    public boolean execute(Player p,AbilityContext ctx){return state.clip(p,target(p,ctx),ctx.branchFor(id()));}
    public void tick(){state.tick();}
    public void forget(Player p){state.forget(p);}
}
