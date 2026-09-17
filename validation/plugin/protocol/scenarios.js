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

// #1 Provenance. Places a 3-block player-built column INSIDE the blast volume and
// asserts each survives while natural neighbours go. Reads playerPlacedExcluded.
exports.sinkholeProvenance = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('fill 0 -58 -8 6 -54 -2 minecraft:air');           // clear headroom
  await cmd('fill 0 -60 -8 6 -60 -2 minecraft:stone');          // natural floor (server-set, unmarked)
  bot.setQuickBarSlot(0);
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

// #5b Mob-targeted M1/M2 against a summoned, non-retaliating target.
exports.mobTargeted = async ({bot,cmd,action,sleep,Vec3}) => {
  await cmd('moba setclass MobaTest test');
  await cmd('kill @e[type=minecraft:zombie]');
  await cmd('summon minecraft:zombie 4 -59 -5 {NoAI:1b,PersistenceRequired:1b,CustomName:\'"MobaTarget"\',Health:20f}');
  await sleep(600);
  await cmd('moba debug MobaTest');
  await bot.lookAt(new Vec3(4,-58.5,-5), true);
  action(6); await sleep(150); bot.swingArm('right'); await sleep(400); action(6);
  await sleep(400);
  await cmd('say MOB_M1_DONE');
  await cmd('data get entity @e[type=minecraft:zombie,limit=1] Health');
  action(6); await sleep(150);
  await bot.activateBlock(bot.blockAt(new Vec3(4,-60,-5)));
  await sleep(2000); action(6); await sleep(200);
  await cmd('say MOB_M2_DONE');
  await cmd('data get entity @e[type=minecraft:zombie,limit=1] Health');
  await cmd('moba debug MobaTest');
};
