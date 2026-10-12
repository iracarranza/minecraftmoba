package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.*;

/** Empty-cell temporary terrain uses the shared reservation and exact restore ledger. */
final class HotswapAbility implements Ability, Listener {
    private record Trap(Player owner, UUID token, Block block, TNTPrimed tnt, int end, Chunk chunk) {}
    private final MobaPlugin plugin; private final ConfigurationSection config;
    private final List<Trap> traps = new ArrayList<>(); private int clock;
    HotswapAbility(MobaPlugin plugin, ConfigurationSection config) { this.plugin=plugin; this.config=config; }
    public String id() { return "hotswap"; }
    public String displayName() { return "Hotswap"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() { return AbilityOutput.single("hotswap", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.ENEMIES); }
    public Map<String,String> branches() { return Map.of("cobweb","Cobweb","lava","Lava","tnt","TNT"); }
    public List<String> branchIds() { return List.of("cobweb","lava","tnt"); }
    /**
     * The cell the placeable goes in: the one against the face being aimed at, placed at once.
     *
     * A click carries the block and the face it was on. An aimed cast (a scripted body, or an activation that carried
     * no click) used to find the block alone and always place on TOP of it; it now takes the face from the same ray,
     * so aiming at a wall's side puts the web against that side exactly as clicking it would.
     */
    private Block target(Player p, AbilityContext ctx) {
        Block b; org.bukkit.block.BlockFace face;
        if(ctx.onBlock()){ b=ctx.block(); face=ctx.face(); }
        else {
            var hit=p.rayTraceBlocks(config.getInt("range"), FluidCollisionMode.NEVER);
            if(hit==null) return null;
            b=hit.getHitBlock(); face=hit.getHitBlockFace();
        }
        if(b==null) return null;
        if(!b.isEmpty()) b=b.getRelative(face==null?org.bukkit.block.BlockFace.UP:face);
        return b.isEmpty() && !plugin.displacement().isReserved(b)?b:null;
    }
    public Collection<Block> preview(Player p,AbilityContext ctx) { Block b=target(p,ctx); return b==null?List.of():List.of(b); }
    public boolean execute(Player p, AbilityContext ctx) {
        Block b=target(p,ctx); if(b==null || !b.getChunk().isLoaded()) return false;
        String branch=ctx.branchFor(id()); UUID token=UUID.randomUUID(); TNTPrimed tnt=null;
        int duration="cobweb".equals(branch)?config.getInt("cobwebTicks"):config.getInt("durationTicks");
        if("tnt".equals(branch)) {
            tnt=b.getWorld().spawn(b.getLocation().add(.5,0,.5),TNTPrimed.class);
            tnt.setSource(p); tnt.setFuseTicks(config.getInt("fuseTicks")); tnt.setPersistent(false); duration=tnt.getFuseTicks()+1;
        } else if(!plugin.displacement().placeTemporary(b,token,("lava".equals(branch)?Material.LAVA:Material.COBWEB).createBlockData())) return false;
        Chunk chunk=b.getChunk(); plugin.temporaryChunks().hold(chunk);
        traps.add(new Trap(p,token,b,tnt,clock+duration,chunk)); return true;
    }
    public void tick() { clock++; for(var t:List.copyOf(traps)) if(clock>=t.end() || !t.owner().isOnline() || t.owner().isDead()) clear(t); }
    private void clear(Trap t) {
        plugin.displacement().restore(t.token()); if(t.tnt()!=null && t.tnt().isValid())t.tnt().remove(); traps.remove(t);
        plugin.temporaryChunks().release(t.chunk());
    }
    @EventHandler(priority=EventPriority.LOW,ignoreCancelled=true)
    public void broken(BlockBreakEvent e) { for(var t:List.copyOf(traps)) if(t.tnt()==null && t.block().equals(e.getBlock())) { e.setCancelled(true);clear(t); } }
    public void close() { for(var t:List.copyOf(traps))clear(t); }
    public void forget(Player p) { for(var t:List.copyOf(traps))if(t.owner().equals(p))clear(t); }
}
