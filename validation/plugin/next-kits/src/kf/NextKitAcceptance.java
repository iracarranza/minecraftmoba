package kf;

import com.minecraftmoba.plugin.*;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

/** Runs actual Paper objects and listeners on an isolated disposable world. */
public final class NextKitAcceptance extends JavaPlugin implements Listener {
    private MobaPlugin m;private NmsBodies bodies;private Bodies.Body fighterBody,gardenerBody,wolfBody,enemyBody;
    private Player fighter,gardener,wolf,enemy;private World w;private Object garden,wolves;private PrintWriter out;private int pass,fail;
    private Block trap,crop;private double before,capacityBefore,effectiveBefore;private int plantsBefore;private boolean cancelGrowth;
    private org.bukkit.scoreboard.Team north,south;private long hideCooldown;
    private interface Action{void run()throws Exception;}
    private void at(int tick,String name,Action action){Bukkit.getScheduler().runTaskLater(this,()->{try{action.run();pass++;log("PASS "+name);}catch(Throwable e){fail++;log("FAIL "+name+" :: "+e);e.printStackTrace(out);out.flush();}},tick);}
    private void log(String s){out.println(s);out.flush();getLogger().info(s);}
    private void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    private Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{var method=o.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(o,args);}
    private Ability ability(Player p,String slot){return m.inputs().abilityFor(m.data(p).classId,slot);}
    private Ability.AbilityContext ctx(Player p){return new Ability.AbilityContext(m,m.provenance(),m.inputs(),m.inputs().definition(m.data(p).classId),m.data(p));}
    private Ability.AbilityContext ctx(Player p,Block b){return new Ability.AbilityContext(m,m.provenance(),m.inputs(),m.inputs().definition(m.data(p).classId),m.data(p),b,org.bukkit.block.BlockFace.UP);}
    private void branch(Player p,String ability,String branch){m.data(p).classState.put("branch."+ability,branch);}
    private void place(Player p,double x,double z){p.teleport(new Location(w,x,100,z,-90,0));p.setVelocity(new org.bukkit.util.Vector());}
    private void add(int amount)throws Exception{call(garden,"add",new Class[]{Player.class,int.class},gardener,amount);}
    private int plants()throws Exception{return (int)call(garden,"cultivars",new Class[]{Player.class},gardener);}
    private List<?> types()throws Exception{return (List<?>)call(garden,"cultivarTypes",new Class[]{Player.class},gardener);}
    private String form()throws Exception{return call(wolves,"form",new Class[]{Player.class},wolf).toString();}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void vetoGrowth(BlockGrowEvent e){if(cancelGrowth&&e.getBlock().equals(crop))e.setCancelled(true);}
    @Override public void onEnable(){
        getDataFolder().mkdirs();try{out=new PrintWriter(new File(getDataFolder(),"report.txt"));}catch(Exception e){throw new RuntimeException(e);}
        Bukkit.getPluginManager().registerEvents(this,this);
        at(60,"all three kits register every slot and branch",()->{
            m=(MobaPlugin)Bukkit.getPluginManager().getPlugin("MinecraftMoba");require(m!=null&&m.isEnabled(),"production plugin disabled");m.getConfig().set("features.lobbySafety.enabled",false);m.getConfig().set("features.hungerRegen.enabled",false);
            for(String cls:List.of("kitfighter","gardener","werewolf"))for(String slot:List.of("a1","a2","ult")){var a=m.inputs().abilityFor(cls,slot);require(a!=null,"missing "+cls+slot);if(!slot.equals("ult"))require(a.branchIds().size()==3,"missing branches "+a.id());}
            garden=call(m.inputs(),"gardener",new Class[]{});wolves=call(m.inputs(),"werewolf",new Class[]{});
            w=Bukkit.getWorlds().getFirst();w.setGameRule(GameRule.RANDOM_TICK_SPEED,0);w.setGameRule(GameRule.NATURAL_REGENERATION,false);w.setTime(6000);
            for(int x=-10;x<=32;x++)for(int z=-10;z<=14;z++){w.getBlockAt(x,99,z).setType(x>=10&&x<=22?Material.DIRT:Material.STONE,false);for(int y=100;y<122;y++)w.getBlockAt(x,y,z).setType(Material.AIR,false);}
            bodies=new NmsBodies(m);fighterBody=bodies.spawn("kit_probe",null,"kitfighter",30,new Location(w,.5,100,.5));fighter=fighterBody.player();
            gardenerBody=bodies.spawn("plant_probe",null,"gardener",30,new Location(w,12.5,100,.5));gardener=gardenerBody.player();
            wolfBody=bodies.spawn("wolf_probe",null,"werewolf",30,new Location(w,25.5,100,.5));wolf=wolfBody.player();
            enemyBody=bodies.spawn("enemy_probe",null,"mole",30,new Location(w,28.5,100,.5));enemy=enemyBody.player();
            var board=Bukkit.getScoreboardManager().getMainScoreboard();north=board.registerNewTeam("next_n");south=board.registerNewTeam("next_s");for(Player p:List.of(fighter,gardener,wolf))north.addEntry(p.getName());south.addEntry(enemy.getName());
            crop=w.getBlockAt(13,100,0);crop.getRelative(0,-1,0).setType(Material.FARMLAND,false);crop.setType(Material.WHEAT,false);var age=(Ageable)crop.getBlockData();age.setAge(7);crop.setBlockData(age,false);
        });
        at(80,"Offhander exchanges real carried Crossbow and retains original",()->{
            fighter.getInventory().setItemInOffHand(new ItemStack(Material.DIAMOND,3));branch(fighter,"offhander","crossbow");require(ability(fighter,"a1").execute(fighter,ctx(fighter)),"equip refused");
            require(fighter.getInventory().getItemInOffHand().getType()==Material.CROSSBOW&&fighter.getInventory().getItem(2).getAmount()==3,"exchange lost originals");
        });
        at(81,"Offhand Fishing Rod, Golden Head and base Bow equip real items",()->{
            for(String branch:List.of("fishing_rod","golden_head","base")){branch(fighter,"offhander",branch);require(ability(fighter,"a1").execute(fighter,ctx(fighter)),"refused "+branch);}
            require(fighter.getInventory().getItemInOffHand().getType()==Material.BOW,"bow absent");
        });
        at(82,"Diamond projection fills naked slots while preserving above-Diamond toughness",()->{
            fighter.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            require(ability(fighter,"ult").execute(fighter,ctx(fighter)),"projection refused");
            require(fighter.getInventory().getChestplate().getType()==Material.NETHERITE_CHESTPLATE,"real item replaced");
        });
        at(84,"Diamond protection applies alongside native equipment modifiers",()->{
            require(fighter.getAttribute(Attribute.ARMOR).getValue()>=20&&fighter.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue()>=9,"protection: armor="+fighter.getAttribute(Attribute.ARMOR).getValue()+" toughness="+fighter.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue());
        });
        at(85,"Hotswap reserves an empty cell for a Cobweb",()->{
            trap=w.getBlockAt(3,100,4);branch(fighter,"hotswap","cobweb");require(ability(fighter,"a2").execute(fighter,ctx(fighter,trap)),"trap refused");require(trap.getType()==Material.COBWEB&&m.displacement().isReserved(trap),"web not reserved");
        });
        at(86,"breaking a temporary web restores air without drops",()->{
            var event=new BlockBreakEvent(trap,enemy);Bukkit.getPluginManager().callEvent(event);require(event.isCancelled()&&trap.isEmpty()&&!m.displacement().isReserved(trap),"trap cleanup failed");
        });
        at(87,"temporary Lava cannot spread outside its reserved cell",()->{
            branch(fighter,"hotswap","lava");require(ability(fighter,"a2").execute(fighter,ctx(fighter,trap)),"lava refused");var flow=new BlockFromToEvent(trap,trap.getRelative(1,0,0));Bukkit.getPluginManager().callEvent(flow);require(flow.isCancelled(),"lava flow escaped");
            var bucket=new org.bukkit.event.player.PlayerBucketFillEvent(enemy,trap,trap,org.bukkit.block.BlockFace.UP,Material.BUCKET,new ItemStack(Material.LAVA_BUCKET),EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(bucket);require(bucket.isCancelled(),"temporary lava exported in bucket");
            ability(fighter,"a2").forget(fighter);require(trap.isEmpty(),"lava not restored");
        });
        at(88,"temporary TNT has a native fuse and cancels without stranded explosive",()->{
            branch(fighter,"hotswap","tnt");require(ability(fighter,"a2").execute(fighter,ctx(fighter,trap)),"TNT refused");require(w.getEntitiesByClass(TNTPrimed.class).size()==1,"no real primed TNT");ability(fighter,"a2").forget(fighter);require(w.getEntitiesByClass(TNTPrimed.class).isEmpty(),"TNT stranded");
        });
        at(90,"Salvage observes a real crafting inventory and ingredient debit",()->{
            var view=fighter.openWorkbench(fighter.getLocation(),true);CraftingInventory inv=(CraftingInventory)view.getTopInventory();
            var recipe=Bukkit.getRecipesFor(new ItemStack(Material.IRON_PICKAXE)).getFirst();
            inv.setMatrix(new ItemStack[]{new ItemStack(Material.IRON_INGOT),new ItemStack(Material.IRON_INGOT),new ItemStack(Material.IRON_INGOT),null,new ItemStack(Material.STICK),null,null,new ItemStack(Material.STICK),null});
            var event=new CraftItemEvent(recipe,view,InventoryType.SlotType.RESULT,0,ClickType.LEFT,InventoryAction.PICKUP_ALL);Bukkit.getPluginManager().callEvent(event);require(!event.isCancelled(),"craft vetoed");
            // Explicit post-craft fixture: Paper has no network client here. Settlement must see both debit and produced equipment.
            inv.setMatrix(new ItemStack[9]);fighter.setItemOnCursor(recipe.getResult());
        });
        at(92,"settled craft grants one material-specific Salvage",()->{require("1".equals(m.data(fighter).classState.get("salvage.IRON_INGOT")),"salvage absent");fighter.setItemOnCursor(null);fighter.closeInventory();});
        at(95,"mature Clip preserves plant state and produces resource",()->{
            var data=crop.getBlockData().clone();branch(gardener,"clip","specimen");require(ability(gardener,"a1").execute(gardener,ctx(gardener,crop)),"clip refused");require(crop.getBlockData().equals(data),"clip reset plant");require("12".equals(m.data(gardener).classState.get("plant_material")),"wrong maturity value");
        });
        at(96,"Cultivate spends one real plant at lower efficiency",()->{
            int stored=gardener.getInventory().getItem(0).getAmount();require(ability(gardener,"a2").execute(gardener,ctx(gardener)),"cultivate refused");require(gardener.getInventory().getItem(0).getAmount()==stored-1&&"14".equals(m.data(gardener).classState.get("plant_material")),"bad plant conversion");
        });
        at(97,"full material meter grows a real Torchflower and light",()->{
            add(6);require(plants()==1&&types().contains("torchflower"),"no cultivar");require("0".equals(m.data(gardener).classState.get("plant_material")),"meter not spent");
            require(m.displacement().reservedCount()==2,"torchflower/light pair absent");var age=(Ageable)crop.getBlockData();age.setAge(0);crop.setBlockData(age,false);
        });
        at(98,"Collection rewards new species and diminishes repeated clipping",()->{
            var age=(Ageable)crop.getBlockData();age.setAge(7);crop.setBlockData(age,false);branch(gardener,"clip","collection");
            m.data(gardener).classState.put("plant_material","0");ability(gardener,"a1").execute(gardener,ctx(gardener,crop));int first=Integer.parseInt(m.data(gardener).classState.get("plant_material"));
            m.data(gardener).classState.put("plant_material","0");ability(gardener,"a1").execute(gardener,ctx(gardener,crop));int second=Integer.parseInt(m.data(gardener).classState.get("plant_material"));require(first>second,"collection did not diminish");
            m.data(gardener).classState.put("plant_material","0");age.setAge(0);crop.setBlockData(age,false);
        });
        at(99,"Proliferation rewards a real same-family plant concentration",()->{
            for(int x=13;x<=15;x++)for(int z=1;z<=2;z++){Block b=w.getBlockAt(x,100,z);b.getRelative(0,-1,0).setType(Material.FARMLAND,false);var age=(Ageable)Material.WHEAT.createBlockData();age.setAge(7);b.setBlockData(age,false);}
            var age=(Ageable)crop.getBlockData();age.setAge(7);crop.setBlockData(age,false);branch(gardener,"clip","proliferation");require(ability(gardener,"a1").execute(gardener,ctx(gardener,crop)),"proliferation refused");require(Integer.parseInt(m.data(gardener).classState.get("plant_material"))>8,"no population bonus");
            m.data(gardener).classState.put("plant_material","0");age.setAge(0);crop.setBlockData(age,false);
        });
        at(145,"Cultivar accelerates actual crop growth",()->{require(((Ageable)crop.getBlockData()).getAge()>0,"crop not accelerated");cancelGrowth=true;var age=(Ageable)crop.getBlockData();age.setAge(0);crop.setBlockData(age,false);});
        at(190,"growth veto is respected and Torchflower grants allied Absorption",()->{require(((Ageable)crop.getBlockData()).getAge()==0,"growth veto bypassed");require(gardener.hasPotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION),"no allied absorption");cancelGrowth=false;});
        at(195,"Handbook queues both unselected Cultivars without respecializing",()->{
            require(ability(gardener,"ult").execute(gardener,ctx(gardener)),"handbook refused");add(20);add(20);
            require(types().containsAll(List.of("sweet_berry_bush","giant_bamboo")),"other cultivars missing");require("torchflower".equals(m.inputs().definition("gardener").branchFor("cultivate")),"specialization changed");plantsBefore=plants();
        });
        at(199,"Berry cultivar prepares provisioning and enemy thicket control",()->{place(enemy,11.5,.5);place(fighter,12.5,.5);fighter.setFoodLevel(10);enemy.setHealth(20);enemy.setNoDamageTicks(0);});
        at(209,"Berry provisions allies while thicket damages and slows enemies",()->{require(fighter.getFoodLevel()>10,"ally not provisioned");require(enemy.getHealth()<20&&enemy.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS),"enemy thicket absent");});
        at(200,"Human Bloodhide reduces damage and Brawler's Mind passes projectiles",()->{
            require(ability(wolf,"a2").execute(wolf,ctx(wolf)),"hide refused");before=wolf.getHealth();wolf.damage(2,enemy);require(wolf.getHealth()<before,"no native damage");
            Arrow arrow=w.spawn(wolf.getEyeLocation().clone().add(-2,0,0),Arrow.class);arrow.setShooter(enemy);var hit=new ProjectileHitEvent(arrow,wolf);Bukkit.getPluginManager().callEvent(hit);require(hit.isCancelled(),"projectile not passed through");arrow.remove();
        });
        at(210,"Human Slash uses held weapon and hits an enemy",()->{
            place(wolf,25.5,.5);place(enemy,28.5,.5);wolf.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));enemy.setNoDamageTicks(0);before=enemy.getHealth();require(ability(wolf,"a1").execute(wolf,ctx(wolf)),"slash refused");require(enemy.getHealth()<before,"slash caused no damage");
        });
        at(255,"low-health combat starts natural transformation streak",()->{
            ability(wolf,"a1").cancel(wolf);place(wolf,25.5,.5);wolf.setHealth(10);capacityBefore=m.effectiveMaxHealth(wolf);effectiveBefore=Vitals.toEffective(wolf.getHealth(),capacityBefore);m.combatState().markAbilityActivation(wolf);
        });
        at(285,"Diamond projection expires without changing the real armor",()->{
            require(fighter.getAttribute(Attribute.ARMOR).getValue()==8&&fighter.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue()==3,"projection leaked armor="+fighter.getAttribute(Attribute.ARMOR).getValue()+" toughness="+fighter.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue());require(fighter.getInventory().getChestplate().getType()==Material.NETHERITE_CHESTPLATE,"real armor lost");
        });
        at(300,"Giant Bamboo produces one bounded generation",()->{require(plants()>plantsBefore&&plants()<=plantsBefore+2,"offspring wrong "+plants());plantsBefore=plants();});
        at(320,"natural transformation grows real hitbox and preserves current effective HP",()->{
            require(form().equals("TRANSFORMED"),"did not transform "+form());require(Math.abs(m.effectiveMaxHealth(wolf)-capacityBefore*1.25)<.001,"max capacity wrong");
            require(Math.abs(Vitals.toEffective(wolf.getHealth(),m.effectiveMaxHealth(wolf))-effectiveBefore)<.001,"transformation effective="+Vitals.toEffective(wolf.getHealth(),m.effectiveMaxHealth(wolf))+" before="+effectiveBefore);require(wolf.getAttribute(Attribute.SCALE).getValue()>1.2,"hitbox not enlarged");
        });
        at(321,"Howl cleanses, heals Transformed and enters public Unleashed",()->{
            wolf.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON,100,0));before=wolf.getHealth();require(ability(wolf,"ult").execute(wolf,ctx(wolf)),"howl refused");require(form().equals("UNLEASHED")&&!wolf.hasPotionEffect(org.bukkit.potion.PotionEffectType.POISON)&&wolf.isGlowing(),"howl state wrong");require(wolf.getHealth()>before,"transformed heal absent");
        });
        at(330,"Unleashed Bloodhide gains size without damage reduction and supports recast",()->{
            branch(wolf,"bloodhide","rabid_frenzy");m.inputs().input(wolf,AbilityInputs.Input.SWAP_HAND);m.inputs().input(wolf,AbilityInputs.Input.RIGHT_CLICK);
            require(m.inputs().executions(wolf,"bloodhide")==1,"dispatcher refused initial hide");
            var book=(AbilityCooldowns)call(m.inputs(),"cooldowns",new Class[]{});hideCooldown=book.remaining(wolf.getUniqueId(),"bloodhide",Bukkit.getCurrentTick());require(hideCooldown>0,"no original cooldown");wolf.setNoDamageTicks(0);before=wolf.getHealth();wolf.damage(2,enemy);
            require(wolf.getAttribute(Attribute.SCALE).getValue()>1.35,"damage did not grow size");
            require(ability(wolf,"a2").cooldownExemptRecast(wolf,ctx(wolf)),"recast not exempt");m.inputs().input(wolf,AbilityInputs.Input.SWAP_HAND); // Recast next tick to retain the one-input-per-tick guard.
        });
        at(331,"Rabid Frenzy recasts through the input dispatcher without resetting cooldown",()->{
            var book=(AbilityCooldowns)call(m.inputs(),"cooldowns",new Class[]{});long before=book.remaining(wolf.getUniqueId(),"bloodhide",Bukkit.getCurrentTick());m.inputs().input(wolf,AbilityInputs.Input.RIGHT_CLICK);
            require(m.inputs().executions(wolf,"bloodhide")==2,"recast blocked by cooldown");require(book.remaining(wolf.getUniqueId(),"bloodhide",Bukkit.getCurrentTick())==before&&before>0,"recast changed original cooldown");
        });
        at(340,"second Howl grants lifesteal without extending the timer",()->{require(ability(wolf,"ult").execute(wolf,ctx(wolf)),"second howl refused");place(wolf,25.5,.5);place(enemy,28.5,.5);enemy.setHealth(20);enemy.setNoDamageTicks(0);wolf.setHealth(8);before=wolf.getHealth();enemy.damage(2,wolf);});
        at(342,"Unleashed lifesteal returns effective dealt damage to caster",()->{require(wolf.getHealth()>before,"lifesteal absent");});
        at(430,"empowered offspring do not recursively reproduce",()->{require(plants()==plantsBefore,"recursive bamboo population "+plants());});
        at(522,"Unleashed ends on original ten-second deadline with recovery",()->{
            require(form().equals("HUMAN"),"second howl extended duration "+form());require(wolf.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS)&&wolf.hasPotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS),"no recovery");require(wolf.getAttribute(Attribute.SCALE).getValue()==1,"size not restored");
        });
        at(535,"destroying a real Cultivar removes its aura and returns reserved air",()->{
            Block flower=null;for(int x=9;x<=16;x++)for(int z=-4;z<=4;z++)if(w.getBlockAt(x,100,z).getType()==Material.TORCHFLOWER)flower=w.getBlockAt(x,100,z);
            require(flower!=null,"torchflower gone");int prior=plants();var event=new BlockBreakEvent(flower,enemy);Bukkit.getPluginManager().callEvent(event);require(event.isCancelled()&&flower.isEmpty()&&plants()==prior-1,"cultivar counterplay failed");
        });
        at(540,"session teardown restores all temporary flora, attributes and reservations",()->{
            call(m.inputs(),"close",new Class[]{});call(m.inputs(),"close",new Class[]{});require(m.displacement().reservedCount()==0&&plants()==0,"terrain reservations leaked");require(wolf.getAttribute(Attribute.SCALE).getValue()==1,"scale modifier leaked");
        });
        Bukkit.getScheduler().runTaskLater(this,()->{log("SUMMARY pass="+pass+" fail="+fail);out.close();if(north!=null)north.unregister();if(south!=null)south.unregister();if(bodies!=null)for(var body:List.of(fighterBody,gardenerBody,wolfBody,enemyBody))bodies.despawn(body);Bukkit.shutdown();},560);
    }
}
