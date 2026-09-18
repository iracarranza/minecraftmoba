// Real server packet-path checks. Assertions use server NBT, not client predictions.
const fs=require('node:fs');const mineflayer=require('mineflayer');const {Vec3}=require('vec3');
const Item=require('prismarine-item')('1.21.11');
const bot=mineflayer.createBot({host:'127.0.0.1',port:25576,username:'MobaVerify',auth:'offline',version:'1.21.11'});
fs.mkdirSync('results',{recursive:true});const out=`results/inventory-${Date.now()}.jsonl`;
const log=(kind,value)=>{const row=JSON.stringify({time:new Date().toISOString(),kind,value});fs.appendFileSync(out,row+'\n');console.log(row)};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));let stateId=0;
bot._client.on('window_items',p=>stateId=p.stateId);bot._client.on('set_slot',p=>stateId=p.stateId);
bot.on('messagestr',m=>log('chat',m));bot.on('error',e=>log('error',e.stack));bot.on('kicked',e=>log('kicked',e));
const cmd=async s=>{bot.chat('/'+s);await sleep(300)};
async function data(path){return await new Promise((resolve,reject)=>{
 const timer=setTimeout(()=>{bot.removeListener('messagestr',receive);reject(new Error('No server NBT response: '+path))},3000);
 function receive(m){if(m.startsWith('MobaVerify has the following entity data: ')){clearTimeout(timer);bot.removeListener('messagestr',receive);resolve(m)}}
 bot.on('messagestr',receive);bot.chat('/data get entity @s '+path);
})}
const click=async(slot,button=0,mode=0)=>{bot._client.write('window_click',{windowId:bot.currentWindow?.id||0,stateId,slot,mouseButton:button,mode,changedSlots:[],cursorItem:Item.toNotch(null)});await sleep(200)};
async function close(){bot._client.write('close_window',{windowId:bot.currentWindow?.id||0});bot.currentWindow=null;await sleep(300)}
let baseline;
async function unchanged(name,action){await action();await close();const after=await data('Inventory');if(after!==baseline)throw new Error(name+' changed server inventory: '+after);await cmd('execute unless entity @e[type=item,distance=..8,tag=!pickup_fixture] run say NO_UNEXPECTED_GROUND_ITEMS');log('pass',name)}
bot.once('spawn',async()=>{try{
 await cmd('moba reset MobaVerify');await cmd('tp @s 200.5 -59 200.5');await sleep(800);
 for(let i=0;i<6;i++) await cmd(`item replace entity @s hotbar.${i} with stone 64`);
 baseline=await data('Inventory');await data('equipment');
 for(const [name,slot,button,mode] of [['map left',45,0,0],['map right',45,1,0],['map shift',45,0,1],['map number key',45,0,2],['map drop one',45,0,4],['map drop stack',45,1,4],['offhand hotkey',36,40,2]]){
  await unchanged(name,()=>click(slot,button,mode));await data('equipment');
 }
 for(const slot of [42,9]) await unchanged('locked insertion '+slot,async()=>{await click(36);await click(slot)});
 await unchanged('locked drag',async()=>{await click(36);await click(-999,0,5);await click(42,1,5);await click(9,1,5);await click(-999,2,5)});
 await unchanged('crafting return safety',async()=>{await click(36);await click(1)});
 await cmd('setblock 202 -59 200 chest');await cmd('item replace block 202 -59 200 container.0 with oak_planks 64');
 async function chest(){await bot.openContainer(bot.blockAt(new Vec3(202,-59,200)));await sleep(200)}
 await unchanged('chest shift insertion',async()=>{await chest();await click(0,0,1)});
 await unchanged('chest locked hotkey',async()=>{await chest();await click(0,6,2)});
 await unchanged('external cursor return denied at full capacity',async()=>{await chest();await click(0)});
 await cmd('data get block 202 -59 200 Items');
 await cmd('summon item ~ ~ ~ {Tags:["pickup_fixture"],Item:{id:"minecraft:dirt",count:64},PickupDelay:0s}');await sleep(1000);
 if(await data('Inventory')!==baseline)throw new Error('Overflow pickup entered locked inventory');log('pass','overflow pickup');
 await cmd('moba setlevel MobaVerify 2');await sleep(1000);const unlocked=await data('Inventory');if(!unlocked.includes('Slot: 6b, id: "minecraft:dirt", count: 64'))throw new Error('Unlocked slot did not accept waiting dirt');log('pass','new slots usable immediately');
 await cmd('gamemode creative');await cmd('moba setlevel MobaVerify 1');baseline=await data('Inventory');
 await unchanged('creative locked insertion',async()=>{bot._client.write('set_creative_slot',{slot:43,item:Item.toNotch(new Item(bot.registry.itemsByName.diamond.id,1))});await sleep(250)});
 await unchanged('creative map deletion',async()=>{bot._client.write('set_creative_slot',{slot:45,item:Item.toNotch(null)});await sleep(250)});await data('equipment');
 await cmd('gamemode survival');
 log('complete','Inventory matrix completed; inspect equipment and no-ground-item markers alongside each pass.');bot.quit('inventory matrix complete');
}catch(e){log('failure',e.stack);bot.quit('inventory matrix failed');process.exitCode=1}});
