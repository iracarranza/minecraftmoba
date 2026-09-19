// Controlled live building workload; never presented as human ordinary-play evidence.
const fs=require('node:fs');
const mineflayer=require('mineflayer');
const {Vec3}=require('vec3');
const bot=mineflayer.createBot({host:'127.0.0.1',port:25576,username:'MobaTest',auth:'offline',version:'1.21.11'});
fs.mkdirSync('results',{recursive:true});
const output=`results/building-${Date.now()}.jsonl`;
const log=(kind,value)=>{const s=JSON.stringify({time:new Date().toISOString(),kind,value});fs.appendFileSync(output,s+'\n');console.log(s)};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const cmd=async text=>{bot.chat('/'+text);await sleep(300)};
bot.on('messagestr',m=>log('chat',m));bot.on('error',e=>log('error',e.stack));bot.on('kicked',e=>log('kicked',e));
bot.once('spawn',async()=>{try {
  await cmd('op MobaVerify');
  await cmd('difficulty peaceful');
  await cmd('moba reset MobaTest');
  await cmd('forceload add 1024 1024'); await sleep(1000);
  await cmd('fill 1024 64 1024 1039 64 1039 stone');
  await cmd('tp @s 1031.5 65 1031.5'); await sleep(1500);
  if(bot.blockAt(new Vec3(1031,64,1031))?.name!=='stone') throw new Error('Fixture foundation is unavailable');
  await cmd('item replace entity @s hotbar.0 with stone 64');
  await cmd('item replace entity @s hotbar.1 with diamond_pickaxe');
  await cmd('moba provenance');
  const positions=[];
  // Low wall along three sides; all placements use real survival packets in reach.
  for(let x=1029;x<=1033;x++) positions.push(new Vec3(x,65,1029));
  for(let z=1030;z<=1033;z++) positions.push(new Vec3(1029,65,z),new Vec3(1033,65,z));
  const start=Date.now();const duration=Number(process.env.BUILD_DURATION_MS||1800000);
  let cycles=0,placed=0,broken=0;
  log('sessionStart',{durationMs:duration,positions:positions.length,description:'Controlled survival place/break at a paced building rate, repeated wall construction and teardown.'});
  while(Date.now()-start<duration) {
    bot.setQuickBarSlot(0);await sleep(250);
    // Operator refill supplies construction material without contributing placement marks.
    await cmd('item replace entity @s hotbar.0 with stone 64');
    for(const pos of positions) {
      await bot.placeBlock(bot.blockAt(pos.offset(0,-1,0)),new Vec3(0,1,0));
      placed++;await sleep(750);
    }
    await cmd('moba provenance');
    bot.setQuickBarSlot(1);await sleep(250);
    for(const pos of [...positions].reverse()) {
      const b=bot.blockAt(pos);if(!b || b.name!=='stone') throw new Error('Expected player wall block at '+pos);
      await bot.dig(b);broken++;await sleep(900);
    }
    cycles++;await cmd('moba provenance');
    log('cycle',{cycles,placed,broken,elapsedSeconds:(Date.now()-start)/1000});
    if(bot.inventory.slots[37]?.name!=='diamond_pickaxe') throw new Error('Missing fixture tool');
  }
  await cmd('moba provenance');
  log('sessionComplete',{cycles,placed,broken,elapsedSeconds:(Date.now()-start)/1000});
  await cmd('forceload remove 1024 1024');
  bot.quit('building measurement complete');
}catch(e){log('failure',e.stack);bot.quit('building measurement failed');process.exitCode=1;}});
