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
