// Reusable scenarios for a connected Mineflayer 4.39.0 bot on the isolated fixture.
// `cmd` sends an operator fixture command and waits for the server response;
// `action` sends ServerboundPlayerAction; `sleep` waits for ticks to elapse.
// Caller must serialize scenarios and preserve server/client logs.
exports.fiftyLunges = async ({bot,cmd,action,sleep}) => {
  await cmd('moba setclass MobaTest test');
  await bot.look(0,0,true);
  for (let i=0;i<50;i++) {
    action(6); await sleep(100); bot.swingArm('right');
    await sleep(100); action(6); await sleep(500);
  }
  await cmd('moba debug MobaTest');
  action(6); await sleep(2800); await cmd('moba debug MobaTest');
};
exports.fiftyUltimates = async ({cmd,action,sleep}) => {
  await cmd('moba setclass MobaTest test');
  for (let i=0;i<50;i++) {
    action(6); await sleep(100); action(4);
    await sleep(100); action(6); await sleep(500);
  }
  await cmd('moba debug MobaTest');
};
exports.inventoryAndUnlock = async ({bot,cmd,sleep}) => {
  await cmd('moba reset MobaTest');
  for(let i=0;i<6;i++) await cmd(`item replace entity @s hotbar.${i} with minecraft:stone 64`);
  await cmd('summon item ~ ~ ~ {Item:{id:"minecraft:dirt",count:64},PickupDelay:0s}');
  await sleep(1000); await cmd('data get entity @s Inventory');
  await bot.clickWindow(36,0,0); await sleep(150);
  await bot.clickWindow(42,0,0); await sleep(250);
  bot.closeWindow(bot.inventory); await sleep(250);
  await cmd('data get entity @s Inventory');
  await bot.clickWindow(45,0,0); await sleep(150);
  await bot.clickWindow(45,0,2); await sleep(150);
  await cmd('data get entity @s Inventory');
  bot.closeWindow(bot.inventory);
  await cmd('moba setlevel MobaTest 2'); await sleep(1200);
  await cmd('data get entity @s Inventory');
};
exports.sinkholeProtection = async ({bot,cmd,action,sleep,Vec3}) => {
  bot.setQuickBarSlot(0);
  await bot.placeBlock(bot.blockAt(new Vec3(3,-60,-5)),new Vec3(0,1,0));
  await sleep(300); await cmd('moba provenance');
  await bot.lookAt(new Vec3(3.5,-59,-4.5),true);
  action(6); await sleep(100);
  await bot.activateBlock(bot.blockAt(new Vec3(3,-59,-5)));
  await sleep(1800); await cmd('moba debug MobaTest');
  await cmd('execute if block 3 -59 -5 minecraft:stone run say PLAYER_BLOCK_SURVIVED');
  await cmd('execute if block 3 -60 -5 minecraft:air run say NATURAL_BLOCK_REMOVED');
};

// --- added 2026-09-17: provenance, death/reconnect, duplicate-input, rewards, mob targeting ---

// #1 Provenance. Places three player blocks INSIDE the blast volume and
// asserts each survives while natural neighbours go. Reads playerPlacedExcluded.
exports.sinkholeProvenance = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('tp MobaTest 2.5 -59 -5.5 0 0');                   // within reach of the build site
  await sleep(600);
  await cmd('fill 0 -59 -8 6 -54 -2 minecraft:air');           // clear from the build layer up
  await cmd('fill 0 -60 -8 6 -60 -2 minecraft:stone');          // natural floor (server-set, unmarked)
  await cmd('item replace entity @s hotbar.0 with minecraft:stone 64');
  await sleep(400);
  bot.setQuickBarSlot(0); await sleep(300);
  for (const [x,z] of [[3,-5],[4,-5],[3,-4]]) {                 // player-placed, marked by BlockPlaceEvent
    await bot.placeBlock(bot.blockAt(new Vec3(x,-60,z)), new Vec3(0,1,0));
    await sleep(250);
  }
  await cmd('say PROVENANCE_SETUP_DONE');
  await bot.lookAt(new Vec3(3.5,-59,-4.5), true);
  action(6); await sleep(120);
  await bot.activateBlock(bot.blockAt(new Vec3(3,-59,-5)));
  await sleep(2500);
  for (const [x,z] of [[3,-5],[4,-5],[3,-4]])
    await cmd(`execute if block ${x} -59 ${z} minecraft:stone run say PLACED_SURVIVED_${x}_${z}`);
  for (const [x,z] of [[5,-5],[3,-3],[2,-5]])
    await cmd(`execute if block ${x} -60 ${z} minecraft:air run say NATURAL_REMOVED_${x}_${z}`);
  await cmd('moba provenance');
};

// #2 Death while ability mode is active, then reconnect. Watches for item loss,
// a stuck mode flag, and whether the offhand map survives keepInventory.
exports.deathInMode = async ({bot,cmd,action,sleep}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('data get entity @s Inventory');
  await cmd('moba debug MobaTest');
  action(6); await sleep(200);
  await cmd('say DEATH_WITH_MODE_ACTIVE');
  await cmd('kill @s'); await sleep(1500);
  await cmd('moba debug MobaTest');            // mode should be cleared, not stuck
  await cmd('data get entity @s Inventory');   // keepInventory: slots should be identical
  action(6); await sleep(150); bot.swingArm('right'); await sleep(150); action(6);
  await sleep(400);
  await cmd('moba debug MobaTest');            // abilities must still work after death
};

// #3 Duplicate ultimate input inside ONE mode window. 50 sequential ults already
// pass; this is the debounce case. Counter must rise by far less than 10.
exports.duplicateUltInput = async ({cmd,action,sleep}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('say DUPLICATE_ULT_BASELINE');
  await cmd('moba debug MobaTest');
  action(6); await sleep(120);
  for (let i=0;i<10;i++) { action(4); await sleep(30); }   // 10 rapid drops, one window
  await sleep(1200);
  await cmd('say DUPLICATE_ULT_AFTER');
  await cmd('moba debug MobaTest');
  action(6); await sleep(200);
  await cmd('moba debug MobaTest');
};

// #5a Reward GUI. Requires rewards.levels populated in the fixture config.
// Asserts the menu never yields items and that a choice persists.
exports.rewardGui = async ({bot,cmd,sleep}) => {
  await cmd('moba reset MobaTest'); await sleep(400);
  await cmd('moba debug MobaTest');
  await cmd('moba setlevel MobaTest 3'); await sleep(600);
  await cmd('say REWARD_PENDING_EXPECTED');
  await cmd('moba debug MobaTest');
  await cmd('moba rewards'); await sleep(800);
  const win = bot.currentWindow;
  await cmd('say REWARD_WINDOW_' + (win ? 'OPEN_' + win.slots.length : 'ABSENT'));
  if (win) {
    await bot.clickWindow(0,0,0); await sleep(500);
    bot.closeWindow(win); await sleep(400);
  }
  await cmd('data get entity @s Inventory');   // must contain no reward item
  await cmd('moba debug MobaTest');            // choice should be recorded
};

// Actual entity packets, not air swings or block interaction.
exports.entityPackets = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('difficulty easy');
  await cmd('gamerule minecraft:spawn_mobs false');
  await cmd('moba setclass MobaTest test');
  await cmd('fill 18 -60 18 26 -60 26 stone');
  await cmd('tp @s 20.5 -59 20.5');
  await cmd('kill @e[tag=moba_entity_fixture]');
  await cmd('summon husk 22.5 -59 20.5 {Tags:["moba_entity_fixture"],NoAI:1b,PersistenceRequired:1b,Health:20f}');
  await sleep(600);
  const target=Object.values(bot.entities).find(e=>e.name==='husk');
  if(!target) throw new Error('Fixture husk not received');
  await bot.lookAt(new Vec3(22.5,-58,20.5),true);
  await cmd('say ENTITY_BASELINE'); await cmd('moba debug MobaTest');
  action(6); await sleep(150); bot.attack(target); await sleep(300); action(6);
  await cmd('execute as @e[tag=moba_entity_fixture,limit=1] if entity @s[nbt={Health:20.0f}] run say ENTITY_HEALTH_UNCHANGED');
  await cmd('data get entity @e[tag=moba_entity_fixture,limit=1] Health');
  await cmd('moba debug MobaTest');
  await bot.lookAt(new Vec3(22.5,-59.8,20.5),true);
  action(6); await sleep(150); await bot.activateEntity(target); await sleep(1200); action(6);
  await cmd('say ENTITY_AFTER'); await cmd('moba debug MobaTest');
};
exports.fiftySinkholes = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('tp @s 40.5 -59 40.5');
  await cmd('setblock 40 -60 40 bedrock');
  await cmd('say SINKHOLE_50_BASELINE'); await cmd('moba debug MobaTest');
  for(let i=0;i<50;i++) {
    await cmd('fill 42 -60 38 48 -60 44 stone');
    await bot.lookAt(new Vec3(44.5,-60,40.5),true);
    action(6); await sleep(100);
    await bot.activateBlock(bot.blockAt(new Vec3(44,-60,40)));
    await sleep(800); action(6); await sleep(100);
  }
  await cmd('say SINKHOLE_50_AFTER'); await cmd('moba debug MobaTest');
};
exports.rewardReset = async ({bot,cmd,sleep}) => {
  await cmd('moba reset MobaTest');
  await cmd('say RESET_BASELINE'); await cmd('moba debug MobaTest');
  await cmd('moba setlevel MobaTest 3'); await cmd('moba rewards');
  await sleep(400);
  for(let i=0;i<2;i++) {
    if(!bot.currentWindow) throw new Error('Expected pending reward menu '+i);
    await bot.clickWindow(0,0,0); await sleep(500);
  }
  await cmd('say TWO_CHOICES'); await cmd('moba debug MobaTest');
  await cmd('data get entity @s Inventory');
  await cmd('moba reset MobaTest'); await sleep(1200);
  await cmd('say RESET_AFTER_CHOICES'); await cmd('moba debug MobaTest');
  await cmd('data get entity @s foodLevel');
  await cmd('data get entity @s Inventory');
};
exports.quitInMode = async ({bot,cmd,action,sleep}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('say QUIT_BASELINE'); await cmd('data get entity @s Inventory');
  action(6); await sleep(200); await cmd('moba debug MobaTest');
  bot.quit('acceptance mode reset on reconnect');
};
exports.reconnectState = async ({bot,cmd,log}) => {
  await cmd('say RECONNECTED_STATE'); await cmd('moba debug MobaTest');
  await cmd('data get entity @s Inventory');
  log('reconnectInventory',{offhand:bot.inventory.slots[45],held:bot.heldItem});
};
exports.entityInteraction = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('difficulty peaceful');
  await cmd('moba setclass MobaTest test');
  await cmd('fill 58 -60 58 66 -60 66 stone');
  await cmd('tp @s 60.5 -59 60.5');
  await cmd('item replace entity @s hotbar.0 with red_dye 3');
  bot.setQuickBarSlot(0);
  await cmd('summon sheep 62.5 -59 60.5 {Tags:["moba_sheep_fixture"],NoAI:1b,PersistenceRequired:1b,Color:0b}');
  await sleep(400);
  const target=Object.values(bot.entities).find(e=>e.name==='sheep' && e.position.distanceTo(new Vec3(62.5,-59,60.5))<1);
  if(!target) throw new Error('Fixture sheep not received');
  await bot.lookAt(new Vec3(62.5,-59.8,60.5),true);
  await cmd('moba debug MobaTest');
  action(6); await sleep(150); await bot.activateEntity(target); await sleep(300); action(6);
  await cmd('execute as @e[tag=moba_sheep_fixture,limit=1] if entity @s[nbt={Color:0b}] run say SHEEP_NOT_DYED');
  await cmd('data get entity @s Inventory'); await cmd('moba debug MobaTest');
};

exports.fixtureEvents = async ({bot,cmd}) => {
  await cmd(`moba setlevel ${bot.username} 1`);
  await cmd('mobafixture transfer-partial');
  await cmd(`moba setlevel ${bot.username} 30`);
  await cmd('mobafixture transfer-full');
  await cmd(`moba setlevel ${bot.username} 1`);
  await cmd('mobafixture denied-place');
};
exports.dropOrdering = async ({bot,cmd,action,sleep}) => {
  const name=bot.username;
  await cmd(`moba setclass ${name} test`);
  await cmd('item replace entity @s hotbar.0 with stone 3');
  await cmd('item replace entity @s hotbar.1 with dirt 3');
  bot.setQuickBarSlot(1);action(4); // held-slot and drop packets must retain native order
  await sleep(300);await cmd('data get entity @s Inventory');
  action(6);action(4);action(3); // immediate F/Q/Ctrl-Q: no held item may leave inventory
  await sleep(300);await cmd('data get entity @s Inventory');
  await cmd(`moba debug ${name}`);
  action(6);await sleep(300);
};

exports.rewardPendingBefore = async ({bot,cmd,sleep}) => {
  await cmd(`moba reset ${bot.username}`);
  await cmd(`moba xp ${bot.username} 250`);
  await cmd(`moba debug ${bot.username}`);
  await cmd('moba rewards');await sleep(400);
  if(!bot.currentWindow) throw new Error('Pending reward GUI missing before logout');
  bot.closeWindow(bot.currentWindow);await sleep(200);
  bot.quit('pending rewards reconnect check');
};
exports.rewardPendingAfter = async ({bot,cmd,sleep,log}) => {
  await cmd(`moba debug ${bot.username}`);
  log('nativeXp',bot.experience);
  if(bot.experience.level!==3 || Math.abs(bot.experience.progress-.5)>.001) throw new Error('Native XP did not persist at level 3 / half bar');
  await cmd('moba rewards');await sleep(400);
  if(!bot.currentWindow) throw new Error('Pending reward GUI missing after logout');
  for(let i=0;i<2;i++) {
    if(!bot.currentWindow) throw new Error('Missing pending reward '+i);
    await bot.clickWindow(0,0,0);await sleep(500);
    await cmd(`moba debug ${bot.username}`);
  }
  await cmd('data get entity @s Inventory');
  log('pass','Pending rewards, native XP, and immediate capacity updates survive reconnect');
};

exports.sinkholeArch = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd(`moba setclass ${bot.username} test`);
  await cmd('tp @s 2.5 -59 -5.5');await sleep(400);
  await cmd('fill 0 -59 -8 6 -54 -2 air');
  await cmd('fill 0 -60 -8 6 -60 -2 stone');
  await cmd('item replace entity @s hotbar.0 with stone 64');
  bot.setQuickBarSlot(0);await sleep(300);
  const placed=[];
  for(const x of [3,5]) for(let y=-59;y<=-57;y++) {
    await bot.placeBlock(bot.blockAt(new Vec3(x,y-1,-5)),new Vec3(0,1,0));
    placed.push([x,y,-5]);await sleep(200);
  }
  await bot.placeBlock(bot.blockAt(new Vec3(3,-57,-5)),new Vec3(1,0,0));placed.push([4,-57,-5]);
  await sleep(250);action(6);await sleep(100);
  await bot.activateBlock(bot.blockAt(new Vec3(3,-59,-5)));await sleep(2000);
  for(const [x,y,z] of placed) await cmd(`execute if block ${x} ${y} ${z} stone run say ARCH_SURVIVED_${x}_${y}_${z}`);
  for(const [x,z] of [[3,-5],[4,-5],[3,-4]]) await cmd(`execute if block ${x} -60 ${z} air run say ARCH_TERRAIN_REMOVED_${x}_${z}`);
  await cmd(`moba debug ${bot.username}`);await cmd('moba provenance');
};

exports.archAfterRestart = async ({bot,cmd,action,sleep,Vec3,log}) => {
  await cmd(`moba debug ${bot.username}`);
  if(bot.experience.level!==3 || Math.abs(bot.experience.progress-.5)>.001) throw new Error('Progression did not survive full server restart');
  await cmd('moba provenance');
  await cmd('fill 0 -60 -8 6 -60 -2 stone');
  await cmd('tp @s 2.5 -59 -5.5');await sleep(500);
  action(6);await sleep(100);await bot.activateBlock(bot.blockAt(new Vec3(3,-59,-5)));await sleep(2000);
  for(const [x,y,z] of [[3,-59,-5],[3,-58,-5],[3,-57,-5],[5,-59,-5],[5,-58,-5],[5,-57,-5],[4,-57,-5]])
    await cmd(`execute if block ${x} ${y} ${z} stone run say PERSISTED_ARCH_${x}_${y}_${z}`);
  await cmd('execute if block 3 -60 -5 air run say PERSISTED_TERRAIN_COLLAPSED');
  await cmd('moba provenance');
  log('checked','Inspect seven persisted arch markers, terrain collapse, and placeCount=0 after server restart');
};

// ---- Regenerative Sources ----
// These assert the seam's invariant: renewal restores availability at a place
// and grants nothing to anyone. Requires the fixture source `fixture_patch`
// (CROP) and `fixture_herd` (ANIMAL) in the acceptance config.

const SRC = {x: 40, y: -60, z: 40};   // fixture source centre, radius 4

// #10 Harvesting wild crops inside a source decrements its availability.
exports.renewableHarvest = async ({bot,cmd,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd(`tp MobaTest ${SRC.x}.5 ${SRC.y + 1} ${SRC.z - 2}.5 0 0`);
  await sleep(600);
  await cmd(`fill ${SRC.x-2} ${SRC.y} ${SRC.z-2} ${SRC.x+2} ${SRC.y} ${SRC.z+2} minecraft:farmland`);
  // Server-set, so unmarked by BlockPlaceEvent: a wild patch, not a player farm.
  await cmd(`fill ${SRC.x-2} ${SRC.y+1} ${SRC.z-2} ${SRC.x+2} ${SRC.y+1} ${SRC.z+2} minecraft:wheat[age=7]`);
  await sleep(500);
  await cmd('say RENEWABLE_BEFORE'); await cmd('moba renewables');
  await cmd('gamemode survival MobaTest'); await sleep(400);
  for (const [dx,dz] of [[0,0],[1,0],[0,1]]) {
    await bot.dig(bot.blockAt(new Vec3(SRC.x+dx, SRC.y+1, SRC.z+dz)));
    await sleep(400);
  }
  await cmd('say RENEWABLE_AFTER'); await cmd('moba renewables');
};

// #11 A player's own crops inside the same volume must NOT count as wild harvest.
exports.renewableFarmNotWild = async ({bot,cmd,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd(`tp MobaTest ${SRC.x}.5 ${SRC.y + 1} ${SRC.z - 2}.5 0 0`);
  await sleep(600);
  await cmd(`fill ${SRC.x-2} ${SRC.y+1} ${SRC.z-2} ${SRC.x+2} ${SRC.y+1} ${SRC.z+2} minecraft:air`);
  // Walking on farmland tramples it back to dirt, and seeds then refuse to
  // place. Re-lay the farmland after the player has already moved.
  await cmd(`fill ${SRC.x-2} ${SRC.y} ${SRC.z-2} ${SRC.x+2} ${SRC.y} ${SRC.z+2} minecraft:farmland`);
  await cmd('gamerule mobGriefing false');
  await cmd('gamemode survival MobaTest');
  await cmd('item replace entity @s hotbar.0 with minecraft:wheat_seeds 64');
  await sleep(400);
  bot.setQuickBarSlot(0); await sleep(300);
  await cmd('say FARM_BEFORE'); await cmd('moba renewables');
  // Placed by the player, so marked; breaking them must not decrement.
  for (const [dx,dz] of [[0,0],[1,0]]) {
    await bot.placeBlock(bot.blockAt(new Vec3(SRC.x+dx, SRC.y, SRC.z+dz)), new Vec3(0,1,0));
    await sleep(350);
    // Prove the crop really exists before breaking it: "availability unchanged"
    // is also what a scenario that placed nothing would report.
    await cmd(`execute if block ${SRC.x+dx} ${SRC.y+1} ${SRC.z+dz} minecraft:wheat run say FARM_PLACED_${dx}_${dz}`);
  }
  for (const [dx,dz] of [[0,0],[1,0]]) {
    const b = bot.blockAt(new Vec3(SRC.x+dx, SRC.y+1, SRC.z+dz));
    if (b && b.name !== 'air') { await bot.dig(b); await sleep(350); }
    await cmd(`execute if block ${SRC.x+dx} ${SRC.y+1} ${SRC.z+dz} minecraft:air run say FARM_BROKEN_${dx}_${dz}`);
  }
  await cmd('say FARM_AFTER'); await cmd('moba renewables');
};

// #12 Depletion then recovery. The assertion that matters is that recovery
// restores availability while granting nothing: inventory before and after,
// and grantedByRenewal, are both printed.
exports.renewableRecovery = async ({bot,cmd,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd(`tp MobaTest ${SRC.x}.5 ${SRC.y + 1} ${SRC.z - 2}.5 0 0`);
  await sleep(600);
  await cmd(`fill ${SRC.x-2} ${SRC.y} ${SRC.z-2} ${SRC.x+2} ${SRC.y} ${SRC.z+2} minecraft:farmland`);
  await cmd(`fill ${SRC.x-2} ${SRC.y+1} ${SRC.z-2} ${SRC.x+2} ${SRC.y+1} ${SRC.z+2} minecraft:wheat[age=7]`);
  await cmd('gamemode survival MobaTest'); await sleep(500);
  // Drain to zero; fixture capacity is small on purpose.
  for (const [dx,dz] of [[0,0],[1,0],[0,1],[1,1],[2,0],[0,2],[2,1],[1,2]]) {
    const b = bot.blockAt(new Vec3(SRC.x+dx, SRC.y+1, SRC.z+dz));
    if (b && b.name !== 'air') { await bot.dig(b); await sleep(300); }
  }
  await cmd('say RENEWABLE_DEPLETED'); await cmd('moba renewables');
  await cmd('clear MobaTest'); await sleep(400);
  await cmd('say RENEWABLE_INVENTORY_BEFORE'); await cmd('data get entity MobaTest Inventory');
  await sleep(400);
  // Push the world clock past the recovery deadline. Lazy recovery settles on
  // the next report, so no ticking loop is needed.
  await cmd('time add 400'); await sleep(600);
  await cmd('say RENEWABLE_RECOVERED'); await cmd('moba renewables');
  await sleep(400);
  await cmd('say RENEWABLE_INVENTORY_AFTER'); await cmd('data get entity MobaTest Inventory');
};

// #13 Animal harvest counts; a monster in the same volume does not.
exports.renewableAnimal = async ({cmd,sleep}) => {
  await cmd('moba setclass MobaTest test');
  await cmd(`tp MobaTest ${SRC.x}.5 ${SRC.y + 1} ${SRC.z - 2}.5 0 0`);
  await sleep(600);
  await cmd('say ANIMAL_BEFORE'); await cmd('moba renewables');
  await cmd(`summon minecraft:cow ${SRC.x}.5 ${SRC.y + 1} ${SRC.z}.5 {NoAI:1b,Health:1f}`);
  await sleep(500);
  // Killed by the player, so it counts as harvest rather than attrition.
  await cmd(`execute as MobaTest at @s run damage @e[type=minecraft:cow,limit=1,sort=nearest] 100 minecraft:player_attack by MobaTest`);
  await sleep(700);
  await cmd('say ANIMAL_AFTER'); await cmd('moba renewables');
  await cmd(`summon minecraft:zombie ${SRC.x}.5 ${SRC.y + 1} ${SRC.z}.5 {NoAI:1b,Health:1f}`);
  await sleep(500);
  await cmd(`execute as MobaTest at @s run damage @e[type=minecraft:zombie,limit=1,sort=nearest] 100 minecraft:player_attack by MobaTest`);
  await sleep(700);
  await cmd('say MONSTER_AFTER'); await cmd('moba renewables');
};
