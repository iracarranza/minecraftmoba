package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;
import java.util.*;

/** Plant Material is a meter. Cultivars are bounded temporary world plants, never a recursive population. */
final class GardenerState implements Listener {
    static final List<String> TYPES=List.of("torchflower","sweet_berry_bush","giant_bamboo");
    private final MobaPlugin plugin; final ConfigurationSection config;
    private record Flora(Player owner,String type,UUID token,List<Block> blocks,Location center,int born,int end,boolean child) {}
    private record Handbook(List<String> remaining,int end) {}
    private final List<Flora> flora=new ArrayList<>();
    private final Set<UUID> reproduced=new HashSet<>();
    private final Map<UUID,Handbook> handbooks=new HashMap<>();
    private final Map<UUID,Map<String,Integer>> collections=new HashMap<>();
    private int clock;
    GardenerState(MobaPlugin plugin) { this.plugin=plugin;config=plugin.getConfig().getConfigurationSection("gardener"); }
    static boolean livingPlant(Material m) {
        return org.bukkit.Tag.CROPS.isTagged(m) || org.bukkit.Tag.SAPLINGS.isTagged(m) || org.bukkit.Tag.FLOWERS.isTagged(m)
                || Set.of(Material.SWEET_BERRY_BUSH,Material.BAMBOO,Material.BAMBOO_SAPLING,Material.SUGAR_CANE,
                Material.CACTUS,Material.VINE,Material.CAVE_VINES,Material.CAVE_VINES_PLANT,Material.KELP,Material.KELP_PLANT).contains(m);
    }
    static String family(Material m) {
        if(org.bukkit.Tag.SAPLINGS.isTagged(m))return "sapling";
        if(org.bukkit.Tag.FLOWERS.isTagged(m))return "flower";
        if(org.bukkit.Tag.CROPS.isTagged(m))return "crop";
        return m.name();
    }
    static boolean mature(Block b) { return !(b.getBlockData() instanceof Ageable age) || age.getAge()==age.getMaximumAge(); }
    static boolean ingredient(Material m) {
        return org.bukkit.Tag.SAPLINGS.isTagged(m) || org.bukkit.Tag.FLOWERS.isTagged(m)
                || Set.of(Material.WHEAT_SEEDS,Material.WHEAT,Material.BEETROOT_SEEDS,Material.BEETROOT,Material.CARROT,
                Material.POTATO,Material.PUMPKIN_SEEDS,Material.MELON_SEEDS,Material.TORCHFLOWER_SEEDS,Material.PITCHER_POD,
                Material.SWEET_BERRIES,Material.BAMBOO,Material.SUGAR_CANE,Material.CACTUS,Material.KELP).contains(m);
    }
    int material(Player p) { return Integer.parseInt(plugin.data(p).classState.getOrDefault("plant_material","0")); }
    boolean clip(Player p,Block b,String branch) {
        if(b==null || !livingPlant(b.getType()) || plugin.displacement().isReserved(b))return false;
        boolean mature=mature(b);double value=mature?config.getDouble("matureClip"):config.getDouble("youngClip");
        if("specimen".equals(branch) && mature)value*=config.getDouble("specimenMultiplier");
        if("proliferation".equals(branch)) {
            int count=0,r=config.getInt("familyRadius");String family=family(b.getType());
            for(int x=-r;x<=r;x++)for(int y=-1;y<=1;y++)for(int z=-r;z<=r;z++) {
                Block other=b.getRelative(x,y,z);
                if(other.getChunk().isLoaded() && livingPlant(other.getType()) && family(other.getType()).equals(family))count++;
            }
            value*=1+Math.min(config.getInt("familyCap"),count)*config.getDouble("familyBonus");
        }
        if("collection".equals(branch)) {
            var seen=collections.computeIfAbsent(p.getUniqueId(),k->new HashMap<>());
            String species=b.getType().name(),family="family."+family(b.getType());
            int repeated=seen.getOrDefault(species,0);int related=seen.getOrDefault(family,0);
            value*=config.getDouble("collectionMultiplier")/(1+repeated+related*.25);
            seen.merge(species,1,Integer::sum);seen.merge(family,1,Integer::sum);
        }
        add(p,Math.max(1,(int)Math.round(value)));
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,b.getLocation().add(.5,.6,.5),6,.2,.2,.2,0);
        return true; // No setBlockData, break or inventory yield: the source plant remains exactly as found.
    }
    void add(Player p,int amount) {
        int total=material(p)+amount,threshold=config.getInt("threshold");
        // If placement is obstructed the full meter is retained, rather than consuming resources for no plant.
        if(total>=threshold && growNext(p))total-=threshold;
        plugin.data(p).classState.put("plant_material",Integer.toString(Math.min(threshold,total)));
        p.sendActionBar(net.kyori.adventure.text.Component.text("Plant Material "+Math.min(threshold,total)+"/"+threshold));
    }
    boolean cultivate(Player p) {
        for(int i=0;i<36;i++) {
            var item=p.getInventory().getItem(i);if(item==null || !ingredient(item.getType()))continue;
            p.getInventory().setItem(i,item.getAmount()==1?null:item.asQuantity(item.getAmount()-1));
            add(p,config.getInt("inventoryMaterial"));return true;
        }
        p.sendMessage("Carry eligible plant items to Cultivate.");return false;
    }
    boolean handbook(Player p,String selected) {
        if(handbooks.containsKey(p.getUniqueId()))return false;
        var remaining=new ArrayList<>(TYPES);remaining.remove(selected);
        handbooks.put(p.getUniqueId(),new Handbook(remaining,clock+config.getInt("handbookTicks")));
        p.sendMessage("Handbook: next full meters grow "+String.join(", ",remaining)+" in sequence.");return true;
    }
    private boolean growNext(Player p) {
        if(flora.stream().filter(f->f.owner().equals(p)).count()>=config.getInt("cultivarCap"))return false;
        var book=handbooks.get(p.getUniqueId());
        String normal=plugin.data(p).classState.getOrDefault("branch.cultivate",plugin.inputs().definition("gardener").branchFor("cultivate"));
        if(!TYPES.contains(normal))normal="torchflower";
        String type=book!=null && !book.remaining().isEmpty()?book.remaining().getFirst():normal;
        if(!spawn(p,type,p.getLocation().getBlock(),false))return false;
        if(book!=null){book.remaining().removeFirst();if(book.remaining().isEmpty())handbooks.remove(p.getUniqueId());}
        return true;
    }
    boolean spawn(Player p,String type,Block around,boolean child) {
        for(int r=1;r<=config.getInt("placementRadius");r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            for(int y=-1;y<=1;y++) {
                Block at=around.getRelative(x,y,z);if(!at.getChunk().isLoaded() || !at.isEmpty() || plugin.displacement().isReserved(at))continue;
                Material ground=at.getRelative(0,-1,0).getType();
                if(!Set.of(Material.DIRT,Material.GRASS_BLOCK,Material.FARMLAND,Material.PODZOL,Material.ROOTED_DIRT,Material.MOSS_BLOCK).contains(ground))continue;
                int height="giant_bamboo".equals(type) && !child?3:1;
                var positions=new ArrayList<Block>(); boolean clear=true;
                for(int h=0;h<height;h++){Block b=at.getRelative(0,h,0);if(!b.isEmpty() || plugin.displacement().isReserved(b)){clear=false;break;}positions.add(b);}
                Block lamp=at.getRelative(0,1,0);
                if("torchflower".equals(type)){if(!lamp.isEmpty() || plugin.displacement().isReserved(lamp))continue;positions.add(lamp);}
                if(!clear)continue;
                UUID token=UUID.randomUUID();Material material="torchflower".equals(type)?Material.TORCHFLOWER:"sweet_berry_bush".equals(type)?Material.SWEET_BERRY_BUSH:Material.BAMBOO;
                boolean success=true;
                for(var b:positions){var data=(b.equals(lamp)&&"torchflower".equals(type)?Material.LIGHT:material).createBlockData();
                    if(data instanceof Ageable age)age.setAge(age.getMaximumAge());
                    if(!plugin.displacement().placeTemporary(b,token,data)){success=false;break;}}
                if(!success){plugin.displacement().restore(token);continue;}
                plugin.temporaryChunks().hold(at.getChunk());
                flora.add(new Flora(p,type,token,positions,at.getLocation().add(.5,.5,.5),clock,clock+config.getInt("lifetimeTicks"),child));return true;
            }
        }
        return false;
    }
    int cultivars(Player p) { return (int)flora.stream().filter(f->f.owner().equals(p)).count(); }
    List<String> cultivarTypes(Player p) { return flora.stream().filter(f->f.owner().equals(p)).map(Flora::type).toList(); }
    public void tick() {
        clock++;handbooks.entrySet().removeIf(e->clock>=e.getValue().end());
        for(var f:List.copyOf(flora)) {
            if(clock>=f.end() || !f.owner().isOnline() || f.owner().isDead() || !plugin.passives().has(f.owner(),"plant_material")){clear(f);continue;}
            if(clock%config.getInt("pulseTicks")==0) { accelerate(f);aura(f); }
            if(!f.child() && "giant_bamboo".equals(f.type()) && clock-f.born()>=config.getInt("offspringTicks") && reproduced.add(f.token()))
                for(int i=0;i<config.getInt("offspringCap") && cultivars(f.owner())<config.getInt("cultivarCap");i++)spawn(f.owner(),"giant_bamboo",f.blocks().getFirst(),true);
        }
    }
    private void accelerate(Flora f) {
        int r=config.getInt("growthRadius");var origin=f.blocks().getFirst();
        for(int x=-r;x<=r;x++)for(int y=-1;y<=2;y++)for(int z=-r;z<=r;z++) {
            Block b=origin.getRelative(x,y,z);if(!b.getChunk().isLoaded() || plugin.displacement().isReserved(b) || !livingPlant(b.getType()))continue;
            if(b.getBlockData() instanceof Ageable age && age.getAge()<age.getMaximumAge()) {
                age.setAge(age.getAge()+1);BlockState changed=b.getState();changed.setBlockData(age);
                var event=new BlockGrowEvent(b,changed);plugin.getServer().getPluginManager().callEvent(event);
                if(!event.isCancelled())event.getNewState().update(true,false);
            } else if(b.getType()==Material.BAMBOO || b.getType()==Material.SUGAR_CANE) {
                int height=1;while(height<3 && b.getRelative(0,-height,0).getType()==b.getType())height++;
                Block above=b.getRelative(0,1,0);
                if(height<3 && above.isEmpty() && !plugin.displacement().isReserved(above)) {
                    BlockState changed=above.getState();changed.setType(b.getType());
                    var event=new BlockGrowEvent(above,changed);plugin.getServer().getPluginManager().callEvent(event);
                    if(!event.isCancelled())event.getNewState().update(true,false);
                }
            } else b.randomTick(); // Native sapling, kelp, vines and mature stem ecology retain their own growth rules.
        }
    }
    private void aura(Flora f) {
        double r=config.getDouble("auraRadius");
        f.center().getWorld().spawnParticle(Particle.HAPPY_VILLAGER,f.center(),3,.4,.5,.4,0);
        for(var e:f.center().getWorld().getNearbyEntities(f.center(),r,2,r))if(e instanceof LivingEntity target) {
            boolean ally=target.equals(f.owner()) || Targetability.allied(target,f.owner());
            if("torchflower".equals(f.type()) && ally)target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,config.getInt("pulseTicks")+10,0,false,true,true));
            if("sweet_berry_bush".equals(f.type())) {
                if(ally && target instanceof Player p) {p.setFoodLevel(Math.min(20,p.getFoodLevel()+config.getInt("provisionFood")));}
                else if(Targetability.status(target,f.owner())) { target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,config.getInt("pulseTicks")+10,1));AbilityDamage.deal(target,config.getDouble("thicketDamage"),f.owner()); }
            }
        }
    }
    private void clear(Flora f) {
        plugin.displacement().restore(f.token());flora.remove(f);reproduced.remove(f.token());
        plugin.temporaryChunks().release(f.blocks().getFirst().getChunk());
    }
    void close() { for(var f:List.copyOf(flora))clear(f);handbooks.clear();collections.clear(); }
    void forget(Player p) { for(var f:List.copyOf(flora))if(f.owner().equals(p))clear(f);handbooks.remove(p.getUniqueId());collections.remove(p.getUniqueId()); }
    @EventHandler(priority=EventPriority.LOW,ignoreCancelled=true)
    public void broken(BlockBreakEvent e) { for(var f:List.copyOf(flora))if(f.blocks().contains(e.getBlock())){e.setCancelled(true);clear(f);} }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void harvest(org.bukkit.event.player.PlayerInteractEvent e) { if(e.getAction()==org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && e.getClickedBlock()!=null && flora.stream().anyMatch(f->f.blocks().contains(e.getClickedBlock())))e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void contact(org.bukkit.event.entity.EntityDamageEvent e) {
        if(e.getCause()!=org.bukkit.event.entity.EntityDamageEvent.DamageCause.CONTACT)return;
        if(flora.stream().anyMatch(f->"sweet_berry_bush".equals(f.type()) && f.center().getWorld().equals(e.getEntity().getWorld()) && f.center().distanceSquared(e.getEntity().getLocation())<4))e.setCancelled(true);
    }
}
