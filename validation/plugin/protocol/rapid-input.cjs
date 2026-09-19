const fs=require('node:fs');const mineflayer=require('mineflayer');const {Vec3}=require('vec3');
const bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MOBA_PORT||25576),username:'MobaVerify',auth:'offline',version:'1.21.11'});
fs.mkdirSync('results',{recursive:true});const file=`results/rapid-${Date.now()}.jsonl`;
const log=(kind,value)=>{const l=JSON.stringify({time:new Date().toISOString(),kind,value});fs.appendFileSync(file,l+'\n');console.log(l)};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const cmd=async s=>{bot.chat('/'+s);await sleep(300)};
const action=status=>bot._client.write('block_dig',{status,location:{x:0,y:0,z:0},face:0,sequence:0});
bot.on('messagestr',m=>log('chat',m));bot.on('error',e=>log('error',e.stack));
bot.once('spawn',async()=>{try{
 await cmd('forceload add 80 80');await sleep(800);
 await cmd('fill 80 -60 80 95 -60 95 bedrock');
 await cmd('tp @s 88.5 -59 88.5 0 0');await sleep(800);
 if(bot.blockAt(new Vec3(88,-60,88))?.name!=='bedrock') throw new Error('Missing safe fixture platform');
 await cmd('moba setclass MobaVerify test');await bot.look(0,0,false);await sleep(200);
 await cmd('data get entity @s Pos');
 await cmd('say RAPID_M1_BEFORE');await cmd('moba debug MobaVerify');
 for(let i=0;i<50;i++){action(6);bot.swingArm('right');await sleep(150);action(6);await sleep(550)}
 await cmd('say RAPID_M1_AFTER');await cmd('moba debug MobaVerify');
 await cmd('tp @s 88.5 -59 88.5 0 0');await sleep(800);
 // Keep every use-item-on target well inside survival reach and the supporting
 // block outside the collapse sphere; avoid fixture misses at the 4.5-block edge.
 await cmd('say RAPID_M2_BEFORE');await cmd('moba debug MobaVerify');
 for(let i=0;i<50;i++) {
  await cmd('fill 91 -58 85 95 -58 91 stone');
  await bot.lookAt(new Vec3(91.5,-57.5,88.5),false);
  action(6);
  bot._client.write('block_place',{location:new Vec3(91,-58,88),direction:1,hand:0,cursorX:.5,cursorY:.5,cursorZ:.5,insideBlock:false,sequence:0,worldBorderHit:false});
  await sleep(150);action(6);await sleep(550);
 }
 await cmd('say RAPID_M2_AFTER');await cmd('moba debug MobaVerify');
 bot.setQuickBarSlot(8);await sleep(200);
 await cmd('say RAPID_EMPTY_Q_BEFORE');await cmd('moba debug MobaVerify');
 for(let i=0;i<50;i++){action(6);action(4);action(3);await sleep(150);action(6);await sleep(550)}
 await cmd('say RAPID_EMPTY_Q_AFTER');await cmd('moba debug MobaVerify');
 await cmd('forceload remove 80 80');bot.quit('rapid input complete');
}catch(e){log('failure',e.stack);bot.quit('rapid input failed')}});
