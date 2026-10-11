package kf;

import com.minecraftmoba.plugin.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Disposable-server acceptance. Runs actual Paper inventories, recipes, entities and listeners. */
public final class KitAcceptance extends JavaPlugin {
    private MobaPlugin m; private NmsBodies bodies; private Bodies.Body chefBody, victimBody;
    private Player chef, victim; private World w; private PrintWriter out; private int pass, fail;
    private Object cooking; private Ability buffet; private double before;
    private org.bukkit.scoreboard.Team a, b;
    private Wolf wolf; private Ability sinkhole; private Location holeAt;
    private interface Action { void run() throws Exception; }
    private void at(int tick, String name, Action action) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try { action.run(); pass++; log("PASS " + name); }
            catch (Throwable e) { fail++; log("FAIL " + name + " :: " + e); e.printStackTrace(out); out.flush(); }
        }, tick);
    }
    private void log(String s) { out.println(s); out.flush(); getLogger().info(s); }
    private void require(boolean condition, String detail) { if (!condition) throw new IllegalStateException(detail); }
    private Object call(Object obj, String method, Class<?>[] types, Object... args) throws Exception {
        var f = obj.getClass().getDeclaredMethod(method, types); f.setAccessible(true); return f.invoke(obj,args);
    }
    private Ability.AbilityContext ctx(Player p) { return new Ability.AbilityContext(m,m.provenance(),m.inputs(),m.inputs().definition(m.data(p).classId),m.data(p)); }
    private Ability ability(Player p, String slot) { return m.inputs().abilityFor(m.data(p).classId,slot); }
    private void branch(String ability, String branch) { m.data(chef).classState.put("branch."+ability, branch); }
    private void place(Player p, double x, double z) { p.teleport(new Location(w,x,100,z,-90,0)); p.setVelocity(new org.bukkit.util.Vector()); }
    private void hotbar(Player p) { p.getInventory().setItem(0,new ItemStack(Material.DIAMOND_SWORD)); p.getInventory().setItem(8,new ItemStack(Material.APPLE,7)); }
    private boolean seated() throws Exception { return (boolean)call(buffet,"seated",new Class[]{Player.class},victim); }
    private void rollout() { place(chef,0.5,.5); place(victim,5.5,.5); chef.getInventory().setItem(1,new ItemStack(Material.COOKED_BEEF,64)); require(buffet.execute(chef,ctx(chef)),"Buffet refused"); }

    @Override public void onEnable() {
        getDataFolder().mkdirs();
        try { out = new PrintWriter(new File(getDataFolder(),"report.txt")); } catch(Exception e) { throw new RuntimeException(e); }
        at(60,"production plugin and all four kits registered", () -> {
            m=(MobaPlugin)Bukkit.getPluginManager().getPlugin("MinecraftMoba"); require(m!=null && m.isEnabled(),"plugin disabled");
            m.getConfig().set("features.lobbySafety.enabled", false); // isolated ability fixture outside a running match
            for(String id:List.of("chef","mole","lightfooted","daredevil"))
                for(String slot:List.of("a1","a2","ult")) require(m.inputs().abilityFor(id,slot)!=null,id+" missing "+slot);
            w=Bukkit.getWorlds().getFirst();
            for(int x=-16;x<=25;x++) for(int z=-10;z<=10;z++) {
                w.getBlockAt(x,99,z).setType(Material.STONE,false);
                for(int y=100;y<120;y++) w.getBlockAt(x,y,z).setType(Material.AIR,false);
            }
            bodies=new NmsBodies(m);
            chefBody=bodies.spawn("chef_probe",null,"chef",30,new Location(w,.5,100,.5)); chef=chefBody.player();
            victimBody=bodies.spawn("victim_probe",null,"mole",30,new Location(w,5.5,100,.5)); victim=victimBody.player();
            var board=Bukkit.getScoreboardManager().getMainScoreboard();
            a=board.registerNewTeam("kit_a"); b=board.registerNewTeam("kit_b"); a.addEntry(chef.getName()); b.addEntry(victim.getName());
            var c=m.getClass().getClassLoader().loadClass("com.minecraftmoba.plugin.ChefCooking").getDeclaredConstructor(MobaPlugin.class); c.setAccessible(true); cooking=c.newInstance(m);
            buffet=ability(chef,"ult"); hotbar(victim);
        });
        at(80,"Furnace carryover finishes one item and advances the next with paid fuel", () -> {
            Block block=w.getBlockAt(0,100,5); block.setType(Material.FURNACE,false); Furnace f=(Furnace)block.getState();
            f.getSnapshotInventory().setSmelting(new ItemStack(Material.BEEF,8)); f.setCookTime((short)140); f.setCookTimeTotal(200); f.setBurnTime((short)1000); f.update(true,false);
            int advanced=(int)call(cooking,"advance",new Class[]{Block.class,int.class,double.class,double.class,double.class},block,160,1.,0.,2.);
            f=(Furnace)block.getState();
            require(advanced==160 && f.getInventory().getSmelting().getAmount()==7 && f.getInventory().getResult().getType()==Material.COOKED_BEEF
                    && f.getCookTime()==100 && f.getBurnTime()==840,"bad carryover: "+advanced+" progress="+f.getCookTime()+" burn="+f.getBurnTime());
        });
        at(81,"Rake the Coals pays half fuel and never invents fuel", () -> {
            Block block=w.getBlockAt(0,100,5); Furnace f=(Furnace)block.getState(); f.setCookTime((short)0); f.setBurnTime((short)100); f.update(true,false);
            int advanced=(int)call(cooking,"advance",new Class[]{Block.class,int.class,double.class,double.class,double.class},block,300,.5,0.,2.);
            require(advanced==200 && ((Furnace)block.getState()).getBurnTime()==0,"conservation wrong "+advanced);
        });
        at(82,"Campfire finishes independent slots immediately", () -> {
            Block block=w.getBlockAt(2,100,5); block.setType(Material.CAMPFIRE,false); Campfire c=(Campfire)block.getState();
            c.setItem(0,new ItemStack(Material.BEEF)); c.setCookTime(0,550); c.setCookTimeTotal(0,600);
            c.setItem(1,new ItemStack(Material.BEEF)); c.setCookTime(1,0); c.setCookTimeTotal(1,600); c.update(true,false);
            call(cooking,"advance",new Class[]{Block.class,int.class,double.class,double.class,double.class},block,100,1.,0.,2.);
            c=(Campfire)block.getState(); require(c.getItem(0)==null || c.getItem(0).isEmpty(),"slot 0 did not finish"); require(c.getCookTime(1)==100,"slot 1 advanced incorrectly");
        });
        at(90,"Food Fight launches a real stored food", () -> { place(chef,.5,.5); place(victim,5.5,.5); before=victim.getHealth(); branch("food_fight","extra_spicy"); require(ability(chef,"a1").execute(chef,ctx(chef)),"projectile refused"); });
        at(110,"Food Fight damages enemy and Extra Spicy ignites", () -> { require(victim.getHealth()<before,"enemy took no damage"); require(victim.getFireTicks()>0,"no fire"); victim.setFireTicks(0); });
        at(120,"Super Nutritious launches toward an ally", () -> { b.removeEntry(victim.getName()); a.addEntry(victim.getName()); place(chef,.5,.5); place(victim,5.5,.5); victim.setHealth(5); before=victim.getHealth(); branch("food_fight","super_nutritious"); require(ability(chef,"a1").execute(chef,ctx(chef)),"heal projectile refused"); });
        at(140,"Super Nutritious heals without damaging ally", () -> { require(victim.getHealth()>before,"ally not healed"); a.removeEntry(victim.getName()); b.addEntry(victim.getName()); });
        at(150,"Buffet starts and leaves Chef free to act", this::rollout);
        at(170,"Buffet mounts victim, protects originals and spends nine real servings", () -> {
            require(seated() && victim.isInsideVehicle(),"victim not seated"); require(victim.getInventory().getItem(0).getType()==Material.COOKED_BEEF,"not a serving");
            require(victim.getPersistentDataContainer().has(new NamespacedKey(m,"buffet_hotbar"),PersistentDataType.BYTE_ARRAY),"no durable originals");
            require(chef.getInventory().getItem(1).getAmount()==55,"servings not paid");
            m.lockedSlots().refresh(victim); require(victim.getInventory().getItem(8).getType()==Material.COOKED_BEEF,"capacity overwrote serving");
            require(!m.inputs().input(victim,AbilityInputs.Input.SWAP_HAND),"seated victim entered ability mode");
        });
        at(175,"Dismount restores original hotbar exactly once", () -> { victim.leaveVehicle(); require(!seated(),"still seated"); require(victim.getInventory().getItem(0).getType()==Material.DIAMOND_SWORD,"sword lost"); require(victim.getInventory().getItem(8).getAmount()==7,"apples lost"); });
        at(180,"second invitation", this::rollout);
        at(200,"Death event preserves original items and exports no temporary serving", () -> {
            require(seated(),"not seated before death");
            var drops=new ArrayList<ItemStack>(); for(int i=0;i<9;i++) drops.add(victim.getInventory().getItem(i).clone());
            var event=new PlayerDeathEvent(victim,org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build(),drops,0,net.kyori.adventure.text.Component.empty(),false);
            event.setKeepInventory(false); Bukkit.getPluginManager().callEvent(event);
            require(!seated(),"death did not restore"); require(event.getDrops().stream().noneMatch(s->s.getType()==Material.COOKED_BEEF),"servings leaked into drops");
            require(event.getItemsToKeep().stream().anyMatch(s->s.getType()==Material.DIAMOND_SWORD),"original sword not kept");
            require(victim.getInventory().getItem(0).getType()==Material.DIAMOND_SWORD,"live inventory not restored");
        });
        at(210,"third invitation", this::rollout);
        at(230,"Real disconnect/despawn restores suspended hotbar", () -> { require(seated(),"not seated"); bodies.despawn(victimBody); require(victim.getInventory().getItem(0).getType()==Material.DIAMOND_SWORD,"quit lost sword"); require(!seated(),"quit left seat"); });
        at(240,"Sinkhole preserves container state and reserves its origin", () -> {
            Block chest=w.getBlockAt(4,100,5); chest.setType(Material.CHEST,false); ((Chest)chest.getState()).getBlockInventory().setItem(0,new ItemStack(Material.DIAMOND,3));
            UUID token=UUID.randomUUID(); require(m.displacement().take(chest,token)!=null,"take refused"); require(chest.isEmpty() && m.displacement().isReserved(chest),"no reservation");
            m.displacement().restore(token); require(chest.getType()==Material.CHEST && ((Chest)chest.getState()).getBlockInventory().getItem(0).getAmount()==3,"container content lost"); require(!m.displacement().isReserved(chest),"reservation leaked");
        });
        at(440,"Lab kit cleanup removes projectiles, tables and reservations", () -> { m.inputs().forget(chef); bodies.despawn(chefBody); require(m.displacement().reservedCount()==0,"reservation remains"); });
        at(250,"Mole casts the actual temporary Sinkhole", () -> {
            m.inputs().forget(chef); m.applyDraftedClass(chef,"mole"); place(chef,.5,.5);
            chef.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE)); chef.setRotation(-90,65);
            holeAt=chef.getTargetBlockExact(24).getLocation(); sinkhole=ability(chef,"ult");
            require(sinkhole.execute(chef,ctx(chef)),"Sinkhole refused");
        });
        at(270,"Sinkhole opens only after telegraph and holds reservations", () -> {
            require(m.displacement().reservedCount()>0 && holeAt.getBlock().isEmpty(),"Sinkhole did not open");
            sinkhole.cancel(chef); require(m.displacement().reservedCount()==0 && holeAt.getBlock().getType()==Material.STONE,"cancel did not restore original terrain");
        });
        at(280,"Lightfooted casts protected animal and caster leaps", () -> {
            m.applyDraftedClass(chef,"lightfooted"); place(chef,.5,.5); chef.setRotation(-90,65);
            wolf=w.spawn(new Location(w,1.5,100,.5),Wolf.class); wolf.setAI(false);
            require(ability(chef,"ult").execute(chef,ctx(chef)),"Rabbit ultimate refused"); require(wolf.isInvulnerable(),"animal not protected");
        });
        at(345,"Forced animal leap lands and releases protection", () -> { require(!wolf.isInvulnerable(),"animal remained invulnerable"); require(!chef.isDead(),"caster died during leap"); wolf.remove(); });
        at(350,"full-engine death fixture seats a fresh victim", () -> {
            m.applyDraftedClass(chef,"chef");
            victimBody=bodies.spawn("engine_death",null,"mole",30,new Location(w,5.5,100,.5)); victim=victimBody.player();
            b.addEntry(victim.getName()); hotbar(victim); rollout();
        });
        at(370,"actual death while seated releases custody", () -> {
            require(seated(),"not seated before real death"); w.setGameRule(GameRule.KEEP_INVENTORY,false);
            victim.setHealth(0); require(!seated(),"actual death left a seat");
        });
        at(385,"respawn after actual Buffet death", () -> victim.spigot().respawn());
        at(410,"actual death/respawn keeps originals once and exports no servings", () -> {
            long swords=Arrays.stream(victim.getInventory().getStorageContents()).filter(Objects::nonNull).filter(i->i.getType()==Material.DIAMOND_SWORD).mapToInt(ItemStack::getAmount).sum();
            long apples=Arrays.stream(victim.getInventory().getStorageContents()).filter(Objects::nonNull).filter(i->i.getType()==Material.APPLE).mapToInt(ItemStack::getAmount).sum();
            require(!victim.isDead() && swords==1 && apples==7,"real death custody: sword="+swords+" apples="+apples+" dead="+victim.isDead());
            require(Arrays.stream(victim.getInventory().getStorageContents()).filter(Objects::nonNull).noneMatch(i->i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(m,"buffet_serving"),PersistentDataType.BYTE)),"serving exported through respawn");
        });
        Bukkit.getScheduler().runTaskLater(this, () -> { log("SUMMARY pass="+pass+" fail="+fail); out.close(); if(a!=null)a.unregister(); if(b!=null)b.unregister(); bodies.despawn(chefBody); bodies.despawn(victimBody); Bukkit.shutdown(); },470);
    }
}
